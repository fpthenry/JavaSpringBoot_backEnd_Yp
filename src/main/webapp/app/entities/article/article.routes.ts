import { Routes } from '@angular/router';

import { ASC } from 'app/config';
import { userRouteAccessService } from 'app/core/auth';

import ArticleResolve from './route/article-routing-resolve.service';

const articleRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/article').then(m => m.Article),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/article-detail').then(m => m.ArticleDetail),
    resolve: {
      article: ArticleResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./update/article-update').then(m => m.ArticleUpdate),
    resolve: {
      article: ArticleResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/article-update').then(m => m.ArticleUpdate),
    resolve: {
      article: ArticleResolve,
    },
    canActivate: [userRouteAccessService],
  },
];

export default articleRoute;
