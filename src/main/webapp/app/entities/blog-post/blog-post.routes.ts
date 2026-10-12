import { Routes } from '@angular/router';

import { DESC } from 'app/config';
import { userRouteAccessService } from 'app/core/auth';

import BlogPostResolve from './route/blog-post-routing-resolve.service';

const blogPostRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/blog-post').then(m => m.BlogPost),
    data: {
      // Sửa tay: bài mới đăng lên đầu
      defaultSort: `publishedAt,${DESC}`,
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
    // Sửa tay: trang soạn bài kiểu WordPress thay cho form JHipster
    loadComponent: () => import('./editor/blog-post-editor').then(m => m.BlogPostEditor),
    resolve: {
      blogPost: BlogPostResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/edit',
    // Sửa tay: trang soạn bài kiểu WordPress thay cho form JHipster
    loadComponent: () => import('./editor/blog-post-editor').then(m => m.BlogPostEditor),
    resolve: {
      blogPost: BlogPostResolve,
    },
    canActivate: [userRouteAccessService],
  },
];

export default blogPostRoute;
