import { Routes } from '@angular/router';

import { userRouteAccessService } from 'app/core/auth';
import { canViewAuthorities } from 'app/shared/auth/permissions';

const routes: Routes = [
  {
    path: 'user-management',
    title: 'userManagement.home.title',
    loadChildren: () => import('./admin/user-management/user-management.routes'),
  },
  {
    path: 'authority',
    title: 'javaSpringBootBackEndApp.adminAuthority.home.title',
    loadChildren: () => import('./admin/authority/authority.routes'),
  },
  {
    path: 'listing',
    // Sửa tay: quyền theo mục; trang con kế thừa, trang tạo/sửa đòi quyền quản lý (xem listing.routes.ts)
    data: { authorities: canViewAuthorities('listing') },
    title: 'javaSpringBootBackEndApp.listing.home.title',
    loadChildren: () => import('./listing/listing.routes'),
  },
  {
    path: 'category',
    // Sửa tay: quyền theo mục; trang con kế thừa, trang tạo/sửa đòi quyền quản lý (xem category.routes.ts)
    data: { authorities: canViewAuthorities('catalog') },
    title: 'javaSpringBootBackEndApp.category.home.title',
    loadChildren: () => import('./category/category.routes'),
  },
  {
    path: 'location',
    // Sửa tay: quyền theo mục; trang con kế thừa, trang tạo/sửa đòi quyền quản lý (xem location.routes.ts)
    data: { authorities: canViewAuthorities('catalog') },
    title: 'javaSpringBootBackEndApp.location.home.title',
    loadChildren: () => import('./location/location.routes'),
  },
  {
    path: 'blog-post',
    // Sửa tay: quyền theo mục; trang con kế thừa, trang tạo/sửa đòi quyền quản lý (xem blog-post.routes.ts)
    data: { authorities: canViewAuthorities('post') },
    title: 'javaSpringBootBackEndApp.blogPost.home.title',
    loadChildren: () => import('./blog-post/blog-post.routes'),
  },
  {
    path: 'tag',
    // Sửa tay: quyền theo mục; trang con kế thừa, trang tạo/sửa đòi quyền quản lý (xem tag.routes.ts)
    data: { authorities: canViewAuthorities('post') },
    title: 'javaSpringBootBackEndApp.tag.home.title',
    loadChildren: () => import('./tag/tag.routes'),
  },
  {
    path: 'gallery',
    // Sửa tay: quyền theo mục; trang con kế thừa, trang tạo/sửa đòi quyền quản lý (xem gallery.routes.ts)
    data: { authorities: canViewAuthorities('banner') },
    title: 'javaSpringBootBackEndApp.gallery.home.title',
    loadChildren: () => import('./gallery/gallery.routes'),
  },
  {
    path: 'gallery-image',
    // Sửa tay: quyền theo mục; trang con kế thừa, trang tạo/sửa đòi quyền quản lý (xem gallery-image.routes.ts)
    data: { authorities: canViewAuthorities('banner') },
    title: 'javaSpringBootBackEndApp.galleryImage.home.title',
    loadChildren: () => import('./gallery-image/gallery-image.routes'),
  },
  {
    path: 'blog-category',
    // Sửa tay: quyền theo mục; trang con kế thừa, trang tạo/sửa đòi quyền quản lý (xem blog-category.routes.ts)
    data: { authorities: canViewAuthorities('post') },
    title: 'javaSpringBootBackEndApp.blogCategory.home.title',
    loadChildren: () => import('./blog-category/blog-category.routes'),
  },
  {
    // Sửa tay: nhập doanh nghiệp từ file Excel/CSV, API ngoài (không qua JDL)
    path: 'listing-import',
    title: 'javaSpringBootBackEndApp.listingImport.home.title',
    loadChildren: () => import('./listing-import/listing-import.routes'),
  },
  {
    // Sửa tay (2026-11-17): bật/tắt hiệu ứng Tết hoa đào trên website (đi cùng quyền Banner)
    path: 'site-effects',
    title: 'siteEffect.title',
    data: { authorities: canViewAuthorities('banner') },
    canActivate: [userRouteAccessService],
    loadComponent: () => import('./site-effect/site-effect').then(m => m.SiteEffect),
  },
  {
    // Sửa tay (2026-11-17): ghim doanh nghiệp lên đầu kết quả tìm kiếm theo từ khóa (đi cùng quyền Doanh nghiệp)
    path: 'search-pins',
    title: 'searchPin.title',
    data: { authorities: canViewAuthorities('listing') },
    canActivate: [userRouteAccessService],
    loadComponent: () => import('./search-pin/search-pin').then(m => m.SearchPin),
  },
  // jhipster-needle-add-entity-route - JHipster will add entity modules routes here
];

export default routes;
