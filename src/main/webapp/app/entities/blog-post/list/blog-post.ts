import { HttpHeaders } from '@angular/common/http';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Data, ParamMap, Router, RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap/modal';
import { NgbPagination } from '@ng-bootstrap/ng-bootstrap/pagination';
import { TranslatePipe } from '@ngx-translate/core';
import { combineLatest, filter, map, tap } from 'rxjs';

import { DEFAULT_SORT_DATA, ITEMS_PER_PAGE, ITEM_DELETED_EVENT, PAGE_HEADER, SORT, TOTAL_COUNT_RESPONSE_HEADER } from 'app/config';
import { DataUtils } from 'app/core/util';
import { Alert, AlertError } from 'app/shared/alert';
import { FormatMediumDatePipe } from 'app/shared/date';
import { Filter, FilterOptions, IFilterOption, IFilterOptions } from 'app/shared/filter';
import { TranslateDirective } from 'app/shared/language';
import { ItemCount } from 'app/shared/pagination';
import { SortByDirective, SortDirective, SortService, type SortState, sortStateSignal } from 'app/shared/sort';
import { BlogCategoryService } from 'app/entities/blog-category/service/blog-category.service';
import { TagService } from 'app/entities/tag/service/tag.service';
import { decodeEntities } from 'app/shared/form';
import { BLOG_POST_STATUSES, CategoryOption, blogStatusLabelKey, categoryTreeOptions, httpsUrl, plainExcerpt } from '../blog-post-display';
import { IBlogPost } from '../blog-post.model';
import { BlogPostDeleteDialog } from '../delete/blog-post-delete-dialog';
import { BlogPostService } from '../service/blog-post.service';

@Component({
  selector: 'jhi-blog-post',
  templateUrl: './blog-post.html',
  imports: [
    RouterLink,
    FormsModule,
    FontAwesomeModule,
    AlertError,
    Alert,
    SortDirective,
    SortByDirective,
    TranslateDirective,
    TranslatePipe,
    FormatMediumDatePipe,
    Filter,
    NgbPagination,
    ItemCount,
  ],
})
export class BlogPost {
  // Sửa tay: trên Elasticsearch, id là keyword (sắp theo chữ) nên cũng bỏ
  private static readonly NOT_SORTABLE_FIELDS_AFTER_SEARCH = [
    'id',
    'title',
    'slug',
    'content',
    'excerpt',
    'thumbnail',
    'status',
    'authorName',
  ];

  readonly blogPosts = signal<IBlogPost[]>([]);

  /** Sửa tay: lọc theo trạng thái, danh mục (gồm danh mục con), thẻ; lưu trên URL (status, categoryId, tagId). */
  readonly statusFilter = signal<string | null>(null);
  readonly categoryFilter = signal<number | null>(null);
  readonly tagFilter = signal<number | null>(null);
  readonly statuses = BLOG_POST_STATUSES;
  /** Số bài theo trạng thái cho các tab (null = đang đếm). */
  readonly statusCounts = signal<Record<string, number | null>>({});
  readonly categoryOptions = signal<CategoryOption[]>([]);
  readonly tagOptions = signal<{ id: number; name: string }[]>([]);
  readonly hasFilters = computed(() => this.statusFilter() !== null || this.categoryFilter() !== null || this.tagFilter() !== null);
  /** Đang tìm trên Elasticsearch (có từ khóa hoặc bộ lọc): chỉ sắp xếp được theo ngày đăng, lượt xem. */
  readonly searching = computed(() => !!this.currentSearch() || this.hasFilters());

  sortState = sortStateSignal({});
  readonly currentSearch = signal('');
  filters: IFilterOptions = new FilterOptions();

  readonly itemsPerPage = signal(ITEMS_PER_PAGE);
  readonly totalItems = signal(0);
  readonly page = signal(1);

  readonly router = inject(Router);
  protected readonly blogPostService = inject(BlogPostService);
  // eslint-disable-next-line @typescript-eslint/member-ordering
  readonly isLoading = this.blogPostService.blogPostsResource.isLoading;
  protected readonly activatedRoute = inject(ActivatedRoute);
  protected readonly activatedRouteState = toSignal(
    combineLatest([this.activatedRoute.queryParamMap, this.activatedRoute.data]).pipe(
      map(([queryParamMap, data]) => ({ queryParamMap, data })),
    ),
    { initialValue: { queryParamMap: this.activatedRoute.snapshot.queryParamMap, data: this.activatedRoute.snapshot.data } },
  );
  protected readonly sortService = inject(SortService);
  protected readonly filterOptions = toSignal(this.filters.filterChanges);
  protected dataUtils = inject(DataUtils);
  protected modalService = inject(NgbModal);
  protected blogCategoryService = inject(BlogCategoryService);
  protected tagService = inject(TagService);
  protected readonly plainExcerpt = plainExcerpt;
  protected readonly decode = decodeEntities;
  protected readonly blogStatusLabelKey = blogStatusLabelKey;
  protected readonly httpsUrl = httpsUrl;

  constructor() {
    this.loadFilterOptions();
    effect(() => {
      const headers = this.blogPostService.blogPostsResource.headers();
      if (headers) {
        this.fillComponentAttributesFromResponseHeader(headers);
      }
    });
    effect(() => {
      this.blogPosts.set(this.fillComponentAttributesFromResponseBody([...this.blogPostService.blogPosts()]));
    });
    effect(() => {
      const activatedRouteState = this.activatedRouteState();
      untracked(() => {
        // Only watch for route changes. Other signals should be ignored.
        this.fillComponentAttributeFromRoute(activatedRouteState.queryParamMap, activatedRouteState.data);
        this.load();
      });
    });

    effect(() => {
      const filterOptions = this.filterOptions();
      if (filterOptions) {
        untracked(() => {
          // Only watch for filter changes. Other signals should be ignored.
          this.handleNavigation(1, this.sortState(), filterOptions);
        });
      }
    });
  }

  trackId = (item: IBlogPost): number => this.blogPostService.getBlogPostIdentifier(item);

  search(query: string): void {
    this.page.set(1);
    this.currentSearch.set(query);
    // Sửa tay: có từ khóa thì bỏ sắp xếp để Elasticsearch xếp theo độ liên quan (bấm tiêu đề cột vẫn sắp xếp được);
    // xóa từ khóa thì quay về sắp xếp mặc định
    this.navigateToWithComponentValues(query ? {} : this.getDefaultSortState());
  }

  setStatus(status: string | null): void {
    this.statusFilter.set(status);
    this.applyFilters();
  }

  setCategory(id: number | null): void {
    this.categoryFilter.set(id);
    this.applyFilters();
  }

  setTag(id: number | null): void {
    this.tagFilter.set(id);
    this.applyFilters();
  }

  clearFilters(): void {
    this.statusFilter.set(null);
    this.categoryFilter.set(null);
    this.tagFilter.set(null);
    this.search('');
  }

  /** Sắp xếp được cột này không (trên Elasticsearch chỉ ngày đăng, lượt xem). */
  sortable(field: string): boolean {
    return !this.searching() || !BlogPost.NOT_SORTABLE_FIELDS_AFTER_SEARCH.includes(field);
  }

  getDefaultSortState(): SortState {
    return this.sortService.parseSortParam(this.activatedRoute.snapshot.data[DEFAULT_SORT_DATA]);
  }

  byteSize(base64String: string): string {
    return this.dataUtils.byteSize(base64String);
  }

  openFile(base64String: string, contentType: string | null | undefined): void {
    return this.dataUtils.openFile(base64String, contentType);
  }

  delete(blogPost: IBlogPost): void {
    const modalRef = this.modalService.open(BlogPostDeleteDialog, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.blogPost = blogPost;
    // unsubscribe not needed because closed completes on modal close
    modalRef.closed
      .pipe(
        filter(reason => reason === ITEM_DELETED_EVENT),
        tap(() => this.load()),
      )
      .subscribe();
  }

  load(): void {
    this.queryBackend();
  }

  navigateToWithComponentValues(event: SortState): void {
    this.handleNavigation(this.page(), event, this.filters.filterOptions, this.currentSearch());
  }

  navigateToPage(page: number): void {
    this.handleNavigation(page, this.sortState(), this.filters.filterOptions, this.currentSearch());
  }

  protected fillComponentAttributeFromRoute(params: ParamMap, data: Data): void {
    const page = params.get(PAGE_HEADER);
    this.page.set(+(page ?? 1));
    const searching = params.has('search') && params.get('search') !== '';
    // Sửa tay: đang tìm mà URL không có sort thì giữ trống (theo độ liên quan), không dùng sort mặc định id,asc
    this.sortState.set(this.sortService.parseSortParam(params.get(SORT) ?? (searching ? undefined : data[DEFAULT_SORT_DATA])));
    this.filters.initializeFromParams(params);
    const numberParam = (name: string): number | null => Number(params.get(name)) || null;
    this.statusFilter.set(params.get('status'));
    this.categoryFilter.set(numberParam('categoryId'));
    this.tagFilter.set(numberParam('tagId'));
    if (searching || this.hasFilters()) {
      this.currentSearch.set(params.get('search') ?? '');
      const { predicate } = this.sortState();
      if (predicate && BlogPost.NOT_SORTABLE_FIELDS_AFTER_SEARCH.includes(predicate)) {
        this.sortState.set({});
      }
    }
  }

  protected fillComponentAttributesFromResponseBody(data: IBlogPost[]): IBlogPost[] {
    return data;
  }

  protected fillComponentAttributesFromResponseHeader(headers: HttpHeaders): void {
    this.totalItems.set(Number(headers.get(TOTAL_COUNT_RESPONSE_HEADER)));
  }

  protected queryBackend(): void {
    const pageToLoad: number = this.page();
    const queryObject: any = {
      page: pageToLoad - 1,
      size: this.itemsPerPage(),
      eagerload: true,
      query: this.currentSearch(),
      sort: this.sortService.buildSortParam(this.sortState()),
    };
    if (this.statusFilter()) {
      queryObject.status = this.statusFilter();
    }
    if (this.categoryFilter() !== null) {
      queryObject.categoryId = this.categoryFilter();
    }
    if (this.tagFilter() !== null) {
      queryObject.tagId = this.tagFilter();
    }
    for (const filterOption of this.filters.filterOptions) {
      queryObject[filterOption.name] = filterOption.values;
    }
    this.blogPostService.blogPostsParams.set(queryObject);
  }

  protected handleNavigation(page: number, sortState: SortState, filterOptions?: IFilterOption[], currentSearch?: string): void {
    const queryParamsObj: any = {
      search: currentSearch,
      page,
      size: this.itemsPerPage(),
      sort: this.sortService.buildSortParam(sortState),
      status: this.statusFilter() ?? undefined,
      categoryId: this.categoryFilter() ?? undefined,
      tagId: this.tagFilter() ?? undefined,
    };

    if (filterOptions) {
      for (const filterOption of filterOptions) {
        queryParamsObj[filterOption.nameAsQueryParam()] = filterOption.values;
      }
    }

    this.router.navigate(['./'], {
      relativeTo: this.activatedRoute,
      queryParams: queryParamsObj,
    });
  }

  /** Đổi bộ lọc: về trang 1; sắp xếp theo cột không dùng được trên Elasticsearch thì bỏ. */
  protected applyFilters(): void {
    this.page.set(1);
    const { predicate } = this.sortState();
    const sort = this.searching() && predicate && BlogPost.NOT_SORTABLE_FIELDS_AFTER_SEARCH.includes(predicate) ? {} : this.sortState();
    this.handleNavigation(1, sort, this.filters.filterOptions, this.currentSearch());
  }

  /** Danh mục (theo cây), thẻ, số bài theo trạng thái: đọc một lần khi mở trang. */
  protected loadFilterOptions(): void {
    this.blogCategoryService.query({ size: 500 }).subscribe({
      next: response => this.categoryOptions.set(categoryTreeOptions(response.body ?? [])),
      error: () => this.categoryOptions.set([]),
    });
    this.tagService.query({ size: 500, sort: ['name,asc'] }).subscribe({
      next: response => this.tagOptions.set((response.body ?? []).map(tag => ({ id: tag.id, name: decodeEntities(tag.name) }))),
      error: () => this.tagOptions.set([]),
    });
    for (const status of [null, ...BLOG_POST_STATUSES]) {
      const key = status ?? 'all';
      this.blogPostService.count(status ? { 'status.equals': status } : {}).subscribe({
        next: count => this.statusCounts.update(counts => ({ ...counts, [key]: count })),
        error: () => this.statusCounts.update(counts => ({ ...counts, [key]: null })),
      });
    }
  }
}
