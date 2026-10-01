import { HttpClient, HttpResponse, httpResource } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';

import dayjs from 'dayjs/esm';
import { Observable, asapScheduler, catchError, map, scheduled } from 'rxjs';

import { serverApiUrl } from 'app/config';
import { SearchWithPagination, createRequestOption } from 'app/core/request';
import { IListing, NewListing } from '../listing.model';

export type PartialUpdateListing = Partial<IListing> & Pick<IListing, 'id'>;

type RestOf<T extends IListing | NewListing> = Omit<T, 'publishedAt' | 'modifiedAt' | 'createdAt' | 'updatedAt'> & {
  publishedAt?: string | null;
  modifiedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type RestListing = RestOf<IListing>;

export type NewRestListing = RestOf<NewListing>;

export type PartialUpdateRestListing = RestOf<PartialUpdateListing>;

@Service()
export class ListingsService {
  readonly listingsParams = signal<Record<string, string | number | boolean | readonly (string | number | boolean)[]> | undefined>(
    undefined,
  );
  readonly listingsResource = httpResource<RestListing[]>(() => {
    const params = this.listingsParams();
    if (!params) {
      return undefined;
    }
    return { url: params.query ? this.resourceSearchUrl : this.resourceUrl, params };
  });
  /**
   * This signal holds the list of listing that have been fetched. It is updated when the listingsResource emits a new value.
   * In case of error while fetching the listings, the signal is set to an empty array.
   */
  readonly listings = computed(() =>
    (this.listingsResource.hasValue() ? this.listingsResource.value() : []).map(item => this.convertValueFromServer(item)),
  );
  protected readonly resourceUrl = `${serverApiUrl}api/listings`;
  protected readonly resourceSearchUrl = `${serverApiUrl}api/listings/_search`;

  protected convertValueFromServer(restListing: RestListing): IListing {
    return {
      ...restListing,
      publishedAt: restListing.publishedAt ? dayjs(restListing.publishedAt) : undefined,
      modifiedAt: restListing.modifiedAt ? dayjs(restListing.modifiedAt) : undefined,
      createdAt: restListing.createdAt ? dayjs(restListing.createdAt) : undefined,
      updatedAt: restListing.updatedAt ? dayjs(restListing.updatedAt) : undefined,
    };
  }
}

@Service()
export class ListingService extends ListingsService {
  protected readonly http = inject(HttpClient);

  create(listing: NewListing): Observable<IListing> {
    const copy = this.convertValueFromClient(listing);
    return this.http.post<RestListing>(this.resourceUrl, copy).pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(listing: IListing): Observable<IListing> {
    const copy = this.convertValueFromClient(listing);
    return this.http
      .put<RestListing>(`${this.resourceUrl}/${encodeURIComponent(this.getListingIdentifier(listing))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(listing: PartialUpdateListing): Observable<IListing> {
    const copy = this.convertValueFromClient(listing);
    return this.http
      .patch<RestListing>(`${this.resourceUrl}/${encodeURIComponent(this.getListingIdentifier(listing))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<IListing> {
    return this.http
      .get<RestListing>(`${this.resourceUrl}/${encodeURIComponent(id)}`)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<HttpResponse<IListing[]>> {
    const options = createRequestOption(req);
    return this.http
      .get<RestListing[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => res.clone({ body: this.convertResponseArrayFromServer(res.body!) })));
  }

  delete(id: number): Observable<undefined> {
    return this.http.delete<undefined>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  search(req: SearchWithPagination): Observable<IListing[]> {
    const options = createRequestOption(req);
    return this.http.get<RestListing[]>(this.resourceSearchUrl, { params: options }).pipe(
      map(res => this.convertResponseArrayFromServer(res)),
      catchError(() => scheduled([], asapScheduler)),
    );
  }

  getListingIdentifier(listing: Pick<IListing, 'id'>): number {
    return listing.id;
  }

  compareListing(o1: Pick<IListing, 'id'> | null, o2: Pick<IListing, 'id'> | null): boolean {
    return o1 && o2 ? this.getListingIdentifier(o1) === this.getListingIdentifier(o2) : o1 === o2;
  }

  addListingToCollectionIfMissing<Type extends Pick<IListing, 'id'>>(
    listingCollection: Type[],
    ...listingsToCheck: (Type | null | undefined)[]
  ): Type[] {
    const listings: Type[] = listingsToCheck.filter(listingItem => listingItem !== null && listingItem !== undefined);
    if (listings.length > 0) {
      const listingCollectionIdentifiers = listingCollection.map(listingItem => this.getListingIdentifier(listingItem));
      const listingsToAdd = listings.filter(listingItem => {
        const listingIdentifier = this.getListingIdentifier(listingItem);
        if (listingCollectionIdentifiers.includes(listingIdentifier)) {
          return false;
        }
        listingCollectionIdentifiers.push(listingIdentifier);
        return true;
      });
      return [...listingsToAdd, ...listingCollection];
    }
    return listingCollection;
  }

  protected convertValueFromClient<T extends IListing | NewListing | PartialUpdateListing>(listing: T): RestOf<T> {
    return {
      ...listing,
      publishedAt: listing.publishedAt?.toJSON() ?? null,
      modifiedAt: listing.modifiedAt?.toJSON() ?? null,
      createdAt: listing.createdAt?.toJSON() ?? null,
      updatedAt: listing.updatedAt?.toJSON() ?? null,
    };
  }

  protected convertResponseFromServer(res: RestListing): IListing {
    return this.convertValueFromServer(res);
  }

  protected convertResponseArrayFromServer(res: RestListing[]): IListing[] {
    return res.map(item => this.convertValueFromServer(item));
  }
}
