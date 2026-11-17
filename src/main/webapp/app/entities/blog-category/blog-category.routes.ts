import { Routes } from '@angular/router';

import { ASC } from 'app/config';
import { userRouteAccessService } from 'app/core/auth';
import { canEditAuthorities } from 'app/shared/auth/permissions';

import BlogCategoryResolve from './route/blog-category-routing-resolve.service';

const blogCategoryRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/blog-category').then(m => m.BlogCategory),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/blog-category-detail').then(m => m.BlogCategoryDetail),
    resolve: {
      blogCategory: BlogCategoryResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: 'new',
    data: { authorities: canEditAuthorities('post') },
    loadComponent: () => import('./update/blog-category-update').then(m => m.BlogCategoryUpdate),
    resolve: {
      blogCategory: BlogCategoryResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/edit',
    data: { authorities: canEditAuthorities('post') },
    loadComponent: () => import('./update/blog-category-update').then(m => m.BlogCategoryUpdate),
    resolve: {
      blogCategory: BlogCategoryResolve,
    },
    canActivate: [userRouteAccessService],
  },
];

export default blogCategoryRoute;
