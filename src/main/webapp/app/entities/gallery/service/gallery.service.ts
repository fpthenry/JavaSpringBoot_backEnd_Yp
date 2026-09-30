import { HttpClient, HttpResponse, httpResource } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';

import dayjs from 'dayjs/esm';
import { Observable, asapScheduler, catchError, map, scheduled } from 'rxjs';

import { serverApiUrl } from 'app/config';
import { SearchWithPagination, createRequestOption } from 'app/core/request';
import { IGallery, NewGallery } from '../gallery.model';

export type PartialUpdateGallery = Partial<IGallery> & Pick<IGallery, 'id'>;

type RestOf<T extends IGallery | NewGallery> = Omit<T, 'createdAt' | 'updatedAt'> & {
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type RestGallery = RestOf<IGallery>;

export type NewRestGallery = RestOf<NewGallery>;

export type PartialUpdateRestGallery = RestOf<PartialUpdateGallery>;

@Service()
export class GalleriesService {
  readonly galleriesParams = signal<Record<string, string | number | boolean | readonly (string | number | boolean)[]> | undefined>(
    undefined,
  );
  readonly galleriesResource = httpResource<RestGallery[]>(() => {
    const params = this.galleriesParams();
    if (!params) {
      return undefined;
    }
    return { url: params.query ? this.resourceSearchUrl : this.resourceUrl, params };
  });
  /**
   * This signal holds the list of gallery that have been fetched. It is updated when the galleriesResource emits a new value.
   * In case of error while fetching the galleries, the signal is set to an empty array.
   */
  readonly galleries = computed(() =>
    (this.galleriesResource.hasValue() ? this.galleriesResource.value() : []).map(item => this.convertValueFromServer(item)),
  );
  protected readonly resourceUrl = `${serverApiUrl}api/galleries`;
  protected readonly resourceSearchUrl = `${serverApiUrl}api/galleries/_search`;

  protected convertValueFromServer(restGallery: RestGallery): IGallery {
    return {
      ...restGallery,
      createdAt: restGallery.createdAt ? dayjs(restGallery.createdAt) : undefined,
      updatedAt: restGallery.updatedAt ? dayjs(restGallery.updatedAt) : undefined,
    };
  }
}

@Service()
export class GalleryService extends GalleriesService {
  protected readonly http = inject(HttpClient);

  create(gallery: NewGallery): Observable<IGallery> {
    const copy = this.convertValueFromClient(gallery);
    return this.http.post<RestGallery>(this.resourceUrl, copy).pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(gallery: IGallery): Observable<IGallery> {
    const copy = this.convertValueFromClient(gallery);
    return this.http
      .put<RestGallery>(`${this.resourceUrl}/${encodeURIComponent(this.getGalleryIdentifier(gallery))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(gallery: PartialUpdateGallery): Observable<IGallery> {
    const copy = this.convertValueFromClient(gallery);
    return this.http
      .patch<RestGallery>(`${this.resourceUrl}/${encodeURIComponent(this.getGalleryIdentifier(gallery))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<IGallery> {
    return this.http
      .get<RestGallery>(`${this.resourceUrl}/${encodeURIComponent(id)}`)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<HttpResponse<IGallery[]>> {
    const options = createRequestOption(req);
    return this.http
      .get<RestGallery[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => res.clone({ body: this.convertResponseArrayFromServer(res.body!) })));
  }

  delete(id: number): Observable<undefined> {
    return this.http.delete<undefined>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  search(req: SearchWithPagination): Observable<IGallery[]> {
    const options = createRequestOption(req);
    return this.http.get<RestGallery[]>(this.resourceSearchUrl, { params: options }).pipe(
      map(res => this.convertResponseArrayFromServer(res)),
      catchError(() => scheduled([], asapScheduler)),
    );
  }

  getGalleryIdentifier(gallery: Pick<IGallery, 'id'>): number {
    return gallery.id;
  }

  compareGallery(o1: Pick<IGallery, 'id'> | null, o2: Pick<IGallery, 'id'> | null): boolean {
    return o1 && o2 ? this.getGalleryIdentifier(o1) === this.getGalleryIdentifier(o2) : o1 === o2;
  }

  addGalleryToCollectionIfMissing<Type extends Pick<IGallery, 'id'>>(
    galleryCollection: Type[],
    ...galleriesToCheck: (Type | null | undefined)[]
  ): Type[] {
    const galleries: Type[] = galleriesToCheck.filter(galleryItem => galleryItem !== null && galleryItem !== undefined);
    if (galleries.length > 0) {
      const galleryCollectionIdentifiers = galleryCollection.map(galleryItem => this.getGalleryIdentifier(galleryItem));
      const galleriesToAdd = galleries.filter(galleryItem => {
        const galleryIdentifier = this.getGalleryIdentifier(galleryItem);
        if (galleryCollectionIdentifiers.includes(galleryIdentifier)) {
          return false;
        }
        galleryCollectionIdentifiers.push(galleryIdentifier);
        return true;
      });
      return [...galleriesToAdd, ...galleryCollection];
    }
    return galleryCollection;
  }

  protected convertValueFromClient<T extends IGallery | NewGallery | PartialUpdateGallery>(gallery: T): RestOf<T> {
    return {
      ...gallery,
      createdAt: gallery.createdAt?.toJSON() ?? null,
      updatedAt: gallery.updatedAt?.toJSON() ?? null,
    };
  }

  protected convertResponseFromServer(res: RestGallery): IGallery {
    return this.convertValueFromServer(res);
  }

  protected convertResponseArrayFromServer(res: RestGallery[]): IGallery[] {
    return res.map(item => this.convertValueFromServer(item));
  }
}
