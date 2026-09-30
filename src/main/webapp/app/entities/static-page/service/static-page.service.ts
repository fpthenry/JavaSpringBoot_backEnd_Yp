import { HttpClient, HttpResponse, httpResource } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';

import dayjs from 'dayjs/esm';
import { Observable, asapScheduler, catchError, map, scheduled } from 'rxjs';

import { serverApiUrl } from 'app/config';
import { Search, createRequestOption } from 'app/core/request';
import { IStaticPage, NewStaticPage } from '../static-page.model';

export type PartialUpdateStaticPage = Partial<IStaticPage> & Pick<IStaticPage, 'id'>;

type RestOf<T extends IStaticPage | NewStaticPage> = Omit<T, 'createdAt' | 'updatedAt'> & {
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type RestStaticPage = RestOf<IStaticPage>;

export type NewRestStaticPage = RestOf<NewStaticPage>;

export type PartialUpdateRestStaticPage = RestOf<PartialUpdateStaticPage>;

@Service()
export class StaticPagesService {
  readonly staticPagesParams = signal<Record<string, string | number | boolean | readonly (string | number | boolean)[]> | undefined>(
    undefined,
  );
  readonly staticPagesResource = httpResource<RestStaticPage[]>(() => {
    const params = this.staticPagesParams();
    if (!params) {
      return undefined;
    }
    return { url: params.query ? this.resourceSearchUrl : this.resourceUrl, params };
  });
  /**
   * This signal holds the list of staticPage that have been fetched. It is updated when the staticPagesResource emits a new value.
   * In case of error while fetching the staticPages, the signal is set to an empty array.
   */
  readonly staticPages = computed(() =>
    (this.staticPagesResource.hasValue() ? this.staticPagesResource.value() : []).map(item => this.convertValueFromServer(item)),
  );
  protected readonly resourceUrl = `${serverApiUrl}api/static-pages`;
  protected readonly resourceSearchUrl = `${serverApiUrl}api/static-pages/_search`;

  protected convertValueFromServer(restStaticPage: RestStaticPage): IStaticPage {
    return {
      ...restStaticPage,
      createdAt: restStaticPage.createdAt ? dayjs(restStaticPage.createdAt) : undefined,
      updatedAt: restStaticPage.updatedAt ? dayjs(restStaticPage.updatedAt) : undefined,
    };
  }
}

@Service()
export class StaticPageService extends StaticPagesService {
  protected readonly http = inject(HttpClient);

  create(staticPage: NewStaticPage): Observable<IStaticPage> {
    const copy = this.convertValueFromClient(staticPage);
    return this.http.post<RestStaticPage>(this.resourceUrl, copy).pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(staticPage: IStaticPage): Observable<IStaticPage> {
    const copy = this.convertValueFromClient(staticPage);
    return this.http
      .put<RestStaticPage>(`${this.resourceUrl}/${encodeURIComponent(this.getStaticPageIdentifier(staticPage))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(staticPage: PartialUpdateStaticPage): Observable<IStaticPage> {
    const copy = this.convertValueFromClient(staticPage);
    return this.http
      .patch<RestStaticPage>(`${this.resourceUrl}/${encodeURIComponent(this.getStaticPageIdentifier(staticPage))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<IStaticPage> {
    return this.http
      .get<RestStaticPage>(`${this.resourceUrl}/${encodeURIComponent(id)}`)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<HttpResponse<IStaticPage[]>> {
    const options = createRequestOption(req);
    return this.http
      .get<RestStaticPage[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => res.clone({ body: this.convertResponseArrayFromServer(res.body!) })));
  }

  delete(id: number): Observable<undefined> {
    return this.http.delete<undefined>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  search(req: Search): Observable<IStaticPage[]> {
    const options = createRequestOption(req);
    return this.http.get<RestStaticPage[]>(this.resourceSearchUrl, { params: options }).pipe(
      map(res => this.convertResponseArrayFromServer(res)),
      catchError(() => scheduled([], asapScheduler)),
    );
  }

  getStaticPageIdentifier(staticPage: Pick<IStaticPage, 'id'>): number {
    return staticPage.id;
  }

  compareStaticPage(o1: Pick<IStaticPage, 'id'> | null, o2: Pick<IStaticPage, 'id'> | null): boolean {
    return o1 && o2 ? this.getStaticPageIdentifier(o1) === this.getStaticPageIdentifier(o2) : o1 === o2;
  }

  addStaticPageToCollectionIfMissing<Type extends Pick<IStaticPage, 'id'>>(
    staticPageCollection: Type[],
    ...staticPagesToCheck: (Type | null | undefined)[]
  ): Type[] {
    const staticPages: Type[] = staticPagesToCheck.filter(staticPageItem => staticPageItem !== null && staticPageItem !== undefined);
    if (staticPages.length > 0) {
      const staticPageCollectionIdentifiers = staticPageCollection.map(staticPageItem => this.getStaticPageIdentifier(staticPageItem));
      const staticPagesToAdd = staticPages.filter(staticPageItem => {
        const staticPageIdentifier = this.getStaticPageIdentifier(staticPageItem);
        if (staticPageCollectionIdentifiers.includes(staticPageIdentifier)) {
          return false;
        }
        staticPageCollectionIdentifiers.push(staticPageIdentifier);
        return true;
      });
      return [...staticPagesToAdd, ...staticPageCollection];
    }
    return staticPageCollection;
  }

  protected convertValueFromClient<T extends IStaticPage | NewStaticPage | PartialUpdateStaticPage>(staticPage: T): RestOf<T> {
    return {
      ...staticPage,
      createdAt: staticPage.createdAt?.toJSON() ?? null,
      updatedAt: staticPage.updatedAt?.toJSON() ?? null,
    };
  }

  protected convertResponseFromServer(res: RestStaticPage): IStaticPage {
    return this.convertValueFromServer(res);
  }

  protected convertResponseArrayFromServer(res: RestStaticPage[]): IStaticPage[] {
    return res.map(item => this.convertValueFromServer(item));
  }
}
