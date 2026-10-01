import { Routes } from '@angular/router';

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
    title: 'javaSpringBootBackEndApp.listing.home.title',
    loadChildren: () => import('./listing/listing.routes'),
  },
  {
    path: 'category',
    title: 'javaSpringBootBackEndApp.category.home.title',
    loadChildren: () => import('./category/category.routes'),
  },
  {
    path: 'location',
    title: 'javaSpringBootBackEndApp.location.home.title',
    loadChildren: () => import('./location/location.routes'),
  },
  {
    path: 'blog-post',
    title: 'javaSpringBootBackEndApp.blogPost.home.title',
    loadChildren: () => import('./blog-post/blog-post.routes'),
  },
  {
    path: 'tag',
    title: 'javaSpringBootBackEndApp.tag.home.title',
    loadChildren: () => import('./tag/tag.routes'),
  },
  // jhipster-needle-add-entity-route - JHipster will add entity modules routes here
];

export default routes;
