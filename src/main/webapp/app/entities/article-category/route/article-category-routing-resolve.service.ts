import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, Router } from '@angular/router';

import { EMPTY, Observable, catchError, of } from 'rxjs';

import { IArticleCategory } from '../article-category.model';
import { ArticleCategoryService } from '../service/article-category.service';

const articleCategoryResolve = (route: ActivatedRouteSnapshot): Observable<null | IArticleCategory> => {
  const { id } = route.params;
  if (id) {
    const router = inject(Router);
    const service = inject(ArticleCategoryService);
    return service.find(id).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 404) {
          router.navigate(['404']);
        } else {
          router.navigate(['error']);
        }
        return EMPTY;
      }),
    );
  }

  return of(null);
};

export default articleCategoryResolve;
