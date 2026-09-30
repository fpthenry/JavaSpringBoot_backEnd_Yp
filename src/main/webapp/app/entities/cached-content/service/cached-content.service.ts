import { HttpClient, HttpResponse, httpResource } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';

import dayjs from 'dayjs/esm';
import { Observable, asapScheduler, catchError, map, scheduled } from 'rxjs';

import { serverApiUrl } from 'app/config';
import { Search, createRequestOption } from 'app/core/request';
import { ICachedContent, NewCachedContent } from '../cached-content.model';

export type PartialUpdateCachedContent = Partial<ICachedContent> & Pick<ICachedContent, 'id'>;

type RestOf<T extends ICachedContent | NewCachedContent> = Omit<T, 'expired'> & {
  expired?: string | null;
};

export type RestCachedContent = RestOf<ICachedContent>;

export type NewRestCachedContent = RestOf<NewCachedContent>;

export type PartialUpdateRestCachedContent = RestOf<PartialUpdateCachedContent>;

@Service()
export class CachedContentsService {
  readonly cachedContentsParams = signal<Record<string, string | number | boolean | readonly (string | number | boolean)[]> | undefined>(
    undefined,
  );
  readonly cachedContentsResource = httpResource<RestCachedContent[]>(() => {
    const params = this.cachedContentsParams();
    if (!params) {
      return undefined;
    }
    return { url: params.query ? this.resourceSearchUrl : this.resourceUrl, params };
  });
  /**
   * This signal holds the list of cachedContent that have been fetched. It is updated when the cachedContentsResource emits a new value.
   * In case of error while fetching the cachedContents, the signal is set to an empty array.
   */
  readonly cachedContents = computed(() =>
    (this.cachedContentsResource.hasValue() ? this.cachedContentsResource.value() : []).map(item => this.convertValueFromServer(item)),
  );
  protected readonly resourceUrl = `${serverApiUrl}api/cached-contents`;
  protected readonly resourceSearchUrl = `${serverApiUrl}api/cached-contents/_search`;

  protected convertValueFromServer(restCachedContent: RestCachedContent): ICachedContent {
    return {
      ...restCachedContent,
      expired: restCachedContent.expired ? dayjs(restCachedContent.expired) : undefined,
    };
  }
}

@Service()
export class CachedContentService extends CachedContentsService {
  protected readonly http = inject(HttpClient);

  create(cachedContent: NewCachedContent): Observable<ICachedContent> {
    const copy = this.convertValueFromClient(cachedContent);
    return this.http.post<RestCachedContent>(this.resourceUrl, copy).pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(cachedContent: ICachedContent): Observable<ICachedContent> {
    const copy = this.convertValueFromClient(cachedContent);
    return this.http
      .put<RestCachedContent>(`${this.resourceUrl}/${encodeURIComponent(this.getCachedContentIdentifier(cachedContent))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(cachedContent: PartialUpdateCachedContent): Observable<ICachedContent> {
    const copy = this.convertValueFromClient(cachedContent);
    return this.http
      .patch<RestCachedContent>(`${this.resourceUrl}/${encodeURIComponent(this.getCachedContentIdentifier(cachedContent))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<ICachedContent> {
    return this.http
      .get<RestCachedContent>(`${this.resourceUrl}/${encodeURIComponent(id)}`)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<HttpResponse<ICachedContent[]>> {
    const options = createRequestOption(req);
    return this.http
      .get<RestCachedContent[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => res.clone({ body: this.convertResponseArrayFromServer(res.body!) })));
  }

  delete(id: number): Observable<undefined> {
    return this.http.delete<undefined>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  search(req: Search): Observable<ICachedContent[]> {
    const options = createRequestOption(req);
    return this.http.get<RestCachedContent[]>(this.resourceSearchUrl, { params: options }).pipe(
      map(res => this.convertResponseArrayFromServer(res)),
      catchError(() => scheduled([], asapScheduler)),
    );
  }

  getCachedContentIdentifier(cachedContent: Pick<ICachedContent, 'id'>): number {
    return cachedContent.id;
  }

  compareCachedContent(o1: Pick<ICachedContent, 'id'> | null, o2: Pick<ICachedContent, 'id'> | null): boolean {
    return o1 && o2 ? this.getCachedContentIdentifier(o1) === this.getCachedContentIdentifier(o2) : o1 === o2;
  }

  addCachedContentToCollectionIfMissing<Type extends Pick<ICachedContent, 'id'>>(
    cachedContentCollection: Type[],
    ...cachedContentsToCheck: (Type | null | undefined)[]
  ): Type[] {
    const cachedContents: Type[] = cachedContentsToCheck.filter(
      cachedContentItem => cachedContentItem !== null && cachedContentItem !== undefined,
    );
    if (cachedContents.length > 0) {
      const cachedContentCollectionIdentifiers = cachedContentCollection.map(cachedContentItem =>
        this.getCachedContentIdentifier(cachedContentItem),
      );
      const cachedContentsToAdd = cachedContents.filter(cachedContentItem => {
        const cachedContentIdentifier = this.getCachedContentIdentifier(cachedContentItem);
        if (cachedContentCollectionIdentifiers.includes(cachedContentIdentifier)) {
          return false;
        }
        cachedContentCollectionIdentifiers.push(cachedContentIdentifier);
        return true;
      });
      return [...cachedContentsToAdd, ...cachedContentCollection];
    }
    return cachedContentCollection;
  }

  protected convertValueFromClient<T extends ICachedContent | NewCachedContent | PartialUpdateCachedContent>(cachedContent: T): RestOf<T> {
    return {
      ...cachedContent,
      expired: cachedContent.expired?.toJSON() ?? null,
    };
  }

  protected convertResponseFromServer(res: RestCachedContent): ICachedContent {
    return this.convertValueFromServer(res);
  }

  protected convertResponseArrayFromServer(res: RestCachedContent[]): ICachedContent[] {
    return res.map(item => this.convertValueFromServer(item));
  }
}
