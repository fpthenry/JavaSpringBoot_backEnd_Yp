import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbPagination } from '@ng-bootstrap/ng-bootstrap/pagination';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { Subscription, switchMap, timer } from 'rxjs';

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
  messageList,
  reasonLabelKey,
  scorePercent,
  sourceCode,
  sourceKind,
  statusCount,
  statusLabelKey,
} from '../listing-import-display';
import { IImportBatch, IImportRowView, ImportAction, ImportRowStatus } from '../listing-import.model';
import { ListingImportService } from '../service/listing-import.service';

const ROWS_PER_PAGE = 50;
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
  protected readonly messageList = messageList;
  protected readonly compareLines = compareLines;

  protected readonly running = computed(() => this.batch()?.status === 'RUNNING');
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
  private readonly alerts = inject(AlertService);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);
  private poll?: Subscription;
  private batchId = 0;

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      this.batchId = Number(params.get('id'));
      this.filter.set(null);
      this.rowPage.set(1);
      this.loadBatch();
      this.loadRows();
    });
  }

  loadBatch(): void {
    this.service.batch(this.batchId).subscribe(b => {
      this.batch.set(b);
      this.now.set(Date.now());
      if (b.status === 'RUNNING') {
        this.startPolling();
      }
    });
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
        if (b.status !== 'RUNNING') {
          this.poll?.unsubscribe();
          this.loadRows();
        }
      });
  }

  loadRows(): void {
    this.loadingRows.set(true);
    this.service.rows(this.batchId, this.filter(), this.rowPage() - 1, ROWS_PER_PAGE).subscribe({
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
