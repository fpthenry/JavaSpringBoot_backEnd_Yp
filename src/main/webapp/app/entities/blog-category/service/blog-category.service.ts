import { HttpClient, HttpResponse, httpResource } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';

import { Observable, asapScheduler, catchError, scheduled } from 'rxjs';

import { serverApiUrl } from 'app/config';
import { SearchWithPagination, createRequestOption } from 'app/core/request';
import { IBlogCategory, NewBlogCategory } from '../blog-category.model';

export type PartialUpdateBlogCategory = Partial<IBlogCategory> & Pick<IBlogCategory, 'id'>;

@Service()
export class BlogCategoriesService {
  readonly blogCategoriesParams = signal<Record<string, string | number | boolean | readonly (string | number | boolean)[]> | undefined>(
    undefined,
  );
  readonly blogCategoriesResource = httpResource<IBlogCategory[]>(() => {
    const params = this.blogCategoriesParams();
    if (!params) {
      return undefined;
    }
    return { url: params.query ? this.resourceSearchUrl : this.resourceUrl, params };
  });
  /**
   * This signal holds the list of blogCategory that have been fetched. It is updated when the blogCategoriesResource emits a new value.
   * In case of error while fetching the blogCategories, the signal is set to an empty array.
   */
  readonly blogCategories = computed(() => (this.blogCategoriesResource.hasValue() ? this.blogCategoriesResource.value() : []));
  protected readonly resourceUrl = `${serverApiUrl}api/blog-categories`;
  protected readonly resourceSearchUrl = `${serverApiUrl}api/blog-categories/_search`;
}

@Service()
export class BlogCategoryService extends BlogCategoriesService {
  protected readonly http = inject(HttpClient);

  create(blogCategory: NewBlogCategory): Observable<IBlogCategory> {
    return this.http.post<IBlogCategory>(this.resourceUrl, blogCategory);
  }

  update(blogCategory: IBlogCategory): Observable<IBlogCategory> {
    return this.http.put<IBlogCategory>(
      `${this.resourceUrl}/${encodeURIComponent(this.getBlogCategoryIdentifier(blogCategory))}`,
      blogCategory,
    );
  }

  partialUpdate(blogCategory: PartialUpdateBlogCategory): Observable<IBlogCategory> {
    return this.http.patch<IBlogCategory>(
      `${this.resourceUrl}/${encodeURIComponent(this.getBlogCategoryIdentifier(blogCategory))}`,
      blogCategory,
    );
  }

  find(id: number): Observable<IBlogCategory> {
    return this.http.get<IBlogCategory>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  query(req?: any): Observable<HttpResponse<IBlogCategory[]>> {
    const options = createRequestOption(req);
    return this.http.get<IBlogCategory[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<undefined> {
    return this.http.delete<undefined>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  search(req: SearchWithPagination): Observable<IBlogCategory[]> {
    const options = createRequestOption(req);
    return this.http.get<IBlogCategory[]>(this.resourceSearchUrl, { params: options }).pipe(catchError(() => scheduled([], asapScheduler)));
  }

  getBlogCategoryIdentifier(blogCategory: Pick<IBlogCategory, 'id'>): number {
    return blogCategory.id;
  }

  compareBlogCategory(o1: Pick<IBlogCategory, 'id'> | null, o2: Pick<IBlogCategory, 'id'> | null): boolean {
    return o1 && o2 ? this.getBlogCategoryIdentifier(o1) === this.getBlogCategoryIdentifier(o2) : o1 === o2;
  }

  addBlogCategoryToCollectionIfMissing<Type extends Pick<IBlogCategory, 'id'>>(
    blogCategoryCollection: Type[],
    ...blogCategoriesToCheck: (Type | null | undefined)[]
  ): Type[] {
    const blogCategories: Type[] = blogCategoriesToCheck.filter(
      blogCategoryItem => blogCategoryItem !== null && blogCategoryItem !== undefined,
    );
    if (blogCategories.length > 0) {
      const blogCategoryCollectionIdentifiers = blogCategoryCollection.map(blogCategoryItem =>
        this.getBlogCategoryIdentifier(blogCategoryItem),
      );
      const blogCategoriesToAdd = blogCategories.filter(blogCategoryItem => {
        const blogCategoryIdentifier = this.getBlogCategoryIdentifier(blogCategoryItem);
        if (blogCategoryCollectionIdentifiers.includes(blogCategoryIdentifier)) {
          return false;
        }
        blogCategoryCollectionIdentifiers.push(blogCategoryIdentifier);
        return true;
      });
      return [...blogCategoriesToAdd, ...blogCategoryCollection];
    }
    return blogCategoryCollection;
  }
}
