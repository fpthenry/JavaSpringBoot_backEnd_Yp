import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, Router } from '@angular/router';

import { EMPTY, Observable, catchError, of } from 'rxjs';

import { IListing } from '../listing.model';
import { ListingService } from '../service/listing.service';

const listingResolve = (route: ActivatedRouteSnapshot): Observable<null | IListing> => {
  const { id } = route.params;
  if (id) {
    const router = inject(Router);
    const service = inject(ListingService);
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

export default listingResolve;
