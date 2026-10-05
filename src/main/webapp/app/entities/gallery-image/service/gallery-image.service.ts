import { HttpClient, HttpResponse, httpResource } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';

import dayjs from 'dayjs/esm';
import { Observable, map } from 'rxjs';

import { serverApiUrl } from 'app/config';
import { createRequestOption } from 'app/core/request';
import { IGalleryImage, NewGalleryImage } from '../gallery-image.model';

export type PartialUpdateGalleryImage = Partial<IGalleryImage> & Pick<IGalleryImage, 'id'>;

type RestOf<T extends IGalleryImage | NewGalleryImage> = Omit<T, 'startAt' | 'endAt'> & {
  startAt?: string | null;
  endAt?: string | null;
};

export type RestGalleryImage = RestOf<IGalleryImage>;

export type NewRestGalleryImage = RestOf<NewGalleryImage>;

export type PartialUpdateRestGalleryImage = RestOf<PartialUpdateGalleryImage>;

@Service()
export class GalleryImagesService {
  readonly galleryImagesParams = signal<Record<string, string | number | boolean | readonly (string | number | boolean)[]> | undefined>(
    undefined,
  );
  readonly galleryImagesResource = httpResource<RestGalleryImage[]>(() => {
    const params = this.galleryImagesParams();
    if (!params) {
      return undefined;
    }
    return { url: this.resourceUrl, params };
  });
  /**
   * This signal holds the list of galleryImage that have been fetched. It is updated when the galleryImagesResource emits a new value.
   * In case of error while fetching the galleryImages, the signal is set to an empty array.
   */
  readonly galleryImages = computed(() =>
    (this.galleryImagesResource.hasValue() ? this.galleryImagesResource.value() : []).map(item => this.convertValueFromServer(item)),
  );
  protected readonly resourceUrl = `${serverApiUrl}api/gallery-images`;

  protected convertValueFromServer(restGalleryImage: RestGalleryImage): IGalleryImage {
    return {
      ...restGalleryImage,
      startAt: restGalleryImage.startAt ? dayjs(restGalleryImage.startAt) : undefined,
      endAt: restGalleryImage.endAt ? dayjs(restGalleryImage.endAt) : undefined,
    };
  }
}

@Service()
export class GalleryImageService extends GalleryImagesService {
  protected readonly http = inject(HttpClient);

  create(galleryImage: NewGalleryImage): Observable<IGalleryImage> {
    const copy = this.convertValueFromClient(galleryImage);
    return this.http.post<RestGalleryImage>(this.resourceUrl, copy).pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(galleryImage: IGalleryImage): Observable<IGalleryImage> {
    const copy = this.convertValueFromClient(galleryImage);
    return this.http
      .put<RestGalleryImage>(`${this.resourceUrl}/${encodeURIComponent(this.getGalleryImageIdentifier(galleryImage))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(galleryImage: PartialUpdateGalleryImage): Observable<IGalleryImage> {
    const copy = this.convertValueFromClient(galleryImage);
    return this.http
      .patch<RestGalleryImage>(`${this.resourceUrl}/${encodeURIComponent(this.getGalleryImageIdentifier(galleryImage))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<IGalleryImage> {
    return this.http
      .get<RestGalleryImage>(`${this.resourceUrl}/${encodeURIComponent(id)}`)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<HttpResponse<IGalleryImage[]>> {
    const options = createRequestOption(req);
    return this.http
      .get<RestGalleryImage[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => res.clone({ body: this.convertResponseArrayFromServer(res.body!) })));
  }

  delete(id: number): Observable<undefined> {
    return this.http.delete<undefined>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  getGalleryImageIdentifier(galleryImage: Pick<IGalleryImage, 'id'>): number {
    return galleryImage.id;
  }

  compareGalleryImage(o1: Pick<IGalleryImage, 'id'> | null, o2: Pick<IGalleryImage, 'id'> | null): boolean {
    return o1 && o2 ? this.getGalleryImageIdentifier(o1) === this.getGalleryImageIdentifier(o2) : o1 === o2;
  }

  addGalleryImageToCollectionIfMissing<Type extends Pick<IGalleryImage, 'id'>>(
    galleryImageCollection: Type[],
    ...galleryImagesToCheck: (Type | null | undefined)[]
  ): Type[] {
    const galleryImages: Type[] = galleryImagesToCheck.filter(
      galleryImageItem => galleryImageItem !== null && galleryImageItem !== undefined,
    );
    if (galleryImages.length > 0) {
      const galleryImageCollectionIdentifiers = galleryImageCollection.map(galleryImageItem =>
        this.getGalleryImageIdentifier(galleryImageItem),
      );
      const galleryImagesToAdd = galleryImages.filter(galleryImageItem => {
        const galleryImageIdentifier = this.getGalleryImageIdentifier(galleryImageItem);
        if (galleryImageCollectionIdentifiers.includes(galleryImageIdentifier)) {
          return false;
        }
        galleryImageCollectionIdentifiers.push(galleryImageIdentifier);
        return true;
      });
      return [...galleryImagesToAdd, ...galleryImageCollection];
    }
    return galleryImageCollection;
  }

  protected convertValueFromClient<T extends IGalleryImage | NewGalleryImage | PartialUpdateGalleryImage>(galleryImage: T): RestOf<T> {
    return {
      ...galleryImage,
      startAt: galleryImage.startAt?.toJSON() ?? null,
      endAt: galleryImage.endAt?.toJSON() ?? null,
    };
  }

  protected convertResponseFromServer(res: RestGalleryImage): IGalleryImage {
    return this.convertValueFromServer(res);
  }

  protected convertResponseArrayFromServer(res: RestGalleryImage[]): IGalleryImage[] {
    return res.map(item => this.convertValueFromServer(item));
  }
}
