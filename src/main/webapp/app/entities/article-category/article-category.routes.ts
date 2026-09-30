import { Routes } from '@angular/router';

import { ASC } from 'app/config';
import { userRouteAccessService } from 'app/core/auth';

import ArticleCategoryResolve from './route/article-category-routing-resolve.service';

const articleCategoryRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/article-category').then(m => m.ArticleCategory),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/article-category-detail').then(m => m.ArticleCategoryDetail),
    resolve: {
      articleCategory: ArticleCategoryResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./update/article-category-update').then(m => m.ArticleCategoryUpdate),
    resolve: {
      articleCategory: ArticleCategoryResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/article-category-update').then(m => m.ArticleCategoryUpdate),
    resolve: {
      articleCategory: ArticleCategoryResolve,
    },
    canActivate: [userRouteAccessService],
  },
];

export default articleCategoryRoute;
