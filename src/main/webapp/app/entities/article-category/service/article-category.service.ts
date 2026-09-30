import { HttpClient, HttpResponse, httpResource } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';

import { Observable, asapScheduler, catchError, scheduled } from 'rxjs';

import { serverApiUrl } from 'app/config';
import { Search, createRequestOption } from 'app/core/request';
import { IArticleCategory, NewArticleCategory } from '../article-category.model';

export type PartialUpdateArticleCategory = Partial<IArticleCategory> & Pick<IArticleCategory, 'id'>;

@Service()
export class ArticleCategoriesService {
  readonly articleCategoriesParams = signal<Record<string, string | number | boolean | readonly (string | number | boolean)[]> | undefined>(
    undefined,
  );
  readonly articleCategoriesResource = httpResource<IArticleCategory[]>(() => {
    const params = this.articleCategoriesParams();
    if (!params) {
      return undefined;
    }
    return { url: params.query ? this.resourceSearchUrl : this.resourceUrl, params };
  });
  /**
   * This signal holds the list of articleCategory that have been fetched. It is updated when the articleCategoriesResource emits a new value.
   * In case of error while fetching the articleCategories, the signal is set to an empty array.
   */
  readonly articleCategories = computed(() => (this.articleCategoriesResource.hasValue() ? this.articleCategoriesResource.value() : []));
  protected readonly resourceUrl = `${serverApiUrl}api/article-categories`;
  protected readonly resourceSearchUrl = `${serverApiUrl}api/article-categories/_search`;
}

@Service()
export class ArticleCategoryService extends ArticleCategoriesService {
  protected readonly http = inject(HttpClient);

  create(articleCategory: NewArticleCategory): Observable<IArticleCategory> {
    return this.http.post<IArticleCategory>(this.resourceUrl, articleCategory);
  }

  update(articleCategory: IArticleCategory): Observable<IArticleCategory> {
    return this.http.put<IArticleCategory>(
      `${this.resourceUrl}/${encodeURIComponent(this.getArticleCategoryIdentifier(articleCategory))}`,
      articleCategory,
    );
  }

  partialUpdate(articleCategory: PartialUpdateArticleCategory): Observable<IArticleCategory> {
    return this.http.patch<IArticleCategory>(
      `${this.resourceUrl}/${encodeURIComponent(this.getArticleCategoryIdentifier(articleCategory))}`,
      articleCategory,
    );
  }

  find(id: number): Observable<IArticleCategory> {
    return this.http.get<IArticleCategory>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  query(req?: any): Observable<HttpResponse<IArticleCategory[]>> {
    const options = createRequestOption(req);
    return this.http.get<IArticleCategory[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<undefined> {
    return this.http.delete<undefined>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  search(req: Search): Observable<IArticleCategory[]> {
    const options = createRequestOption(req);
    return this.http
      .get<IArticleCategory[]>(this.resourceSearchUrl, { params: options })
      .pipe(catchError(() => scheduled([], asapScheduler)));
  }

  getArticleCategoryIdentifier(articleCategory: Pick<IArticleCategory, 'id'>): number {
    return articleCategory.id;
  }

  compareArticleCategory(o1: Pick<IArticleCategory, 'id'> | null, o2: Pick<IArticleCategory, 'id'> | null): boolean {
    return o1 && o2 ? this.getArticleCategoryIdentifier(o1) === this.getArticleCategoryIdentifier(o2) : o1 === o2;
  }

  addArticleCategoryToCollectionIfMissing<Type extends Pick<IArticleCategory, 'id'>>(
    articleCategoryCollection: Type[],
    ...articleCategoriesToCheck: (Type | null | undefined)[]
  ): Type[] {
    const articleCategories: Type[] = articleCategoriesToCheck.filter(
      articleCategoryItem => articleCategoryItem !== null && articleCategoryItem !== undefined,
    );
    if (articleCategories.length > 0) {
      const articleCategoryCollectionIdentifiers = articleCategoryCollection.map(articleCategoryItem =>
        this.getArticleCategoryIdentifier(articleCategoryItem),
      );
      const articleCategoriesToAdd = articleCategories.filter(articleCategoryItem => {
        const articleCategoryIdentifier = this.getArticleCategoryIdentifier(articleCategoryItem);
        if (articleCategoryCollectionIdentifiers.includes(articleCategoryIdentifier)) {
          return false;
        }
        articleCategoryCollectionIdentifiers.push(articleCategoryIdentifier);
        return true;
      });
      return [...articleCategoriesToAdd, ...articleCategoryCollection];
    }
    return articleCategoryCollection;
  }
}
