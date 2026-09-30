import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, Router } from '@angular/router';

import { EMPTY, Observable, catchError, of } from 'rxjs';

import { ICachedContent } from '../cached-content.model';
import { CachedContentService } from '../service/cached-content.service';

const cachedContentResolve = (route: ActivatedRouteSnapshot): Observable<null | ICachedContent> => {
  const { id } = route.params;
  if (id) {
    const router = inject(Router);
    const service = inject(CachedContentService);
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

export default cachedContentResolve;
