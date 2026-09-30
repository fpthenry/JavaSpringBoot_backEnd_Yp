import { Routes } from '@angular/router';

import { ASC } from 'app/config';
import { userRouteAccessService } from 'app/core/auth';

import ListingImageResolve from './route/listing-image-routing-resolve.service';

const listingImageRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/listing-image').then(m => m.ListingImage),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/listing-image-detail').then(m => m.ListingImageDetail),
    resolve: {
      listingImage: ListingImageResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./update/listing-image-update').then(m => m.ListingImageUpdate),
    resolve: {
      listingImage: ListingImageResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/listing-image-update').then(m => m.ListingImageUpdate),
    resolve: {
      listingImage: ListingImageResolve,
    },
    canActivate: [userRouteAccessService],
  },
];

export default listingImageRoute;
