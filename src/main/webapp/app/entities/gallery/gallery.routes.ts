import { Routes } from '@angular/router';

import { ASC } from 'app/config';
import { userRouteAccessService } from 'app/core/auth';
import { canEditAuthorities } from 'app/shared/auth/permissions';

import GalleryResolve from './route/gallery-routing-resolve.service';

const galleryRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/gallery').then(m => m.Gallery),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/gallery-detail').then(m => m.GalleryDetail),
    resolve: {
      gallery: GalleryResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: 'new',
    data: { authorities: canEditAuthorities('banner') },
    loadComponent: () => import('./update/gallery-update').then(m => m.GalleryUpdate),
    resolve: {
      gallery: GalleryResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/edit',
    data: { authorities: canEditAuthorities('banner') },
    loadComponent: () => import('./update/gallery-update').then(m => m.GalleryUpdate),
    resolve: {
      gallery: GalleryResolve,
    },
    canActivate: [userRouteAccessService],
  },
];

export default galleryRoute;
