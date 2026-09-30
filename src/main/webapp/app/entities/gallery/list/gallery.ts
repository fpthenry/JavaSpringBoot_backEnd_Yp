import { HttpHeaders } from '@angular/common/http';
import { Component, WritableSignal, computed, effect, inject, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Data, ParamMap, Router, RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap/modal';
import { TranslatePipe } from '@ngx-translate/core';
import { InfiniteScrollDirective } from 'ngx-infinite-scroll';
import { combineLatest, filter, map, tap } from 'rxjs';

import { DEFAULT_SORT_DATA, ITEMS_PER_PAGE, ITEM_DELETED_EVENT, SORT } from 'app/config';
import { DataUtils, ParseLinks } from 'app/core/util';
import { Alert, AlertError } from 'app/shared/alert';
import { FormatMediumDatetimePipe } from 'app/shared/date';
import { TranslateDirective } from 'app/shared/language';
import { SortByDirective, SortDirective, SortService, type SortState, sortStateSignal } from 'app/shared/sort';
import { GalleryDeleteDialog } from '../delete/gallery-delete-dialog';
import { IGallery } from '../gallery.model';
import { GalleryService } from '../service/gallery.service';

@Component({
  selector: 'jhi-gallery',
  templateUrl: './gallery.html',
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
    InfiniteScrollDirective,
  ],
})
export class Gallery {
  private static readonly NOT_SORTABLE_FIELDS_AFTER_SEARCH = ['title', 'slug', 'content', 'status'];

  readonly galleries = signal<IGallery[]>([]);

  sortState = sortStateSignal({});
  readonly currentSearch = signal('');

  readonly itemsPerPage = signal(ITEMS_PER_PAGE);
  readonly links: WritableSignal<Record<string, undefined | Record<string, string | undefined>>> = signal({});
  readonly hasMorePage = computed(() => !!this.links().next);
  readonly isFirstFetch = computed(() => Object.keys(this.links()).length === 0);

  readonly router = inject(Router);
  protected readonly galleryService = inject(GalleryService);
  // eslint-disable-next-line @typescript-eslint/member-ordering
  readonly isLoading = this.galleryService.galleriesResource.isLoading;
  protected readonly activatedRoute = inject(ActivatedRoute);
  protected readonly activatedRouteState = toSignal(
    combineLatest([this.activatedRoute.queryParamMap, this.activatedRoute.data]).pipe(
      map(([queryParamMap, data]) => ({ queryParamMap, data })),
    ),
    { initialValue: { queryParamMap: this.activatedRoute.snapshot.queryParamMap, data: this.activatedRoute.snapshot.data } },
  );
  protected readonly sortService = inject(SortService);
  protected parseLinks = inject(ParseLinks);
  protected dataUtils = inject(DataUtils);
  protected modalService = inject(NgbModal);

  constructor() {
    effect(() => {
      const headers = this.galleryService.galleriesResource.headers();
      if (headers) {
        this.fillComponentAttributesFromResponseHeader(headers);
      }
    });
    effect(() => {
      this.galleries.update(galleries => this.fillComponentAttributesFromResponseBody([...this.galleryService.galleries()], galleries));
    });
    effect(() => {
      const activatedRouteState = this.activatedRouteState();
      untracked(() => {
        // Only watch for route changes. Other signals should be ignored.
        this.fillComponentAttributeFromRoute(activatedRouteState.queryParamMap, activatedRouteState.data);
        this.reset();
        this.load();
      });
    });
  }

  trackId = (item: IGallery): number => this.galleryService.getGalleryIdentifier(item);

  reset(): void {
    this.galleries.set([]);
    this.links.set({});
  }

  loadNextPage(): void {
    this.load();
  }

  search(query: string): void {
    this.currentSearch.set(query);
    const { predicate } = this.sortState();
    if (query && predicate && Gallery.NOT_SORTABLE_FIELDS_AFTER_SEARCH.includes(predicate)) {
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

  delete(gallery: IGallery): void {
    const modalRef = this.modalService.open(GalleryDeleteDialog, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.gallery = gallery;
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
      if (predicate && Gallery.NOT_SORTABLE_FIELDS_AFTER_SEARCH.includes(predicate)) {
        this.sortState.set({});
      }
    }
  }

  protected fillComponentAttributesFromResponseBody(data: IGallery[], currentValue: IGallery[]): IGallery[] {
    const galleriesNew = [...currentValue];
    for (const d of data) {
      if (!galleriesNew.some(op => op.id === d.id)) {
        galleriesNew.push(d);
      }
    }
    return galleriesNew;
  }

  protected fillComponentAttributesFromResponseHeader(headers: HttpHeaders): void {
    const linkHeader = headers.get('link');
    if (linkHeader) {
      this.links.set(this.parseLinks.parseAll(linkHeader));
    } else {
      this.links.set({});
    }
  }

  protected queryBackend(): void {
    const queryObject: any = {
      size: this.itemsPerPage(),
      eagerload: true,
      query: this.currentSearch(),
    };
    if (this.hasMorePage()) {
      Object.assign(queryObject, this.links().next);
    } else if (this.isFirstFetch()) {
      Object.assign(queryObject, { sort: this.sortService.buildSortParam(this.sortState()) });
    }

    this.galleryService.galleriesParams.set(queryObject);
  }

  protected handleNavigation(sortState: SortState, currentSearch?: string): void {
    this.links.set({});

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
