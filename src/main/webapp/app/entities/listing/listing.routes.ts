import { Routes } from '@angular/router';

import { ASC } from 'app/config';
import { userRouteAccessService } from 'app/core/auth';

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
    loadComponent: () => import('./update/listing-update').then(m => m.ListingUpdate),
    resolve: {
      listing: ListingResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/listing-update').then(m => m.ListingUpdate),
    resolve: {
      listing: ListingResolve,
    },
    canActivate: [userRouteAccessService],
  },
];

export default listingRoute;
