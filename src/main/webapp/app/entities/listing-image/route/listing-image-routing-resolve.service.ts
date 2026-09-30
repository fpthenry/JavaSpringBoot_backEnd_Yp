import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, Router } from '@angular/router';

import { EMPTY, Observable, catchError, of } from 'rxjs';

import { IListingImage } from '../listing-image.model';
import { ListingImageService } from '../service/listing-image.service';

const listingImageResolve = (route: ActivatedRouteSnapshot): Observable<null | IListingImage> => {
  const { id } = route.params;
  if (id) {
    const router = inject(Router);
    const service = inject(ListingImageService);
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

export default listingImageResolve;
