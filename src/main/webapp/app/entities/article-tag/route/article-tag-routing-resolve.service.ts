import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, Router } from '@angular/router';

import { EMPTY, Observable, catchError, of } from 'rxjs';

import { IArticleTag } from '../article-tag.model';
import { ArticleTagService } from '../service/article-tag.service';

const articleTagResolve = (route: ActivatedRouteSnapshot): Observable<null | IArticleTag> => {
  const { id } = route.params;
  if (id) {
    const router = inject(Router);
    const service = inject(ArticleTagService);
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

export default articleTagResolve;
