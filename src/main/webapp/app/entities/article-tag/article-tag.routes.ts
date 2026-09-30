import { Routes } from '@angular/router';

import { ASC } from 'app/config';
import { userRouteAccessService } from 'app/core/auth';

import ArticleTagResolve from './route/article-tag-routing-resolve.service';

const articleTagRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/article-tag').then(m => m.ArticleTag),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/article-tag-detail').then(m => m.ArticleTagDetail),
    resolve: {
      articleTag: ArticleTagResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./update/article-tag-update').then(m => m.ArticleTagUpdate),
    resolve: {
      articleTag: ArticleTagResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/article-tag-update').then(m => m.ArticleTagUpdate),
    resolve: {
      articleTag: ArticleTagResolve,
    },
    canActivate: [userRouteAccessService],
  },
];

export default articleTagRoute;
