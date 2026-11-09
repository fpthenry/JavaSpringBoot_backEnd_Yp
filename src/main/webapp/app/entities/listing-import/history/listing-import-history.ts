import { DatePipe } from '@angular/common';
import { Component, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { TranslatePipe } from '@ngx-translate/core';

import { TranslateDirective } from 'app/shared/language';
import { shortValue, visibleChanges } from '../listing-import-display';
import { IListingHistory } from '../listing-import.model';
import { ListingImportService } from '../service/listing-import.service';

/** Sửa tay: khối "Lịch sử nhập" ở trang doanh nghiệp: mỗi lần nhập đổi gì, từ bản nguồn nào. Chỉ ROLE_ADMIN đọc được, không có quyền thì ẩn. */
@Component({
  selector: 'jhi-listing-import-history',
  templateUrl: './listing-import-history.html',
  imports: [RouterLink, FontAwesomeModule, TranslatePipe, TranslateDirective, DatePipe],
})
export class ListingImportHistory {
  readonly listingId = input.required<number>();

  protected readonly history = signal<IListingHistory | null>(null);
  protected readonly visibleChanges = visibleChanges;
  protected readonly shortValue = shortValue;

  private readonly service = inject(ListingImportService);

  constructor() {
    effect(() => {
      this.service.history(this.listingId()).subscribe({
        next: history => this.history.set(history),
        error: () => this.history.set(null),
      });
    });
  }
}
