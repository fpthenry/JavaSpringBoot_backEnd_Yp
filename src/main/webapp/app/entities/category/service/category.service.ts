import { HttpClient, HttpResponse, httpResource } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';

import dayjs from 'dayjs/esm';
import { Observable, asapScheduler, catchError, map, scheduled } from 'rxjs';

import { serverApiUrl } from 'app/config';
import { SearchWithPagination, createRequestOption } from 'app/core/request';
import { ICategory, NewCategory } from '../category.model';

export type PartialUpdateCategory = Partial<ICategory> & Pick<ICategory, 'id'>;

type RestOf<T extends ICategory | NewCategory> = Omit<T, 'createdAt' | 'updatedAt'> & {
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type RestCategory = RestOf<ICategory>;

export type NewRestCategory = RestOf<NewCategory>;

export type PartialUpdateRestCategory = RestOf<PartialUpdateCategory>;

@Service()
export class CategoriesService {
  readonly categoriesParams = signal<Record<string, string | number | boolean | readonly (string | number | boolean)[]> | undefined>(
    undefined,
  );
  readonly categoriesResource = httpResource<RestCategory[]>(() => {
    const params = this.categoriesParams();
    if (!params) {
      return undefined;
    }
    return { url: params.query ? this.resourceSearchUrl : this.resourceUrl, params };
  });
  /**
   * This signal holds the list of category that have been fetched. It is updated when the categoriesResource emits a new value.
   * In case of error while fetching the categories, the signal is set to an empty array.
   */
  readonly categories = computed(() =>
    (this.categoriesResource.hasValue() ? this.categoriesResource.value() : []).map(item => this.convertValueFromServer(item)),
  );
  protected readonly resourceUrl = `${serverApiUrl}api/categories`;
  protected readonly resourceSearchUrl = `${serverApiUrl}api/categories/_search`;

  protected convertValueFromServer(restCategory: RestCategory): ICategory {
    return {
      ...restCategory,
      createdAt: restCategory.createdAt ? dayjs(restCategory.createdAt) : undefined,
      updatedAt: restCategory.updatedAt ? dayjs(restCategory.updatedAt) : undefined,
    };
  }
}

@Service()
export class CategoryService extends CategoriesService {
  protected readonly http = inject(HttpClient);

  create(category: NewCategory): Observable<ICategory> {
    const copy = this.convertValueFromClient(category);
    return this.http.post<RestCategory>(this.resourceUrl, copy).pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(category: ICategory): Observable<ICategory> {
    const copy = this.convertValueFromClient(category);
    return this.http
      .put<RestCategory>(`${this.resourceUrl}/${encodeURIComponent(this.getCategoryIdentifier(category))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(category: PartialUpdateCategory): Observable<ICategory> {
    const copy = this.convertValueFromClient(category);
    return this.http
      .patch<RestCategory>(`${this.resourceUrl}/${encodeURIComponent(this.getCategoryIdentifier(category))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<ICategory> {
    return this.http
      .get<RestCategory>(`${this.resourceUrl}/${encodeURIComponent(id)}`)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<HttpResponse<ICategory[]>> {
    const options = createRequestOption(req);
    return this.http
      .get<RestCategory[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => res.clone({ body: this.convertResponseArrayFromServer(res.body!) })));
  }

  delete(id: number): Observable<undefined> {
    return this.http.delete<undefined>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  search(req: SearchWithPagination): Observable<ICategory[]> {
    const options = createRequestOption(req);
    return this.http.get<RestCategory[]>(this.resourceSearchUrl, { params: options }).pipe(
      map(res => this.convertResponseArrayFromServer(res)),
      catchError(() => scheduled([], asapScheduler)),
    );
  }

  getCategoryIdentifier(category: Pick<ICategory, 'id'>): number {
    return category.id;
  }

  compareCategory(o1: Pick<ICategory, 'id'> | null, o2: Pick<ICategory, 'id'> | null): boolean {
    return o1 && o2 ? this.getCategoryIdentifier(o1) === this.getCategoryIdentifier(o2) : o1 === o2;
  }

  addCategoryToCollectionIfMissing<Type extends Pick<ICategory, 'id'>>(
    categoryCollection: Type[],
    ...categoriesToCheck: (Type | null | undefined)[]
  ): Type[] {
    const categories: Type[] = categoriesToCheck.filter(categoryItem => categoryItem !== null && categoryItem !== undefined);
    if (categories.length > 0) {
      const categoryCollectionIdentifiers = categoryCollection.map(categoryItem => this.getCategoryIdentifier(categoryItem));
      const categoriesToAdd = categories.filter(categoryItem => {
        const categoryIdentifier = this.getCategoryIdentifier(categoryItem);
        if (categoryCollectionIdentifiers.includes(categoryIdentifier)) {
          return false;
        }
        categoryCollectionIdentifiers.push(categoryIdentifier);
        return true;
      });
      return [...categoriesToAdd, ...categoryCollection];
    }
    return categoryCollection;
  }

  protected convertValueFromClient<T extends ICategory | NewCategory | PartialUpdateCategory>(category: T): RestOf<T> {
    return {
      ...category,
      createdAt: category.createdAt?.toJSON() ?? null,
      updatedAt: category.updatedAt?.toJSON() ?? null,
    };
  }

  protected convertResponseFromServer(res: RestCategory): ICategory {
    return this.convertValueFromServer(res);
  }

  protected convertResponseArrayFromServer(res: RestCategory[]): ICategory[] {
    return res.map(item => this.convertValueFromServer(item));
  }
}
