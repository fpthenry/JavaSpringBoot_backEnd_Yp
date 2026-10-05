import { HttpClient, HttpResponse, httpResource } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';

import { Observable } from 'rxjs';

import { serverApiUrl } from 'app/config';
import { createRequestOption } from 'app/core/request';
import { IGallery, NewGallery } from '../gallery.model';

export type PartialUpdateGallery = Partial<IGallery> & Pick<IGallery, 'id'>;

@Service()
export class GalleriesService {
  readonly galleriesParams = signal<Record<string, string | number | boolean | readonly (string | number | boolean)[]> | undefined>(
    undefined,
  );
  readonly galleriesResource = httpResource<IGallery[]>(() => {
    const params = this.galleriesParams();
    if (!params) {
      return undefined;
    }
    return { url: this.resourceUrl, params };
  });
  /**
   * This signal holds the list of gallery that have been fetched. It is updated when the galleriesResource emits a new value.
   * In case of error while fetching the galleries, the signal is set to an empty array.
   */
  readonly galleries = computed(() => (this.galleriesResource.hasValue() ? this.galleriesResource.value() : []));
  protected readonly resourceUrl = `${serverApiUrl}api/galleries`;
}

@Service()
export class GalleryService extends GalleriesService {
  protected readonly http = inject(HttpClient);

  create(gallery: NewGallery): Observable<IGallery> {
    return this.http.post<IGallery>(this.resourceUrl, gallery);
  }

  update(gallery: IGallery): Observable<IGallery> {
    return this.http.put<IGallery>(`${this.resourceUrl}/${encodeURIComponent(this.getGalleryIdentifier(gallery))}`, gallery);
  }

  partialUpdate(gallery: PartialUpdateGallery): Observable<IGallery> {
    return this.http.patch<IGallery>(`${this.resourceUrl}/${encodeURIComponent(this.getGalleryIdentifier(gallery))}`, gallery);
  }

  find(id: number): Observable<IGallery> {
    return this.http.get<IGallery>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  query(req?: any): Observable<HttpResponse<IGallery[]>> {
    const options = createRequestOption(req);
    return this.http.get<IGallery[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<undefined> {
    return this.http.delete<undefined>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
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
}
