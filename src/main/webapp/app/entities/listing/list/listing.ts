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
import { FormatMediumDatetimePipe } from 'app/shared/date';
import { Filter, FilterOptions, IFilterOption, IFilterOptions } from 'app/shared/filter';
import { TranslateDirective } from 'app/shared/language';
import { ItemCount } from 'app/shared/pagination';
import { SortByDirective, SortDirective, SortService, type SortState, sortStateSignal } from 'app/shared/sort';
import { CategoryService } from 'app/entities/category/service/category.service';
import { LocationService } from 'app/entities/location/service/location.service';
import { TreeFilter, createTreeSource } from 'app/shared/tree';
import { ListingDeleteDialog } from '../delete/listing-delete-dialog';
import { IListing } from '../listing.model';
import { ListingService } from '../service/listing.service';

@Component({
  selector: 'jhi-listing',
  templateUrl: './listing.html',
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
    FormatMediumDatetimePipe,
    Filter,
    NgbPagination,
    ItemCount,
    TreeFilter,
  ],
})
export class Listing {
  /** Đuôi slug của đơn vị hành chính mới (từ 01/7/2025). */
  static readonly NEW_UNIT_SUFFIX = '-2025';
  static readonly LOCATION_FILTER = 'locationTreeId.equals';

  private static readonly NOT_SORTABLE_FIELDS_AFTER_SEARCH = [
    'apiId',
    'name',
    'nameEn',
    'nameAlias',
    'slug',
    'description',
    'phone',
    'mobile',
    'email',
    'website',
    'address',
    'addressNew',
    'locationJson',
    'taxCode',
    'representative',
    'capital',
    'foundedYear',
    'businessType',
    'businessStatus',
    'industryCode',
    'managedBy',
    'thumbnail',
    'images',
    'status',
  ];

  readonly listings = signal<IListing[]>([]);

  sortState = sortStateSignal({});
  readonly currentSearch = signal('');
  filters: IFilterOptions = new FilterOptions();
  // Sửa tay: nguồn cây cho bộ lọc theo địa phương và ngành nghề.
  // Địa phương có hai bộ: mới (từ 01/7/2025, slug đuôi -2025: tỉnh -> xã) và cũ (tỉnh -> huyện -> xã). Mặc định bộ mới.
  readonly oldLocations = signal(false);
  readonly locationTree = computed(() => (this.oldLocations() ? this.locationTreeOld : this.locationTreeNew));
  readonly locationPlaceholders = computed(() =>
    this.oldLocations()
      ? [
          'javaSpringBootBackEndApp.location.filter.allProvinces',
          'javaSpringBootBackEndApp.location.filter.allDistricts',
          'javaSpringBootBackEndApp.location.filter.allWards',
        ]
      : ['javaSpringBootBackEndApp.location.filter.allProvinces', 'javaSpringBootBackEndApp.location.filter.allWards'],
  );
  readonly categoryTree = createTreeSource(inject(CategoryService));

  readonly itemsPerPage = signal(ITEMS_PER_PAGE);
  readonly totalItems = signal(0);
  readonly page = signal(1);

  readonly router = inject(Router);
  protected readonly listingService = inject(ListingService);
  // eslint-disable-next-line @typescript-eslint/member-ordering
  readonly isLoading = this.listingService.listingsResource.isLoading;
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
  protected readonly locationService = inject(LocationService);
  protected readonly locationTreeNew = createTreeSource(this.locationService, {
    rootSort: 'name,asc',
    rootCriteria: { 'slug.contains': Listing.NEW_UNIT_SUFFIX },
  });
  protected readonly locationTreeOld = createTreeSource(this.locationService, {
    rootSort: 'name,asc',
    rootCriteria: { 'slug.doesNotContain': Listing.NEW_UNIT_SUFFIX },
  });

  constructor() {
    effect(() => {
      const headers = this.listingService.listingsResource.headers();
      if (headers) {
        this.fillComponentAttributesFromResponseHeader(headers);
      }
    });
    effect(() => {
      this.listings.set(this.fillComponentAttributesFromResponseBody([...this.listingService.listings()]));
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

  trackId = (item: IListing): number => this.listingService.getListingIdentifier(item);

  /** Chuyển giữa địa giới mới và cũ: bỏ lựa chọn địa phương đang có (thuộc bộ kia) rồi đổi danh sách tỉnh. */
  toggleOldLocations(): void {
    const name = Listing.LOCATION_FILTER;
    const values = this.filters.filterOptions.find(option => option.name === name)?.values ?? [];
    [...values].forEach(value => this.filters.removeFilter(name, value));
    this.oldLocations.update(old => !old);
  }

  search(query: string): void {
    this.page.set(1);
    this.currentSearch.set(query);
    // Sửa tay: có từ khóa thì không gửi sort, kết quả theo độ liên quan; xóa từ khóa thì về sắp xếp mặc định
    this.navigateToWithComponentValues(query ? {} : this.getDefaultSortState());
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

  delete(listing: IListing): void {
    const modalRef = this.modalService.open(ListingDeleteDialog, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.listing = listing;
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
    // Sửa tay: đang tìm mà URL không có sort thì giữ trống (theo độ liên quan)
    this.sortState.set(this.sortService.parseSortParam(params.get(SORT) ?? (searching ? undefined : data[DEFAULT_SORT_DATA])));
    this.filters.initializeFromParams(params);
    this.detectLocationGeneration(params);
    if (searching) {
      this.currentSearch.set(params.get('search') as string);
      const { predicate } = this.sortState();
      if (predicate && Listing.NOT_SORTABLE_FIELDS_AFTER_SEARCH.includes(predicate)) {
        this.sortState.set({});
      }
    }
  }

  /** Mở link đang lọc theo một đơn vị: bật chế độ địa giới cũ nếu đơn vị đó thuộc bộ cũ. */
  protected detectLocationGeneration(params: ParamMap): void {
    const id = Number(params.get(`filter[${Listing.LOCATION_FILTER}]`));
    if (!id) {
      return;
    }
    this.locationService.find(id).subscribe(location => {
      this.oldLocations.set(!(location.slug ?? '').endsWith(Listing.NEW_UNIT_SUFFIX));
    });
  }

  protected fillComponentAttributesFromResponseBody(data: IListing[]): IListing[] {
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
    for (const filterOption of this.filters.filterOptions) {
      queryObject[filterOption.name] = filterOption.values;
    }
    this.listingService.listingsParams.set(queryObject);
  }

  protected handleNavigation(page: number, sortState: SortState, filterOptions?: IFilterOption[], currentSearch?: string): void {
    const queryParamsObj: any = {
      search: currentSearch,
      page,
      size: this.itemsPerPage(),
      sort: this.sortService.buildSortParam(sortState),
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
}
