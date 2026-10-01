import { Routes } from '@angular/router';

import { ASC } from 'app/config';
import { userRouteAccessService } from 'app/core/auth';

import BlogPostResolve from './route/blog-post-routing-resolve.service';

const blogPostRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/blog-post').then(m => m.BlogPost),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/blog-post-detail').then(m => m.BlogPostDetail),
    resolve: {
      blogPost: BlogPostResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./update/blog-post-update').then(m => m.BlogPostUpdate),
    resolve: {
      blogPost: BlogPostResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/blog-post-update').then(m => m.BlogPostUpdate),
    resolve: {
      blogPost: BlogPostResolve,
    },
    canActivate: [userRouteAccessService],
  },
];

export default blogPostRoute;
