import { Routes } from '@angular/router';

import { ASC } from 'app/config';
import { userRouteAccessService } from 'app/core/auth';

import StaticPageResolve from './route/static-page-routing-resolve.service';

const staticPageRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/static-page').then(m => m.StaticPage),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/static-page-detail').then(m => m.StaticPageDetail),
    resolve: {
      staticPage: StaticPageResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./update/static-page-update').then(m => m.StaticPageUpdate),
    resolve: {
      staticPage: StaticPageResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/static-page-update').then(m => m.StaticPageUpdate),
    resolve: {
      staticPage: StaticPageResolve,
    },
    canActivate: [userRouteAccessService],
  },
];

export default staticPageRoute;
