import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { TranslatePipe } from '@ngx-translate/core';
import { Subscription, switchMap, timer } from 'rxjs';

import { Alert, AlertError } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { durationText, parsePairs, rejectedCount } from '../listing-import-display';
import { IApiImportRequest, IDirectusSummary, IImportBatch, PullMode } from '../listing-import.model';
import { ListingImportService } from '../service/listing-import.service';

/** Giới hạn upload của backend (spring.servlet.multipart.max-file-size). */
const MAX_FILE_BYTES = 100 * 1024 * 1024;
/** Nguồn API Trang Vàng (DirectusRecordSource.SOURCE). */
const DIRECTUS_SOURCE = 'api:yp-directus';
/** Đợt đang chạy: hỏi lại mỗi 2 giây. */
const POLL_MS = 2000;

/**
 * Sửa tay: trang "Nhập doanh nghiệp": ba bước với API Trang Vàng (lấy dữ liệu, kiểm tra, nhập vào hệ thống), nhập từ file Excel/CSV
 * hoặc API khác, theo dõi đợt đang chạy. Lịch sử ở /listing-import/history, chi tiết ở /listing-import/:id. Chỉ ROLE_ADMIN.
 */
@Component({
  selector: 'jhi-listing-import-list',
  templateUrl: './listing-import-list.html',
  imports: [RouterLink, FormsModule, FontAwesomeModule, TranslateDirective, TranslatePipe, DatePipe, Alert, AlertError],
})
export class ListingImportList implements OnInit {
  /** Đợt gần nhất, gồm cả lần kiểm tra */
  protected readonly latest = signal<IImportBatch | null>(null);
  protected readonly running = signal<IImportBatch | null>(null);
  protected readonly now = signal(Date.now());

  // ---- khung nhập
  protected readonly tab = signal<'file' | 'api' | 'directus'>('directus');
  protected readonly file = signal<File | null>(null);
  protected readonly fileError = signal<string | null>(null);
  protected readonly dragOver = signal(false);
  protected dryRun = true;
  protected readonly submitting = signal(false);
  protected readonly submitError = signal<string | null>(null);
  /** Tab API Trang Vàng: tình trạng bảng bản gốc và tiến độ kéo */
  protected readonly pull = signal<IDirectusSummary | null>(null);
  protected readonly pulling = computed(() => this.pull()?.pull?.state === 'RUNNING');
  /** Lần lấy toàn bộ bị dừng giữa chừng: lấy tiếp từ id đã dừng */
  /** Lần lấy toàn bộ bị dừng giữa chừng (kể cả khi đã lấy bản mới sau đó): lấy tiếp từ id đã dừng */
  protected readonly canResume = computed(() => !!this.pull()?.unfinishedFull && !this.pulling());
  protected readonly cancellingPull = signal(false);
  protected api = {
    source: '',
    url: '',
    itemsPath: '',
    headers: '',
    pageParam: '',
    firstPage: 1,
    maxPages: 1,
    delayMs: 300,
    fieldMapping: '',
  };

  protected readonly confirmImport = signal(false);
  protected readonly rejectedCount = rejectedCount;
  /** Kết quả kiểm tra mới nhất còn hiệu lực: chưa nhập, còn dữ liệu chờ */
  protected readonly checkResult = computed(() => {
    const b = this.latest();
    return b?.dryRun && b.status === 'DONE' && b.source === DIRECTUS_SOURCE && (this.pull()?.pending ?? 0) > 0 ? b : null;
  });
  /** Lần nhập mới nhất, khi chưa có lần kiểm tra nào sau đó */
  protected readonly lastImport = computed(() => {
    const b = this.latest();
    return b && !b.dryRun && b.status === 'DONE' && b.source === DIRECTUS_SOURCE ? b : null;
  });
  protected readonly runningDuration = computed(() => {
    const r = this.running();
    return r ? durationText(r, this.now()) : '';
  });

  protected readonly cancelling = signal(false);

  private readonly service = inject(ListingImportService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private poll?: Subscription;
  private pullPoll?: Subscription;

  ngOnInit(): void {
    this.load();
    this.watchRunning();
    this.watchPull();
  }

  load(): void {
    this.service.batches(0, 1).subscribe(res => this.latest.set(res.body?.[0] ?? null));
  }

  /** Hỏi đợt đang chạy mỗi 2 giây; khi đợt xong thì tải lại danh sách để cập nhật bộ đếm. */
  private watchRunning(): void {
    this.poll?.unsubscribe();
    this.poll = timer(0, POLL_MS)
      .pipe(
        switchMap(() => this.service.running()),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: batch => {
          const wasRunning = this.running();
          this.running.set(batch);
          this.now.set(Date.now());
          if (wasRunning && !batch) {
            this.cancelling.set(false);
            this.load();
            this.watchPull(); // số "chưa nhập" đổi sau khi nhập
          }
          if (!batch) {
            this.poll?.unsubscribe();
          }
        },
        error: () => this.poll?.unsubscribe(),
      });
  }

  cancelRunning(batchId: number): void {
    this.cancelling.set(true);
    this.service.cancel(batchId).subscribe({ error: () => this.cancelling.set(false) });
  }

  // ---------------------------------------------------------------- API Trang Vàng

  openDirectus(): void {
    this.tab.set('directus');
    this.watchPull();
  }

  canCheck(): boolean {
    return !this.submitting() && !this.running() && !this.pulling() && (this.pull()?.pending ?? 0) > 0;
  }

  /** Bước 2 (true: kiểm tra, chưa ghi) và bước 3 (false: nhập vào hệ thống). Ở lại trang này, xong thì hiện kết quả. */
  runDirectus(check: boolean): void {
    this.submitting.set(true);
    this.submitError.set(null);
    this.confirmImport.set(false);
    this.service.importDirectus(check).subscribe({
      next: batch => {
        this.submitting.set(false);
        this.running.set(batch);
        this.latest.set(batch);
        this.watchRunning();
      },
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false);
        this.submitError.set(err.error?.error ?? err.error?.detail ?? err.message);
      },
    });
  }

  /** Bước 1: lấy về bảng tạm. NEW: chỉ phần thay đổi; FULL: cả bảng; RESUME: đọc tiếp khi lần trước lỗi. */
  startPull(mode: PullMode): void {
    this.submitError.set(null);
    this.service.startPull(mode).subscribe({
      next: summary => {
        this.pull.set(summary);
        this.watchPull();
      },
      error: (err: HttpErrorResponse) => this.submitError.set(err.error?.error ?? err.error?.detail ?? err.message),
    });
  }

  cancelPull(): void {
    this.cancellingPull.set(true);
    this.service.cancelPull().subscribe({
      next: () => this.cancellingPull.set(false),
      error: () => this.cancellingPull.set(false),
    });
  }

  /** Màu khung kết quả lần lấy gần nhất. */
  pullAlertClass(state: string): string {
    switch (state) {
      case 'RUNNING':
        return 'alert-info';
      case 'DONE':
        return 'alert-success';
      case 'FAILED':
        return 'alert-danger';
      default:
        return 'alert-warning';
    }
  }

  /** Hỏi tiến độ kéo mỗi 2 giây cho tới khi xong. */
  private watchPull(): void {
    this.pullPoll?.unsubscribe();
    this.pullPoll = timer(0, POLL_MS)
      .pipe(
        switchMap(() => this.service.pullSummary()),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: summary => {
          this.pull.set(summary);
          if (summary.pull?.state !== 'RUNNING') {
            this.pullPoll?.unsubscribe();
          }
        },
        error: (err: HttpErrorResponse) => {
          this.pullPoll?.unsubscribe();
          this.submitError.set(err.error?.error ?? err.error?.detail ?? err.message);
        },
      });
  }

  // ---------------------------------------------------------------- file mẫu

  downloadTemplate(): void {
    this.service.template().subscribe(blob => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'mau-nhap-doanh-nghiep.csv';
      a.click();
      URL.revokeObjectURL(url);
    });
  }

  // ---------------------------------------------------------------- chọn file

  onFileInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.pickFile(input.files?.[0] ?? null);
    input.value = '';
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    this.pickFile(event.dataTransfer?.files?.[0] ?? null);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(true);
  }

  pickFile(file: File | null): void {
    this.submitError.set(null);
    if (!file) {
      return;
    }
    if (!/\.(xlsx|csv)$/i.test(file.name)) {
      this.file.set(null);
      this.fileError.set('javaSpringBootBackEndApp.listingImport.new.badType');
      return;
    }
    if (file.size > MAX_FILE_BYTES) {
      this.file.set(null);
      this.fileError.set('javaSpringBootBackEndApp.listingImport.new.tooBig');
      return;
    }
    this.fileError.set(null);
    this.file.set(file);
  }

  fileSize(bytes: number): string {
    return bytes < 1024 * 1024 ? `${Math.max(1, Math.round(bytes / 1024))} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  }

  // ---------------------------------------------------------------- nhập

  apiSourceValid(): boolean {
    return /^[a-z0-9-]{1,40}$/.test(this.api.source.trim());
  }

  apiUrlValid(): boolean {
    return /^https?:\/\/\S+$/i.test(this.api.url.trim());
  }

  canSubmit(): boolean {
    if (this.submitting() || this.running()) {
      return false;
    }
    return this.tab() === 'file' ? this.file() !== null : this.apiSourceValid() && this.apiUrlValid();
  }

  submit(): void {
    if (!this.canSubmit()) {
      return;
    }
    this.submitting.set(true);
    this.submitError.set(null);
    const request$ = this.tab() === 'file' ? this.service.importFile(this.file()!, this.dryRun) : this.service.importApi(this.apiRequest());
    request$.subscribe({
      next: batch => {
        this.submitting.set(false);
        this.router.navigate(['/listing-import', batch.id]);
      },
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false);
        // Backend trả {"error": "..."} cho 400/409 (vd đang có đợt khác chạy)
        this.submitError.set(err.error?.error ?? err.error?.detail ?? err.message);
      },
    });
  }

  apiRequest(): IApiImportRequest {
    const headers = parsePairs(this.api.headers, ':');
    const fieldMapping = parsePairs(this.api.fieldMapping, '=');
    return {
      source: this.api.source.trim(),
      url: this.api.url.trim(),
      itemsPath: this.api.itemsPath.trim() || undefined,
      pageParam: this.api.pageParam.trim() || undefined,
      firstPage: this.api.firstPage || undefined,
      maxPages: this.api.maxPages || undefined,
      delayMs: this.api.delayMs ?? undefined,
      headers: Object.keys(headers).length ? headers : undefined,
      fieldMapping: Object.keys(fieldMapping).length ? fieldMapping : undefined,
      dryRun: this.dryRun,
    };
  }

  /** 1234 → "1.234" */
  fmt(n: number | null | undefined): string {
    return (n ?? 0).toLocaleString('vi-VN');
  }
}
