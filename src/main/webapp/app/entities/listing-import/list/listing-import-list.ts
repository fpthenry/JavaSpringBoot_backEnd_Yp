import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbPagination } from '@ng-bootstrap/ng-bootstrap/pagination';
import { TranslatePipe } from '@ngx-translate/core';
import { Subscription, switchMap, timer } from 'rxjs';

import { TOTAL_COUNT_RESPONSE_HEADER } from 'app/config';
import { Alert, AlertError } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { durationText, parsePairs, sourceCode, sourceKind } from '../listing-import-display';
import { IApiImportRequest, IImportBatch } from '../listing-import.model';
import { ListingImportService } from '../service/listing-import.service';

/** Giới hạn upload của backend (spring.servlet.multipart.max-file-size). */
const MAX_FILE_BYTES = 100 * 1024 * 1024;
const PAGE_SIZE = 20;
/** Đợt đang chạy: hỏi lại mỗi 2 giây. */
const POLL_MS = 2000;

/**
 * Sửa tay: trang "Nhập doanh nghiệp" — các đợt nhập, khung nhập mới (file Excel/CSV hoặc API), tải file mẫu,
 * theo dõi đợt đang chạy. Chi tiết, duyệt nghi trùng ở /listing-import/:id. Chỉ ROLE_ADMIN.
 */
@Component({
  selector: 'jhi-listing-import-list',
  templateUrl: './listing-import-list.html',
  imports: [RouterLink, FormsModule, FontAwesomeModule, NgbPagination, TranslateDirective, TranslatePipe, DatePipe, Alert, AlertError],
})
export class ListingImportList implements OnInit {
  protected readonly batches = signal<IImportBatch[]>([]);
  protected readonly totalItems = signal(0);
  protected readonly page = signal(1);
  protected readonly loading = signal(false);
  protected readonly running = signal<IImportBatch | null>(null);
  protected readonly now = signal(Date.now());

  // ---- khung nhập mới
  protected readonly showNew = signal(false);
  protected readonly tab = signal<'file' | 'api'>('file');
  protected readonly file = signal<File | null>(null);
  protected readonly fileError = signal<string | null>(null);
  protected readonly dragOver = signal(false);
  protected dryRun = true;
  protected readonly submitting = signal(false);
  protected readonly submitError = signal<string | null>(null);
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

  protected readonly pageSize = PAGE_SIZE;
  protected readonly sourceKind = sourceKind;
  protected readonly sourceCode = sourceCode;
  protected readonly runningDuration = computed(() => {
    const r = this.running();
    return r ? durationText(r, this.now()) : '';
  });

  private readonly service = inject(ListingImportService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private poll?: Subscription;

  ngOnInit(): void {
    this.load();
    this.watchRunning();
  }

  load(): void {
    this.loading.set(true);
    this.service.batches(this.page() - 1, PAGE_SIZE).subscribe({
      next: res => {
        this.batches.set(res.body ?? []);
        this.totalItems.set(Number(res.headers.get(TOTAL_COUNT_RESPONSE_HEADER) ?? 0));
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  changePage(page: number): void {
    this.page.set(page);
    this.load();
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
            this.load();
          }
          if (!batch) {
            this.poll?.unsubscribe();
          }
        },
        error: () => this.poll?.unsubscribe(),
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

  durationOf(batch: IImportBatch): string {
    return durationText(batch, this.now());
  }

  toggleNew(): void {
    this.showNew.update(v => !v);
    this.submitError.set(null);
  }
}
