import { Routes } from '@angular/router';

import { ASC } from 'app/config';
import { userRouteAccessService } from 'app/core/auth';

import GalleryImageResolve from './route/gallery-image-routing-resolve.service';

const galleryImageRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/gallery-image').then(m => m.GalleryImage),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/gallery-image-detail').then(m => m.GalleryImageDetail),
    resolve: {
      galleryImage: GalleryImageResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./update/gallery-image-update').then(m => m.GalleryImageUpdate),
    resolve: {
      galleryImage: GalleryImageResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/gallery-image-update').then(m => m.GalleryImageUpdate),
    resolve: {
      galleryImage: GalleryImageResolve,
    },
    canActivate: [userRouteAccessService],
  },
];

export default galleryImageRoute;
