import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, Router } from '@angular/router';

import { EMPTY, Observable, catchError, of } from 'rxjs';

import { IArticle } from '../article.model';
import { ArticleService } from '../service/article.service';

const articleResolve = (route: ActivatedRouteSnapshot): Observable<null | IArticle> => {
  const { id } = route.params;
  if (id) {
    const router = inject(Router);
    const service = inject(ArticleService);
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

export default articleResolve;
