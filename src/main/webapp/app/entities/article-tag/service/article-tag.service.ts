import { HttpClient, HttpResponse, httpResource } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';

import { Observable, asapScheduler, catchError, scheduled } from 'rxjs';

import { serverApiUrl } from 'app/config';
import { Search, createRequestOption } from 'app/core/request';
import { IArticleTag, NewArticleTag } from '../article-tag.model';

export type PartialUpdateArticleTag = Partial<IArticleTag> & Pick<IArticleTag, 'id'>;

@Service()
export class ArticleTagsService {
  readonly articleTagsParams = signal<Record<string, string | number | boolean | readonly (string | number | boolean)[]> | undefined>(
    undefined,
  );
  readonly articleTagsResource = httpResource<IArticleTag[]>(() => {
    const params = this.articleTagsParams();
    if (!params) {
      return undefined;
    }
    return { url: params.query ? this.resourceSearchUrl : this.resourceUrl, params };
  });
  /**
   * This signal holds the list of articleTag that have been fetched. It is updated when the articleTagsResource emits a new value.
   * In case of error while fetching the articleTags, the signal is set to an empty array.
   */
  readonly articleTags = computed(() => (this.articleTagsResource.hasValue() ? this.articleTagsResource.value() : []));
  protected readonly resourceUrl = `${serverApiUrl}api/article-tags`;
  protected readonly resourceSearchUrl = `${serverApiUrl}api/article-tags/_search`;
}

@Service()
export class ArticleTagService extends ArticleTagsService {
  protected readonly http = inject(HttpClient);

  create(articleTag: NewArticleTag): Observable<IArticleTag> {
    return this.http.post<IArticleTag>(this.resourceUrl, articleTag);
  }

  update(articleTag: IArticleTag): Observable<IArticleTag> {
    return this.http.put<IArticleTag>(`${this.resourceUrl}/${encodeURIComponent(this.getArticleTagIdentifier(articleTag))}`, articleTag);
  }

  partialUpdate(articleTag: PartialUpdateArticleTag): Observable<IArticleTag> {
    return this.http.patch<IArticleTag>(`${this.resourceUrl}/${encodeURIComponent(this.getArticleTagIdentifier(articleTag))}`, articleTag);
  }

  find(id: number): Observable<IArticleTag> {
    return this.http.get<IArticleTag>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  query(req?: any): Observable<HttpResponse<IArticleTag[]>> {
    const options = createRequestOption(req);
    return this.http.get<IArticleTag[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<undefined> {
    return this.http.delete<undefined>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  search(req: Search): Observable<IArticleTag[]> {
    const options = createRequestOption(req);
    return this.http.get<IArticleTag[]>(this.resourceSearchUrl, { params: options }).pipe(catchError(() => scheduled([], asapScheduler)));
  }

  getArticleTagIdentifier(articleTag: Pick<IArticleTag, 'id'>): number {
    return articleTag.id;
  }

  compareArticleTag(o1: Pick<IArticleTag, 'id'> | null, o2: Pick<IArticleTag, 'id'> | null): boolean {
    return o1 && o2 ? this.getArticleTagIdentifier(o1) === this.getArticleTagIdentifier(o2) : o1 === o2;
  }

  addArticleTagToCollectionIfMissing<Type extends Pick<IArticleTag, 'id'>>(
    articleTagCollection: Type[],
    ...articleTagsToCheck: (Type | null | undefined)[]
  ): Type[] {
    const articleTags: Type[] = articleTagsToCheck.filter(articleTagItem => articleTagItem !== null && articleTagItem !== undefined);
    if (articleTags.length > 0) {
      const articleTagCollectionIdentifiers = articleTagCollection.map(articleTagItem => this.getArticleTagIdentifier(articleTagItem));
      const articleTagsToAdd = articleTags.filter(articleTagItem => {
        const articleTagIdentifier = this.getArticleTagIdentifier(articleTagItem);
        if (articleTagCollectionIdentifiers.includes(articleTagIdentifier)) {
          return false;
        }
        articleTagCollectionIdentifiers.push(articleTagIdentifier);
        return true;
      });
      return [...articleTagsToAdd, ...articleTagCollection];
    }
    return articleTagCollection;
  }
}
