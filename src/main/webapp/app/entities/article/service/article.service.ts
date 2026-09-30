import { HttpClient, HttpResponse, httpResource } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';

import dayjs from 'dayjs/esm';
import { Observable, asapScheduler, catchError, map, scheduled } from 'rxjs';

import { serverApiUrl } from 'app/config';
import { SearchWithPagination, createRequestOption } from 'app/core/request';
import { IArticle, NewArticle } from '../article.model';

export type PartialUpdateArticle = Partial<IArticle> & Pick<IArticle, 'id'>;

type RestOf<T extends IArticle | NewArticle> = Omit<T, 'createdAt' | 'updatedAt'> & {
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type RestArticle = RestOf<IArticle>;

export type NewRestArticle = RestOf<NewArticle>;

export type PartialUpdateRestArticle = RestOf<PartialUpdateArticle>;

@Service()
export class ArticlesService {
  readonly articlesParams = signal<Record<string, string | number | boolean | readonly (string | number | boolean)[]> | undefined>(
    undefined,
  );
  readonly articlesResource = httpResource<RestArticle[]>(() => {
    const params = this.articlesParams();
    if (!params) {
      return undefined;
    }
    return { url: params.query ? this.resourceSearchUrl : this.resourceUrl, params };
  });
  /**
   * This signal holds the list of article that have been fetched. It is updated when the articlesResource emits a new value.
   * In case of error while fetching the articles, the signal is set to an empty array.
   */
  readonly articles = computed(() =>
    (this.articlesResource.hasValue() ? this.articlesResource.value() : []).map(item => this.convertValueFromServer(item)),
  );
  protected readonly resourceUrl = `${serverApiUrl}api/articles`;
  protected readonly resourceSearchUrl = `${serverApiUrl}api/articles/_search`;

  protected convertValueFromServer(restArticle: RestArticle): IArticle {
    return {
      ...restArticle,
      createdAt: restArticle.createdAt ? dayjs(restArticle.createdAt) : undefined,
      updatedAt: restArticle.updatedAt ? dayjs(restArticle.updatedAt) : undefined,
    };
  }
}

@Service()
export class ArticleService extends ArticlesService {
  protected readonly http = inject(HttpClient);

  create(article: NewArticle): Observable<IArticle> {
    const copy = this.convertValueFromClient(article);
    return this.http.post<RestArticle>(this.resourceUrl, copy).pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(article: IArticle): Observable<IArticle> {
    const copy = this.convertValueFromClient(article);
    return this.http
      .put<RestArticle>(`${this.resourceUrl}/${encodeURIComponent(this.getArticleIdentifier(article))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(article: PartialUpdateArticle): Observable<IArticle> {
    const copy = this.convertValueFromClient(article);
    return this.http
      .patch<RestArticle>(`${this.resourceUrl}/${encodeURIComponent(this.getArticleIdentifier(article))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<IArticle> {
    return this.http
      .get<RestArticle>(`${this.resourceUrl}/${encodeURIComponent(id)}`)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<HttpResponse<IArticle[]>> {
    const options = createRequestOption(req);
    return this.http
      .get<RestArticle[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => res.clone({ body: this.convertResponseArrayFromServer(res.body!) })));
  }

  delete(id: number): Observable<undefined> {
    return this.http.delete<undefined>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  search(req: SearchWithPagination): Observable<IArticle[]> {
    const options = createRequestOption(req);
    return this.http.get<RestArticle[]>(this.resourceSearchUrl, { params: options }).pipe(
      map(res => this.convertResponseArrayFromServer(res)),
      catchError(() => scheduled([], asapScheduler)),
    );
  }

  getArticleIdentifier(article: Pick<IArticle, 'id'>): number {
    return article.id;
  }

  compareArticle(o1: Pick<IArticle, 'id'> | null, o2: Pick<IArticle, 'id'> | null): boolean {
    return o1 && o2 ? this.getArticleIdentifier(o1) === this.getArticleIdentifier(o2) : o1 === o2;
  }

  addArticleToCollectionIfMissing<Type extends Pick<IArticle, 'id'>>(
    articleCollection: Type[],
    ...articlesToCheck: (Type | null | undefined)[]
  ): Type[] {
    const articles: Type[] = articlesToCheck.filter(articleItem => articleItem !== null && articleItem !== undefined);
    if (articles.length > 0) {
      const articleCollectionIdentifiers = articleCollection.map(articleItem => this.getArticleIdentifier(articleItem));
      const articlesToAdd = articles.filter(articleItem => {
        const articleIdentifier = this.getArticleIdentifier(articleItem);
        if (articleCollectionIdentifiers.includes(articleIdentifier)) {
          return false;
        }
        articleCollectionIdentifiers.push(articleIdentifier);
        return true;
      });
      return [...articlesToAdd, ...articleCollection];
    }
    return articleCollection;
  }

  protected convertValueFromClient<T extends IArticle | NewArticle | PartialUpdateArticle>(article: T): RestOf<T> {
    return {
      ...article,
      createdAt: article.createdAt?.toJSON() ?? null,
      updatedAt: article.updatedAt?.toJSON() ?? null,
    };
  }

  protected convertResponseFromServer(res: RestArticle): IArticle {
    return this.convertValueFromServer(res);
  }

  protected convertResponseArrayFromServer(res: RestArticle[]): IArticle[] {
    return res.map(item => this.convertValueFromServer(item));
  }
}
