import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbPagination } from '@ng-bootstrap/ng-bootstrap/pagination';
import { TranslatePipe } from '@ngx-translate/core';
import { Subject, debounceTime, distinctUntilChanged, finalize, switchMap } from 'rxjs';

import { shortCount } from 'app/entities/category/index/category-index';
import { AlertError } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import {
  LocationGeneration,
  LocationIndexFilter,
  LocationIndexItem,
  LocationIndexPage,
  LocationIndexService,
  LocationLetter,
  LocationLevel,
} from '../service/location-index.service';

export type LocationIndexSort = 'name' | 'count';

const GENERATIONS: LocationGeneration[] = ['new', 'old', 'all'];
const LEVELS: LocationLevel[] = ['province', 'district', 'ward'];

/** Dòng phụ dưới tên: "Quận Hai Bà Trưng, Thành phố Hà Nội"; xã mới chỉ có tỉnh; tỉnh thì không có. */
export function placeOf(item: LocationIndexItem): string | null {
  if (item.parentId === null || !item.provinceName) {
    return null;
  }
  return item.parentName && item.parentName !== item.provinceName ? `${item.parentName}, ${item.provinceName}` : item.provinceName;
}

/**
 * Mục lục tỉnh thành: dãy nút chữ cái A–Z (Đ riêng), ô tìm theo tên (không dấu, cả tên tỉnh), thẻ đơn vị kèm số doanh
 * nghiệp (gồm đơn vị con). Mặc định địa giới mới (từ 01/7/2025), chuyển được sang cũ hoặc cả hai; lọc theo cấp. Bấm thẻ
 * mở danh sách doanh nghiệp lọc theo đơn vị đó. Trạng thái lưu trên URL (letter, q, page, sort, all, gen, level).
 */
@Component({
  selector: 'jhi-location-index',
  templateUrl: './location-index.html',
  styleUrl: './location-index.scss',
  imports: [RouterLink, FormsModule, FontAwesomeModule, NgbPagination, TranslatePipe, TranslateDirective, AlertError],
})
export class LocationIndex {
  static readonly PAGE_SIZE = 60;

  readonly letters = signal<LocationLetter[]>([]);
  readonly letter = signal<string | null>(null);
  readonly query = signal('');
  readonly hideEmpty = signal(true);
  readonly sort = signal<LocationIndexSort>('name');
  readonly generation = signal<LocationGeneration>('new');
  readonly level = signal<LocationLevel | null>(null);
  /** Trang hiện tại, bắt đầu từ 1 (ngb-pagination). */
  readonly page = signal(1);
  readonly result = signal<LocationIndexPage | null>(null);
  readonly isLoading = signal(false);
  readonly pageSize = LocationIndex.PAGE_SIZE;
  readonly generations = GENERATIONS;

  protected readonly shortCount = shortCount;
  protected readonly placeOf = placeOf;
  protected readonly locationIndexService = inject(LocationIndexService);
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
    const generation = params.get('gen') as LocationGeneration | null;
    this.generation.set(generation && GENERATIONS.includes(generation) ? generation : 'new');
    const level = params.get('level') as LocationLevel | null;
    this.level.set(level && this.levels().includes(level) ? level : null);
    this.page.set(Math.max(Number(params.get('page') ?? 1) || 1, 1));

    const destroyRef = inject(DestroyRef);
    // switchMap: chỉ lấy kết quả của lần gọi mới nhất (bấm chữ cái liên tục, gõ nhanh)
    this.reload$
      .pipe(
        switchMap(() => {
          this.isLoading.set(true);
          return this.locationIndexService
            .search({
              ...this.filter(),
              letter: this.letter(),
              q: this.query().trim(),
              page: this.page() - 1,
              size: LocationIndex.PAGE_SIZE,
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

  /** Bộ mới không có cấp quận/huyện. */
  levels(): LocationLevel[] {
    return this.generation() === 'new' ? LEVELS.filter(level => level !== 'district') : LEVELS;
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
    this.filterChanged();
  }

  changeGeneration(generation: LocationGeneration): void {
    this.generation.set(generation);
    const level = this.level();
    if (level && !this.levels().includes(level)) {
      this.level.set(null);
    }
    this.filterChanged();
  }

  changeLevel(level: LocationLevel | null): void {
    this.level.set(level);
    this.filterChanged();
  }

  changeSort(sort: LocationIndexSort): void {
    this.sort.set(sort);
    this.page.set(1);
    this.refresh();
  }

  navigateToPage(page: number): void {
    this.page.set(page);
    this.refresh();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  listingQueryParams(item: LocationIndexItem): Record<string, number> {
    return { 'filter[locationTreeId.equals]': item.id };
  }

  formatCount(count: number): string {
    return count.toLocaleString('vi-VN');
  }

  trackId(item: LocationIndexItem): number {
    return item.id;
  }

  protected filter(): LocationIndexFilter {
    return { generation: this.generation(), level: this.level(), hideEmpty: this.hideEmpty() };
  }

  /** Đổi địa giới, cấp, ẩn nơi trống: dãy chữ cái đổi theo, về trang 1. */
  protected filterChanged(): void {
    this.page.set(1);
    this.loadLetters();
    this.refresh();
  }

  protected loadLetters(): void {
    this.locationIndexService.letters(this.filter()).subscribe(letters => this.letters.set(letters));
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
        gen: this.generation() === 'new' ? null : this.generation(),
        level: this.level(),
      },
      replaceUrl: true,
    });
    this.reload$.next();
  }
}
