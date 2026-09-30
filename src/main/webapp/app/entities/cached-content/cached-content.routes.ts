import { Routes } from '@angular/router';

import { ASC } from 'app/config';
import { userRouteAccessService } from 'app/core/auth';

import CachedContentResolve from './route/cached-content-routing-resolve.service';

const cachedContentRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/cached-content').then(m => m.CachedContent),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/cached-content-detail').then(m => m.CachedContentDetail),
    resolve: {
      cachedContent: CachedContentResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./update/cached-content-update').then(m => m.CachedContentUpdate),
    resolve: {
      cachedContent: CachedContentResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/cached-content-update').then(m => m.CachedContentUpdate),
    resolve: {
      cachedContent: CachedContentResolve,
    },
    canActivate: [userRouteAccessService],
  },
];

export default cachedContentRoute;
