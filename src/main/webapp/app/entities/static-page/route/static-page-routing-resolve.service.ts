import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, Router } from '@angular/router';

import { EMPTY, Observable, catchError, of } from 'rxjs';

import { StaticPageService } from '../service/static-page.service';
import { IStaticPage } from '../static-page.model';

const staticPageResolve = (route: ActivatedRouteSnapshot): Observable<null | IStaticPage> => {
  const { id } = route.params;
  if (id) {
    const router = inject(Router);
    const service = inject(StaticPageService);
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

export default staticPageResolve;
