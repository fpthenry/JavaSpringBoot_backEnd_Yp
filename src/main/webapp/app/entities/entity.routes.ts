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
    path: 'listing-image',
    title: 'javaSpringBootBackEndApp.listingImage.home.title',
    loadChildren: () => import('./listing-image/listing-image.routes'),
  },
  {
    path: 'gallery',
    title: 'javaSpringBootBackEndApp.gallery.home.title',
    loadChildren: () => import('./gallery/gallery.routes'),
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
    path: 'article',
    title: 'javaSpringBootBackEndApp.article.home.title',
    loadChildren: () => import('./article/article.routes'),
  },
  {
    path: 'article-category',
    title: 'javaSpringBootBackEndApp.articleCategory.home.title',
    loadChildren: () => import('./article-category/article-category.routes'),
  },
  {
    path: 'article-tag',
    title: 'javaSpringBootBackEndApp.articleTag.home.title',
    loadChildren: () => import('./article-tag/article-tag.routes'),
  },
  {
    path: 'static-page',
    title: 'javaSpringBootBackEndApp.staticPage.home.title',
    loadChildren: () => import('./static-page/static-page.routes'),
  },
  {
    path: 'redirect-rule',
    title: 'javaSpringBootBackEndApp.redirectRule.home.title',
    loadChildren: () => import('./redirect-rule/redirect-rule.routes'),
  },
  {
    path: 'cached-content',
    title: 'javaSpringBootBackEndApp.cachedContent.home.title',
    loadChildren: () => import('./cached-content/cached-content.routes'),
  },
  // jhipster-needle-add-entity-route - JHipster will add entity modules routes here
];

export default routes;
