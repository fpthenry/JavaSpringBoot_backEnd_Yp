import { Routes } from '@angular/router';

import { userRouteAccessService } from 'app/core/auth';

/** Sửa tay: nhập doanh nghiệp từ nguồn ngoài — chỉ ROLE_ADMIN (REST /api/admin/listing-imports cũng chỉ cho ADMIN). */
const listingImportRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/listing-import-list').then(m => m.ListingImportList),
    data: { authorities: ['ROLE_ADMIN'] },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id',
    loadComponent: () => import('./detail/listing-import-detail').then(m => m.ListingImportDetail),
    data: { authorities: ['ROLE_ADMIN'] },
    canActivate: [userRouteAccessService],
  },
];

export default listingImportRoute;
