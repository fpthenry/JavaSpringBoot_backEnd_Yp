import { Component, effect, inject, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Data, ParamMap, Router, RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap/modal';
import { TranslatePipe } from '@ngx-translate/core';
import { combineLatest, filter, map, tap } from 'rxjs';

import { DEFAULT_SORT_DATA, ITEM_DELETED_EVENT, SORT } from 'app/config';
import { DataUtils } from 'app/core/util';
import { Alert, AlertError } from 'app/shared/alert';
import { FormatMediumDatetimePipe } from 'app/shared/date';
import { TranslateDirective } from 'app/shared/language';
import { SortByDirective, SortDirective, SortService, type SortState, sortStateSignal } from 'app/shared/sort';
import { ICachedContent } from '../cached-content.model';
import { CachedContentDeleteDialog } from '../delete/cached-content-delete-dialog';
import { CachedContentService } from '../service/cached-content.service';

@Component({
  selector: 'jhi-cached-content',
  templateUrl: './cached-content.html',
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
  ],
})
export class CachedContent {
  private static readonly NOT_SORTABLE_FIELDS_AFTER_SEARCH = ['termSlug', 'content'];

  readonly cachedContents = signal<ICachedContent[]>([]);

  sortState = sortStateSignal({});
  readonly currentSearch = signal('');

  readonly router = inject(Router);
  protected readonly cachedContentService = inject(CachedContentService);
  // eslint-disable-next-line @typescript-eslint/member-ordering
  readonly isLoading = this.cachedContentService.cachedContentsResource.isLoading;
  protected readonly activatedRoute = inject(ActivatedRoute);
  protected readonly activatedRouteState = toSignal(
    combineLatest([this.activatedRoute.queryParamMap, this.activatedRoute.data]).pipe(
      map(([queryParamMap, data]) => ({ queryParamMap, data })),
    ),
    { initialValue: { queryParamMap: this.activatedRoute.snapshot.queryParamMap, data: this.activatedRoute.snapshot.data } },
  );
  protected readonly sortService = inject(SortService);
  protected dataUtils = inject(DataUtils);
  protected modalService = inject(NgbModal);

  constructor() {
    effect(() => {
      this.cachedContents.set(this.fillComponentAttributesFromResponseBody([...this.cachedContentService.cachedContents()]));
    });
    effect(() => {
      const activatedRouteState = this.activatedRouteState();
      untracked(() => {
        // Only watch for route changes. Other signals should be ignored.
        this.fillComponentAttributeFromRoute(activatedRouteState.queryParamMap, activatedRouteState.data);
        this.load();
      });
    });
  }

  trackId = (item: ICachedContent): number => this.cachedContentService.getCachedContentIdentifier(item);

  search(query: string): void {
    this.currentSearch.set(query);
    const { predicate } = this.sortState();
    if (query && predicate && CachedContent.NOT_SORTABLE_FIELDS_AFTER_SEARCH.includes(predicate)) {
      this.navigateToWithComponentValues(this.getDefaultSortState());
      return;
    }
    this.navigateToWithComponentValues(this.sortState());
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

  delete(cachedContent: ICachedContent): void {
    const modalRef = this.modalService.open(CachedContentDeleteDialog, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.cachedContent = cachedContent;
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
    this.handleNavigation(event, this.currentSearch());
  }

  protected fillComponentAttributeFromRoute(params: ParamMap, data: Data): void {
    this.sortState.set(this.sortService.parseSortParam(params.get(SORT) ?? data[DEFAULT_SORT_DATA]));
    if (params.has('search') && params.get('search') !== '') {
      this.currentSearch.set(params.get('search') as string);
      const { predicate } = this.sortState();
      if (predicate && CachedContent.NOT_SORTABLE_FIELDS_AFTER_SEARCH.includes(predicate)) {
        this.sortState.set({});
      }
    }
  }

  protected refineData(data: ICachedContent[]): ICachedContent[] {
    const { predicate, order } = this.sortState();
    return predicate && order ? data.sort(this.sortService.startSort({ predicate, order })) : data;
  }

  protected fillComponentAttributesFromResponseBody(data: ICachedContent[]): ICachedContent[] {
    return this.refineData(data);
  }

  protected queryBackend(): void {
    const queryObject: any = {
      query: this.currentSearch(),
      sort: this.sortService.buildSortParam(this.sortState()),
    };
    this.cachedContentService.cachedContentsParams.set(queryObject);
  }

  protected handleNavigation(sortState: SortState, currentSearch?: string): void {
    const queryParamsObj = {
      search: currentSearch,
      sort: this.sortService.buildSortParam(sortState),
    };

    this.router.navigate(['./'], {
      relativeTo: this.activatedRoute,
      queryParams: queryParamsObj,
    });
  }
}
