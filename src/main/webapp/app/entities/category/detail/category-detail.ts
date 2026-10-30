import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  NgbDropdown,
  NgbDropdownButtonItem,
  NgbDropdownItem,
  NgbDropdownMenu,
  NgbDropdownToggle,
} from '@ng-bootstrap/ng-bootstrap/dropdown';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap/modal';
import { TranslatePipe } from '@ngx-translate/core';
import { filter } from 'rxjs';

import { ITEM_DELETED_EVENT, TOTAL_COUNT_RESPONSE_HEADER } from 'app/config';
import { IListing } from 'app/entities/listing/listing.model';
import { ListingService } from 'app/entities/listing/service/listing.service';
import { Alert, AlertError } from 'app/shared/alert';
import { FormatMediumDatePipe } from 'app/shared/date';
import { CopyButton } from 'app/shared/detail';
import { decodeEntities } from 'app/shared/form';
import { TranslateDirective } from 'app/shared/language';
import { CategoryRef, loadCategoryPath } from '../category-path';
import { ICategory } from '../category.model';
import { CategoryDeleteDialog } from '../delete/category-delete-dialog';
import { CategoryService } from '../service/category.service';

/** Số doanh nghiệp mới cập nhật hiện trên trang xem. */
export const RECENT_LISTINGS = 5;

/**
 * Sửa tay: trang xem ngành nghề VSIC 2025: mã ngành trên tiêu đề, đường dẫn cây, ngành con (xếp theo mã); cấp lấy từ
 * cột level (máy chủ suy từ mã). Bố cục gọn: đầu trang (đường dẫn trong cây, tên, một dòng cấp · số doanh nghiệp · số ngành
 * con, nút Xem doanh nghiệp / Sửa, thao tác ít dùng trong menu ⋯), mô tả, ngành con dạng danh sách 2 cột (10 dòng đầu,
 * "Xem thêm"), doanh nghiệp mới cập nhật (lọc theo cả cây ngành trên Elasticsearch), cột phải gom thông tin. Phím E để sửa.
 * Chưa xóa được khi còn ngành con.
 */
@Component({
  selector: 'jhi-category-detail',
  templateUrl: './category-detail.html',
  imports: [
    FontAwesomeModule,
    Alert,
    AlertError,
    TranslateDirective,
    TranslatePipe,
    RouterLink,
    FormatMediumDatePipe,
    CopyButton,
    NgbDropdown,
    NgbDropdownToggle,
    NgbDropdownMenu,
    NgbDropdownItem,
    // Cần cho <button ngbDropdownItem [disabled]>: thiếu thì [disabled] chỉ vào input của NgbDropdownItem, nút vẫn bấm được
    NgbDropdownButtonItem,
  ],
  host: { '(document:keydown)': 'onKeydown($event)' },
})
export class CategoryDetail {
  readonly category = input<ICategory | null>(null);

  /** Số ngành con hiện trước khi bấm "Xem thêm". */
  readonly childLimit = 10;
  readonly showAllChildren = signal(false);
  /** Ngành gốc → ngành cha (không gồm ngành đang xem). */
  readonly ancestors = signal<CategoryRef[]>([]);
  readonly children = signal<ICategory[] | null>(null);
  readonly recentListings = signal<IListing[] | null>(null);
  readonly recentTotal = signal<number | null>(null);

  readonly name = computed(() => decodeEntities(this.category()?.name));
  readonly level = computed(() => this.category()?.level ?? this.ancestors().length + 1);
  readonly childCount = computed(() => this.children()?.length ?? null);
  readonly visibleChildren = computed(() => {
    const list = this.children() ?? [];
    return this.showAllChildren() ? list : list.slice(0, this.childLimit);
  });
  readonly description = computed(() => this.category()?.description?.trim() ?? '');
  /** Chữ cái của ngành trong mục lục A–Z (bỏ dấu, Đ → D như mục lục ngành nghề). */
  readonly letter = computed(() => {
    const first = [...this.name().trim()][0] ?? '';
    const folded = first.normalize('NFD').replace(/\p{M}/gu, '').replace(/[đĐ]/g, 'D').toUpperCase();
    return /^[A-Z]$/.test(folded) ? folded : '#';
  });

  protected readonly decode = decodeEntities;
  protected readonly categoryService = inject(CategoryService);
  protected readonly listingService = inject(ListingService);
  protected readonly modalService = inject(NgbModal);
  protected readonly router = inject(Router);

  constructor() {
    effect(() => {
      const category = this.category();
      this.ancestors.set([]);
      this.children.set(null);
      this.recentListings.set(null);
      this.showAllChildren.set(false);
      if (category) {
        this.loadRelated(category);
      }
    });
  }

  listingQueryParams(id: number): Record<string, number> {
    return { 'filter[categoryTreeId.equals]': id };
  }

  editQueryParams(field: string): Record<string, string> {
    return { focus: field };
  }

  newChildQueryParams(id: number): Record<string, number> {
    return { parentId: id };
  }

  indexQueryParams(): Record<string, string> {
    return { letter: this.letter(), q: this.name() };
  }

  /** Phím E (không gõ trong ô nhập, không giữ Ctrl/Alt): mở form sửa. */
  onKeydown(event: KeyboardEvent): void {
    const target = event.target as HTMLElement | null;
    const typing = !!target?.closest('input, textarea, select, [contenteditable="true"]');
    const category = this.category();
    if (category && !typing && !event.ctrlKey && !event.metaKey && !event.altKey && event.key.toLowerCase() === 'e') {
      event.preventDefault();
      void this.router.navigate(['/category', category.id, 'edit']);
    }
  }

  delete(category: ICategory): void {
    const modalRef = this.modalService.open(CategoryDeleteDialog, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.category = category;
    modalRef.closed.pipe(filter(reason => reason === ITEM_DELETED_EVENT)).subscribe(() => void this.router.navigate(['/category']));
  }

  /** Về trang trước trong ứng dụng; mở thẳng trang xem (link, tab mới) thì về danh sách. */
  previousState(): void {
    const navigationId = (globalThis.history.state as { navigationId?: number } | null)?.navigationId ?? 1;
    if (navigationId > 1) {
      globalThis.history.back();
    } else {
      void this.router.navigate(['/category']);
    }
  }

  protected loadRelated(category: ICategory): void {
    loadCategoryPath(this.categoryService, category.parent).subscribe(path => this.ancestors.set(path));
    this.categoryService.query({ 'parentId.equals': category.id, size: 500, sort: ['code,asc', 'name,asc'] }).subscribe({
      next: response => this.children.set(response.body ?? []),
      error: () => this.children.set([]),
    });
    // Lọc theo cả cây ngành chạy trên Elasticsearch (dưới 0,3 s kể cả nhóm VSIC lớn)
    this.listingService
      .query({ 'categoryTreeId.equals': category.id, page: 0, size: RECENT_LISTINGS, sort: ['updatedAt,desc'] })
      .subscribe({
        next: response => {
          this.recentListings.set(response.body ?? []);
          const total = Number(response.headers.get(TOTAL_COUNT_RESPONSE_HEADER));
          this.recentTotal.set(Number.isFinite(total) ? total : null);
        },
        error: () => this.recentListings.set([]),
      });
  }
}
