import { HttpClient, HttpResponse, httpResource } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';

import dayjs from 'dayjs/esm';
import { Observable, asapScheduler, catchError, map, scheduled } from 'rxjs';

import { serverApiUrl } from 'app/config';
import { SearchWithPagination, createRequestOption } from 'app/core/request';
import { IListingImage, NewListingImage } from '../listing-image.model';

export type PartialUpdateListingImage = Partial<IListingImage> & Pick<IListingImage, 'id'>;

type RestOf<T extends IListingImage | NewListingImage> = Omit<T, 'createdAt' | 'updatedAt'> & {
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type RestListingImage = RestOf<IListingImage>;

export type NewRestListingImage = RestOf<NewListingImage>;

export type PartialUpdateRestListingImage = RestOf<PartialUpdateListingImage>;

@Service()
export class ListingImagesService {
  readonly listingImagesParams = signal<Record<string, string | number | boolean | readonly (string | number | boolean)[]> | undefined>(
    undefined,
  );
  readonly listingImagesResource = httpResource<RestListingImage[]>(() => {
    const params = this.listingImagesParams();
    if (!params) {
      return undefined;
    }
    return { url: params.query ? this.resourceSearchUrl : this.resourceUrl, params };
  });
  /**
   * This signal holds the list of listingImage that have been fetched. It is updated when the listingImagesResource emits a new value.
   * In case of error while fetching the listingImages, the signal is set to an empty array.
   */
  readonly listingImages = computed(() =>
    (this.listingImagesResource.hasValue() ? this.listingImagesResource.value() : []).map(item => this.convertValueFromServer(item)),
  );
  protected readonly resourceUrl = `${serverApiUrl}api/listing-images`;
  protected readonly resourceSearchUrl = `${serverApiUrl}api/listing-images/_search`;

  protected convertValueFromServer(restListingImage: RestListingImage): IListingImage {
    return {
      ...restListingImage,
      createdAt: restListingImage.createdAt ? dayjs(restListingImage.createdAt) : undefined,
      updatedAt: restListingImage.updatedAt ? dayjs(restListingImage.updatedAt) : undefined,
    };
  }
}

@Service()
export class ListingImageService extends ListingImagesService {
  protected readonly http = inject(HttpClient);

  create(listingImage: NewListingImage): Observable<IListingImage> {
    const copy = this.convertValueFromClient(listingImage);
    return this.http.post<RestListingImage>(this.resourceUrl, copy).pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(listingImage: IListingImage): Observable<IListingImage> {
    const copy = this.convertValueFromClient(listingImage);
    return this.http
      .put<RestListingImage>(`${this.resourceUrl}/${encodeURIComponent(this.getListingImageIdentifier(listingImage))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(listingImage: PartialUpdateListingImage): Observable<IListingImage> {
    const copy = this.convertValueFromClient(listingImage);
    return this.http
      .patch<RestListingImage>(`${this.resourceUrl}/${encodeURIComponent(this.getListingImageIdentifier(listingImage))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<IListingImage> {
    return this.http
      .get<RestListingImage>(`${this.resourceUrl}/${encodeURIComponent(id)}`)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<HttpResponse<IListingImage[]>> {
    const options = createRequestOption(req);
    return this.http
      .get<RestListingImage[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => res.clone({ body: this.convertResponseArrayFromServer(res.body!) })));
  }

  delete(id: number): Observable<undefined> {
    return this.http.delete<undefined>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  search(req: SearchWithPagination): Observable<IListingImage[]> {
    const options = createRequestOption(req);
    return this.http.get<RestListingImage[]>(this.resourceSearchUrl, { params: options }).pipe(
      map(res => this.convertResponseArrayFromServer(res)),
      catchError(() => scheduled([], asapScheduler)),
    );
  }

  getListingImageIdentifier(listingImage: Pick<IListingImage, 'id'>): number {
    return listingImage.id;
  }

  compareListingImage(o1: Pick<IListingImage, 'id'> | null, o2: Pick<IListingImage, 'id'> | null): boolean {
    return o1 && o2 ? this.getListingImageIdentifier(o1) === this.getListingImageIdentifier(o2) : o1 === o2;
  }

  addListingImageToCollectionIfMissing<Type extends Pick<IListingImage, 'id'>>(
    listingImageCollection: Type[],
    ...listingImagesToCheck: (Type | null | undefined)[]
  ): Type[] {
    const listingImages: Type[] = listingImagesToCheck.filter(
      listingImageItem => listingImageItem !== null && listingImageItem !== undefined,
    );
    if (listingImages.length > 0) {
      const listingImageCollectionIdentifiers = listingImageCollection.map(listingImageItem =>
        this.getListingImageIdentifier(listingImageItem),
      );
      const listingImagesToAdd = listingImages.filter(listingImageItem => {
        const listingImageIdentifier = this.getListingImageIdentifier(listingImageItem);
        if (listingImageCollectionIdentifiers.includes(listingImageIdentifier)) {
          return false;
        }
        listingImageCollectionIdentifiers.push(listingImageIdentifier);
        return true;
      });
      return [...listingImagesToAdd, ...listingImageCollection];
    }
    return listingImageCollection;
  }

  protected convertValueFromClient<T extends IListingImage | NewListingImage | PartialUpdateListingImage>(listingImage: T): RestOf<T> {
    return {
      ...listingImage,
      createdAt: listingImage.createdAt?.toJSON() ?? null,
      updatedAt: listingImage.updatedAt?.toJSON() ?? null,
    };
  }

  protected convertResponseFromServer(res: RestListingImage): IListingImage {
    return this.convertValueFromServer(res);
  }

  protected convertResponseArrayFromServer(res: RestListingImage[]): IListingImage[] {
    return res.map(item => this.convertValueFromServer(item));
  }
}
