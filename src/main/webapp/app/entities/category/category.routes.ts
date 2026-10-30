import { Routes } from '@angular/router';

import { ASC } from 'app/config';
import { userRouteAccessService } from 'app/core/auth';
import { unsavedChangesGuard } from 'app/shared/form';

import CategoryResolve from './route/category-routing-resolve.service';

const categoryRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/category').then(m => m.Category),
    data: {
      // Sửa tay: xếp theo mã ngành VSIC (A, 01, 011, 0111, 01110...)
      defaultSort: `code,${ASC}`,
    },
    canActivate: [userRouteAccessService],
  },
  {
    // Sửa tay: trang tra cứu mã ngành VSIC dạng cây (giữ đường dẫn /category/tree cho menu, link cũ)
    path: 'tree',
    loadComponent: () => import('./lookup/category-lookup').then(m => m.CategoryLookup),
    canActivate: [userRouteAccessService],
  },
  {
    path: 'index',
    loadComponent: () => import('./index/category-index').then(m => m.CategoryIndex),
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/category-detail').then(m => m.CategoryDetail),
    resolve: {
      category: CategoryResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./update/category-update').then(m => m.CategoryUpdate),
    resolve: {
      category: CategoryResolve,
    },
    canActivate: [userRouteAccessService],
    canDeactivate: [unsavedChangesGuard],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/category-update').then(m => m.CategoryUpdate),
    resolve: {
      category: CategoryResolve,
    },
    canActivate: [userRouteAccessService],
    canDeactivate: [unsavedChangesGuard],
  },
];

export default categoryRoute;
