import { Routes } from '@angular/router';

import { ASC } from 'app/config';
import { userRouteAccessService } from 'app/core/auth';
import { canEditAuthorities } from 'app/shared/auth/permissions';
import { unsavedChangesGuard } from 'app/shared/form';

import ListingResolve from './route/listing-routing-resolve.service';

const listingRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/listing').then(m => m.Listing),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/listing-detail').then(m => m.ListingDetail),
    resolve: {
      listing: ListingResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: 'new',
    data: { authorities: canEditAuthorities('listing') },
    loadComponent: () => import('./update/listing-update').then(m => m.ListingUpdate),
    resolve: {
      listing: ListingResolve,
    },
    canActivate: [userRouteAccessService],
    canDeactivate: [unsavedChangesGuard],
  },
  {
    path: ':id/edit',
    data: { authorities: canEditAuthorities('listing') },
    loadComponent: () => import('./update/listing-update').then(m => m.ListingUpdate),
    resolve: {
      listing: ListingResolve,
    },
    canActivate: [userRouteAccessService],
    canDeactivate: [unsavedChangesGuard],
  },
];

export default listingRoute;
