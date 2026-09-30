import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, Router } from '@angular/router';

import { EMPTY, Observable, catchError, of } from 'rxjs';

import { IRedirectRule } from '../redirect-rule.model';
import { RedirectRuleService } from '../service/redirect-rule.service';

const redirectRuleResolve = (route: ActivatedRouteSnapshot): Observable<null | IRedirectRule> => {
  const { id } = route.params;
  if (id) {
    const router = inject(Router);
    const service = inject(RedirectRuleService);
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

export default redirectRuleResolve;
