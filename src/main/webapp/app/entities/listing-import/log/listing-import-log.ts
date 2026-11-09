import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbPagination } from '@ng-bootstrap/ng-bootstrap/pagination';
import { TranslatePipe } from '@ngx-translate/core';

import { TOTAL_COUNT_RESPONSE_HEADER } from 'app/config';
import { TranslateDirective } from 'app/shared/language';
import { rejectedCount, sourceKind } from '../listing-import-display';
import { IImportBatch } from '../listing-import.model';
import { ListingImportService } from '../service/listing-import.service';

const PAGE_SIZE = 20;

/** Sửa tay: trang "Lịch sử nhập": chỉ các lần đã nhập vào hệ thống, không có lần kiểm tra. Chỉ ROLE_ADMIN. */
@Component({
  selector: 'jhi-listing-import-log',
  templateUrl: './listing-import-log.html',
  imports: [RouterLink, FontAwesomeModule, NgbPagination, TranslateDirective, TranslatePipe, DatePipe],
})
export class ListingImportLog implements OnInit {
  protected readonly batches = signal<IImportBatch[]>([]);
  protected readonly total = signal(0);
  protected readonly page = signal(1);
  protected readonly loading = signal(false);
  protected readonly pageSize = PAGE_SIZE;
  protected readonly sourceKind = sourceKind;
  protected readonly rejectedCount = rejectedCount;

  private readonly service = inject(ListingImportService);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.service.batches(this.page() - 1, PAGE_SIZE, false).subscribe({
      next: res => {
        this.batches.set(res.body ?? []);
        this.total.set(Number(res.headers.get(TOTAL_COUNT_RESPONSE_HEADER) ?? 0));
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  changePage(page: number): void {
    this.page.set(page);
    this.load();
  }

  fmt(n: number | null | undefined): string {
    return (n ?? 0).toLocaleString('vi-VN');
  }
}
