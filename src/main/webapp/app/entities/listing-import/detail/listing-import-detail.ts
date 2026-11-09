import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbPagination } from '@ng-bootstrap/ng-bootstrap/pagination';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { Subscription, forkJoin, switchMap, timer } from 'rxjs';

import { TOTAL_COUNT_RESPONSE_HEADER } from 'app/config';
import { AlertService } from 'app/core/util/alert.service';
import { Alert, AlertError } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import {
  ROW_STATUSES,
  STATUS_BADGE,
  compareLines,
  durationText,
  filledList,
  filledOnlyList,
  messageList,
  reasonLabelKey,
  scorePercent,
  shortValue,
  sourceCode,
  sourceKind,
  statusCount,
  statusLabelKey,
  updatedList,
  visibleChanges,
} from '../listing-import-display';
import { IFieldStat, IImportBatch, IImportRowView, ImportAction, ImportRowStatus } from '../listing-import.model';
import { ListingImportService } from '../service/listing-import.service';

/** Nguồn API Trang Vàng (DirectusRecordSource.SOURCE). */
const DIRECTUS_SOURCE = 'api:yp-directus';
const ROWS_PER_PAGE = 50;
/** Đợt còn đang xử lý: trang tự cập nhật. */
const isActive = (b: IImportBatch): boolean => b.status === 'RUNNING' || b.status === 'UNDOING';
const POLL_MS = 2000;

/**
 * Sửa tay: một đợt nhập — tiến độ (tự cập nhật khi đang chạy), bộ đếm bấm để lọc, bảng từng dòng,
 * duyệt dòng nghi trùng (so cạnh nhau: gộp / tạo mới / bỏ qua / gộp vào doanh nghiệp khác), xuất bản doanh nghiệp nháp.
 */
@Component({
  selector: 'jhi-listing-import-detail',
  templateUrl: './listing-import-detail.html',
  imports: [RouterLink, FormsModule, FontAwesomeModule, NgbPagination, TranslateDirective, TranslatePipe, DatePipe, Alert, AlertError],
})
export class ListingImportDetail implements OnInit {
  protected readonly batch = signal<IImportBatch | null>(null);
  protected readonly rows = signal<IImportRowView[]>([]);
  protected readonly rowTotal = signal(0);
  protected readonly rowPage = signal(1);
  protected readonly filter = signal<ImportRowStatus | null>(null);
  protected readonly loadingRows = signal(false);
  protected readonly now = signal(Date.now());
  /** Dòng đang gửi quyết định duyệt (khóa nút) */
  protected readonly busyRow = signal<number | null>(null);
  protected readonly rowError = signal<{ rowId: number; message: string } | null>(null);
  /** ID doanh nghiệp khác để gộp, theo dòng */
  protected otherId: Record<number, number | null> = {};
  protected readonly confirmPublish = signal(false);
  protected readonly publishing = signal(false);
  /** Số doanh nghiệp nháp còn lại của đợt; null khi chưa tải xong */
  protected readonly drafts = signal<number | null>(null);
  /** Đợt chạy thử mới nhất của API Trang Vàng, còn dữ liệu chưa nhập: hiện nút Nhập thật */
  protected readonly canImport = signal(false);
  protected readonly confirmImport = signal(false);
  protected readonly importing = signal(false);
  protected readonly importError = signal<string | null>(null);
  protected readonly cancelling = signal(false);
  /** Ô tìm: chữ đang gõ; {@link q}: chữ đã áp dụng */
  protected search = '';
  protected readonly q = signal('');
  /** Lọc theo trường đã đổi */
  protected readonly field = signal<string | null>(null);
  protected readonly stats = signal<IFieldStat[]>([]);
  /** Dòng đang mở xem cũ → mới */
  protected readonly expanded = signal<ReadonlySet<number>>(new Set());
  protected readonly exporting = signal(false);
  protected readonly confirmUndo = signal(false);

  protected readonly statuses = ROW_STATUSES;
  protected readonly badge = STATUS_BADGE;
  protected readonly rowsPerPage = ROWS_PER_PAGE;
  protected readonly sourceKind = sourceKind;
  protected readonly sourceCode = sourceCode;
  protected readonly statusCount = statusCount;
  protected readonly statusLabelKey = statusLabelKey;
  protected readonly reasonLabelKey = reasonLabelKey;
  protected readonly scorePercent = scorePercent;
  protected readonly filledList = filledList;
  protected readonly filledOnlyList = filledOnlyList;
  protected readonly updatedList = updatedList;
  protected readonly visibleChanges = visibleChanges;
  protected readonly shortValue = shortValue;
  protected readonly messageList = messageList;
  protected readonly compareLines = compareLines;

  protected readonly running = computed(() => this.batch()?.status === 'RUNNING');
  protected readonly undoing = computed(() => this.batch()?.status === 'UNDOING');
  /** Hoàn tác được: đợt nhập thật đã xong, có đổi dữ liệu */
  protected readonly canUndo = computed(() => {
    const b = this.batch();
    return !!b && !b.dryRun && b.status === 'DONE' && b.createdCount + b.mergedCount > 0;
  });
  protected readonly duration = computed(() => {
    const b = this.batch();
    return b ? durationText(b, this.now()) : '';
  });
  /** Duyệt được: đợt nhập thật đã chạy xong */
  protected readonly canResolve = computed(() => {
    const b = this.batch();
    return !!b && !b.dryRun && b.status === 'DONE';
  });

  private readonly service = inject(ListingImportService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly alerts = inject(AlertService);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);
  private poll?: Subscription;
  private batchId = 0;

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      this.batchId = Number(params.get('id'));
      // Mở từ thẻ số liệu ở trang nhập: vào thẳng tab đó
      this.filter.set(this.route.snapshot.queryParamMap.get('status') as ImportRowStatus | null);
      this.rowPage.set(1);
      this.loadBatch();
      this.loadRows();
    });
  }

  loadBatch(): void {
    this.service.batch(this.batchId).subscribe(b => {
      this.batch.set(b);
      this.now.set(Date.now());
      this.loadDrafts(b);
      this.loadImportable(b);
      this.loadStats(b);
      if (isActive(b)) {
        this.startPolling();
      }
    });
  }

  private loadStats(b: IImportBatch): void {
    if (isActive(b)) {
      return;
    }
    this.service.fieldStats(b.id).subscribe(stats => this.stats.set(stats));
  }

  private loadImportable(b: IImportBatch): void {
    if (!b.dryRun || b.status !== 'DONE' || b.source !== DIRECTUS_SOURCE) {
      this.canImport.set(false);
      return;
    }
    forkJoin([this.service.batches(0, 1), this.service.pullSummary()]).subscribe(([latest, summary]) => {
      this.canImport.set(latest.body?.[0]?.id === b.id && summary.pending > 0);
    });
  }

  applySearch(): void {
    this.q.set(this.search.trim());
    this.rowPage.set(1);
    this.loadRows();
  }

  clearSearch(): void {
    this.search = '';
    this.applySearch();
  }

  /** Bấm lại trường đang chọn thì bỏ lọc. */
  setField(field: string): void {
    this.field.update(current => (current === field ? null : field));
    this.rowPage.set(1);
    this.loadRows();
  }

  toggleRow(id: number): void {
    this.expanded.update(current => {
      const next = new Set(current);
      if (!next.delete(id)) {
        next.add(id);
      }
      return next;
    });
  }

  /** Tải đúng các dòng đang lọc. */
  exportRows(): void {
    this.exporting.set(true);
    this.service.exportRows(this.batchId, this.filter(), this.q(), this.field()).subscribe({
      next: blob => {
        this.exporting.set(false);
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `dot-nhap-${this.batchId}.csv`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.exporting.set(false),
    });
  }

  undo(): void {
    this.importError.set(null);
    this.service.undo(this.batchId).subscribe({
      next: batch => {
        this.confirmUndo.set(false);
        this.batch.set(batch);
        this.startPolling();
      },
      error: (err: HttpErrorResponse) => {
        this.confirmUndo.set(false);
        this.importError.set(err.error?.error ?? err.error?.detail ?? err.message);
      },
    });
  }

  cancelRun(): void {
    this.cancelling.set(true);
    this.service.cancel(this.batchId).subscribe({ error: () => this.cancelling.set(false) });
  }

  /** Từ kết quả chạy thử: nhập thật cùng dữ liệu, rồi chuyển sang đợt nhập mới. */
  importReal(): void {
    this.importing.set(true);
    this.importError.set(null);
    this.service.importDirectus(false).subscribe({
      next: batch => {
        this.importing.set(false);
        this.confirmImport.set(false);
        this.router.navigate(['/listing-import', batch.id]);
      },
      error: (err: HttpErrorResponse) => {
        this.importing.set(false);
        this.importError.set(err.error?.error ?? err.error?.detail ?? err.message);
      },
    });
  }

  /** Đợt nhập thật đã xong: lấy số nháp còn lại, để nút xuất bản giảm dần và ẩn khi hết. */
  private loadDrafts(b: IImportBatch): void {
    if (b.dryRun || b.status !== 'DONE' || b.createdCount === 0) {
      this.drafts.set(0);
      return;
    }
    this.service.drafts(b.id).subscribe(count => this.drafts.set(count));
  }

  /** Đang chạy: hỏi lại tiến độ mỗi 2 giây; xong thì tải lại bảng dòng. */
  private startPolling(): void {
    if (this.poll && !this.poll.closed) {
      return;
    }
    this.poll = timer(POLL_MS, POLL_MS)
      .pipe(
        switchMap(() => this.service.batch(this.batchId)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(b => {
        this.batch.set(b);
        this.now.set(Date.now());
        if (!isActive(b)) {
          this.poll?.unsubscribe();
          this.loadDrafts(b);
          this.loadImportable(b);
          this.loadStats(b);
          this.loadRows();
        } else if (this.rows().length === 0 && !this.loadingRows()) {
          this.loadRows(); // đợt mới chạy: hiện dòng đầu tiên ngay, không chờ tới khi xong
        }
      });
  }

  loadRows(): void {
    this.loadingRows.set(true);
    this.service.rows(this.batchId, this.filter(), this.rowPage() - 1, ROWS_PER_PAGE, this.q(), this.field()).subscribe({
      next: res => {
        this.rows.set(res.body ?? []);
        this.rowTotal.set(Number(res.headers.get(TOTAL_COUNT_RESPONSE_HEADER) ?? 0));
        this.loadingRows.set(false);
      },
      error: () => this.loadingRows.set(false),
    });
  }

  setFilter(status: ImportRowStatus | null): void {
    this.filter.set(status);
    this.rowPage.set(1);
    this.loadRows();
  }

  changeRowPage(page: number): void {
    this.rowPage.set(page);
    this.loadRows();
  }

  // ---------------------------------------------------------------- duyệt

  resolve(view: IImportRowView, action: ImportAction, listingId?: number | null): void {
    const row = view.row;
    this.busyRow.set(row.id);
    this.rowError.set(null);
    this.service.resolve(row.id, action, listingId).subscribe({
      next: updated => {
        this.busyRow.set(null);
        const result = this.translate.instant(statusLabelKey(updated.status, false));
        this.alerts.addAlert({
          type: 'success',
          translationKey: 'javaSpringBootBackEndApp.listingImport.review.done',
          translationParams: { row: row.rowNo, result },
        });
        this.loadBatch();
        this.loadRows();
      },
      error: (err: HttpErrorResponse) => {
        this.busyRow.set(null);
        this.rowError.set({ rowId: row.id, message: err.error?.error ?? err.error?.detail ?? err.message });
      },
    });
  }

  mergeOther(view: IImportRowView): void {
    const id = this.otherId[view.row.id];
    if (id && id > 0) {
      this.resolve(view, 'MERGE', id);
    }
  }

  // ---------------------------------------------------------------- xuất bản

  publish(): void {
    this.publishing.set(true);
    this.service.publish(this.batchId).subscribe({
      next: count => {
        this.publishing.set(false);
        this.confirmPublish.set(false);
        this.alerts.addAlert({
          type: 'success',
          translationKey: 'javaSpringBootBackEndApp.listingImport.action.published',
          translationParams: { count: count.toLocaleString('vi-VN') },
        });
        this.loadBatch();
      },
      error: () => {
        this.publishing.set(false);
        this.confirmPublish.set(false);
      },
    });
  }

  fmt(n: number | null | undefined): string {
    return (n ?? 0).toLocaleString('vi-VN');
  }
}
