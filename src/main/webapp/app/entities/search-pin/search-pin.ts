import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { AccountService } from 'app/core/auth';
import { IListing } from 'app/entities/listing/listing.model';
import { ListingService } from 'app/entities/listing/service/listing.service';
import { Alert, AlertError } from 'app/shared/alert';
import { canEditAuthorities } from 'app/shared/auth/permissions';
import { TranslateDirective } from 'app/shared/language';

import { PinGroup, PinItem, SearchPinService, pinKey } from './search-pin.service';

/** Website công khai, để mở thử trang tìm kiếm với từ khóa. */
const WEBSITE = 'https://yp.vn';

/**
 * Sửa tay (2026-11-17): trang "Ghim kết quả tìm kiếm". Mỗi từ khóa có danh sách doanh nghiệp ghim theo thứ tự: khách tìm
 * đúng từ khóa (không phân biệt dấu, hoa thường) thì các doanh nghiệp này hiện đầu tiên. Thay "Custom Search Results" của
 * ElasticPress trên WordPress cũ (dữ liệu cũ đã được nhập sẵn).
 */
@Component({
  selector: 'jhi-search-pin',
  templateUrl: './search-pin.html',
  styleUrl: './search-pin.scss',
  imports: [FormsModule, RouterLink, FontAwesomeModule, TranslatePipe, TranslateDirective, Alert, AlertError],
})
export class SearchPin implements OnInit {
  readonly groups = signal<PinGroup[]>([]);
  readonly isLoading = signal(false);
  readonly busy = signal(false);
  readonly filter = signal('');
  readonly selectedKey = signal<string | null>(null);
  /** Từ khóa mới đang soạn, chưa có doanh nghiệp nào nên chưa có trên server. */
  readonly draft = signal<PinGroup | null>(null);
  readonly newKeyword = signal('');

  readonly listingQuery = signal('');
  readonly listingResults = signal<IListing[]>([]);
  readonly searching = signal(false);
  readonly searched = signal(false);

  readonly canEdit = computed(() => !!this.account() && this.accountService.hasAnyAuthority(canEditAuthorities('listing')));

  readonly visibleGroups = computed(() => {
    const filter = pinKey(this.filter());
    return this.groups().filter(group => !filter || group.key.includes(filter));
  });

  readonly selected = computed<PinGroup | null>(() => {
    const key = this.selectedKey();
    const draft = this.draft();
    return this.groups().find(group => group.key === key) ?? (draft?.key === key ? draft : null);
  });

  readonly pinnedListingIds = computed(() => new Set((this.selected()?.items ?? []).map(item => item.listingId)));

  private readonly accountService = inject(AccountService);
  // eslint-disable-next-line @typescript-eslint/member-ordering
  readonly account = this.accountService.account;
  private readonly service = inject(SearchPinService);
  private readonly listingService = inject(ListingService);

  ngOnInit(): void {
    this.load();
  }

  load(selectKey?: string): void {
    this.isLoading.set(true);
    this.service
      .groups()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe(groups => {
        this.groups.set(groups);
        const key = selectKey ?? this.selectedKey();
        if (key && groups.some(group => group.key === key)) {
          this.select(key);
        } else if (!this.draft() && groups.length) {
          this.select(groups[0].key);
        }
      });
  }

  select(key: string): void {
    this.selectedKey.set(key);
    if (this.draft()?.key !== key) {
      this.draft.set(null);
    }
    this.listingResults.set([]);
    this.searched.set(false);
  }

  /** Tạo từ khóa mới (chưa lưu cho tới khi ghim doanh nghiệp đầu tiên); trùng từ khóa đã có thì chọn từ khóa đó. */
  addKeyword(): void {
    const keyword = this.newKeyword().trim();
    const key = pinKey(keyword);
    if (!key) {
      return;
    }
    this.newKeyword.set('');
    if (this.groups().some(group => group.key === key)) {
      this.select(key);
      return;
    }
    this.draft.set({ keyword, key, items: [] });
    this.select(key);
  }

  searchListings(): void {
    const query = this.listingQuery().trim();
    if (!query) {
      return;
    }
    this.searching.set(true);
    this.listingService
      .search({ query, page: 0, size: 10, sort: [] })
      .pipe(finalize(() => this.searching.set(false)))
      .subscribe(listings => {
        this.listingResults.set(listings);
        this.searched.set(true);
      });
  }

  pin(listing: IListing): void {
    const group = this.selected();
    if (!group) {
      return;
    }
    this.busy.set(true);
    this.service
      .add(group.keyword, listing.id)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe(updated => {
        this.draft.set(null);
        this.replaceGroup(updated);
        this.selectedKey.set(updated.key);
      });
  }

  move(item: PinItem, delta: number): void {
    const group = this.selected();
    if (!group) {
      return;
    }
    const ids = group.items.map(pin => pin.id);
    const from = ids.indexOf(item.id);
    const to = from + delta;
    if (from < 0 || to < 0 || to >= ids.length) {
      return;
    }
    [ids[from], ids[to]] = [ids[to], ids[from]];
    this.busy.set(true);
    this.service
      .reorder(ids)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe(updated => this.replaceGroup(updated));
  }

  unpin(item: PinItem): void {
    this.busy.set(true);
    this.service
      .remove(item.id)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe(() => this.load(this.selectedKey() ?? undefined));
  }

  removeGroup(group: PinGroup): void {
    if (!globalThis.confirm(`Xóa từ khóa "${group.keyword}" và bỏ ghim ${group.items.length} doanh nghiệp?`)) {
      return;
    }
    this.busy.set(true);
    this.service
      .removeGroup(group.keyword)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe(() => {
        this.selectedKey.set(null);
        this.load();
      });
  }

  websiteSearchUrl(keyword: string): string {
    return `${WEBSITE}/listing?q=${encodeURIComponent(keyword)}`;
  }

  private replaceGroup(updated: PinGroup): void {
    this.groups.update(groups => {
      const rest = groups.filter(group => group.key !== updated.key);
      return updated.items.length ? [...rest, updated].sort((a, b) => a.key.localeCompare(b.key)) : rest;
    });
  }
}
