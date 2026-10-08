import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbPagination } from '@ng-bootstrap/ng-bootstrap/pagination';
import { TranslatePipe } from '@ngx-translate/core';
import { Subject, debounceTime, distinctUntilChanged, finalize, switchMap } from 'rxjs';

import { AlertError } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { CategoryIndexItem, CategoryIndexPage, CategoryIndexService, CategoryLetter } from '../service/category-index.service';

export type CategoryIndexSort = 'name' | 'count';

/** Số doanh nghiệp rút gọn cho ô vuông bên trái thẻ ngành: 74, 1k, 321k, 1,1tr. */
export function shortCount(count: number): string {
  if (count < 1000) {
    return String(count);
  }
  if (count < 1_000_000) {
    return `${Math.floor(count / 1000)}k`;
  }
  // Dấu phẩy thập phân kiểu Việt; không dùng toLocaleString để không phụ thuộc ICU của môi trường
  return `${String(Math.floor(count / 100_000) / 10).replace('.', ',')}tr`;
}

/**
 * Mục lục ngành nghề: dãy nút chữ cái A–Z, ô tìm theo tên (không dấu), thẻ ngành kèm số doanh nghiệp (gồm ngành con).
 * Bấm thẻ ngành mở danh sách doanh nghiệp lọc theo cả cây ngành đó. Trạng thái lưu trên URL (letter, q, page, sort, all)
 * để quay lại, chia sẻ link được.
 */
@Component({
  selector: 'jhi-category-index',
  templateUrl: './category-index.html',
  styleUrl: './category-index.scss',
  imports: [RouterLink, FormsModule, FontAwesomeModule, NgbPagination, TranslatePipe, TranslateDirective, AlertError],
})
export class CategoryIndex {
  static readonly PAGE_SIZE = 60;

  readonly letters = signal<CategoryLetter[]>([]);
  readonly letter = signal<string | null>(null);
  readonly query = signal('');
  readonly hideEmpty = signal(true);
  readonly sort = signal<CategoryIndexSort>('name');
  /** Trang hiện tại, bắt đầu từ 1 (ngb-pagination). */
  readonly page = signal(1);
  readonly result = signal<CategoryIndexPage | null>(null);
  readonly isLoading = signal(false);
  readonly pageSize = CategoryIndex.PAGE_SIZE;

  protected readonly shortCount = shortCount;
  protected readonly categoryIndexService = inject(CategoryIndexService);
  protected readonly router = inject(Router);
  protected readonly activatedRoute = inject(ActivatedRoute);

  private readonly reload$ = new Subject<void>();
  private readonly queryInput$ = new Subject<string>();

  constructor() {
    const params = this.activatedRoute.snapshot.queryParamMap;
    this.letter.set(params.get('letter'));
    this.query.set(params.get('q') ?? '');
    this.hideEmpty.set(params.get('all') !== 'true');
    this.sort.set(params.get('sort') === 'count' ? 'count' : 'name');
    this.page.set(Math.max(Number(params.get('page') ?? 1) || 1, 1));

    const destroyRef = inject(DestroyRef);
    // switchMap: chỉ lấy kết quả của lần gọi mới nhất (bấm chữ cái liên tục, gõ nhanh)
    this.reload$
      .pipe(
        switchMap(() => {
          this.isLoading.set(true);
          return this.categoryIndexService
            .search({
              letter: this.letter(),
              q: this.query().trim(),
              hideEmpty: this.hideEmpty(),
              page: this.page() - 1,
              size: CategoryIndex.PAGE_SIZE,
              sort: this.sort() === 'count' ? 'listingCount,desc' : 'name,asc',
            })
            .pipe(finalize(() => this.isLoading.set(false)));
        }),
        takeUntilDestroyed(destroyRef),
      )
      .subscribe(page => this.result.set(page));
    this.queryInput$.pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(destroyRef)).subscribe(() => {
      this.page.set(1);
      this.refresh();
    });

    this.loadLetters();
    this.reload$.next();
  }

  selectLetter(letter: string | null): void {
    this.letter.set(letter);
    this.page.set(1);
    this.refresh();
  }

  onQueryChange(value: string): void {
    this.query.set(value);
    this.queryInput$.next(value.trim());
  }

  clearQuery(): void {
    this.query.set('');
    this.queryInput$.next('');
  }

  toggleHideEmpty(): void {
    this.hideEmpty.update(value => !value);
    this.page.set(1);
    this.loadLetters();
    this.refresh();
  }

  changeSort(sort: CategoryIndexSort): void {
    this.sort.set(sort);
    this.page.set(1);
    this.refresh();
  }

  navigateToPage(page: number): void {
    this.page.set(page);
    this.refresh();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  listingQueryParams(item: CategoryIndexItem): Record<string, number> {
    return { 'filter[categoryTreeId.equals]': item.id };
  }

  formatCount(count: number): string {
    return count.toLocaleString('vi-VN');
  }

  trackId(item: CategoryIndexItem): number {
    return item.id;
  }

  protected loadLetters(): void {
    this.categoryIndexService.letters(this.hideEmpty()).subscribe(letters => this.letters.set(letters));
  }

  /** Ghi trạng thái lên URL (không tạo lịch sử mới cho mỗi lần gõ) rồi tải lại. */
  protected refresh(): void {
    void this.router.navigate([], {
      relativeTo: this.activatedRoute,
      queryParams: {
        letter: this.letter(),
        q: this.query().trim() || null,
        page: this.page() > 1 ? this.page() : null,
        sort: this.sort() === 'count' ? 'count' : null,
        all: this.hideEmpty() ? null : 'true',
      },
      replaceUrl: true,
    });
    this.reload$.next();
  }
}
