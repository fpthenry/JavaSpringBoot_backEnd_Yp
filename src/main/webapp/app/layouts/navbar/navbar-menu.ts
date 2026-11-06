import { IconProp } from '@fortawesome/fontawesome-svg-core';

/** Sửa tay: một mục menu bên trái. Có `children` thì bấm để mở/đóng nhóm con, không có đường dẫn riêng. */
export interface MenuItem {
  label: string;
  icon?: IconProp;
  link?: string;
  /** Chỉ sáng khi đúng đường dẫn, không sáng ở trang con (vd. /category không sáng ở /category/tree). */
  exact?: boolean;
  adminOnly?: boolean;
  /** Chỉ hiện khi backend bật OpenAPI. */
  openApi?: boolean;
  children?: MenuItem[];
}

export interface MenuGroup {
  label?: string;
  items: MenuItem[];
}

export const MENU_GROUPS: MenuGroup[] = [
  { items: [{ label: 'global.menu.home', icon: 'home', link: '/', exact: true }] },
  {
    label: 'global.menu.groups.data',
    items: [
      { label: 'global.menu.entities.listing', icon: 'building', link: '/listing' },
      { label: 'global.menu.entities.listingImport', icon: 'file-import', link: '/listing-import', adminOnly: true },
      {
        label: 'global.menu.entities.categoryGroup',
        icon: 'briefcase',
        children: [
          { label: 'global.menu.sidebar.lookup', link: '/category/tree', exact: true },
          { label: 'global.menu.sidebar.index', link: '/category/index', exact: true },
          { label: 'global.menu.sidebar.list', link: '/category', exact: true },
        ],
      },
      {
        label: 'global.menu.entities.locationGroup',
        icon: 'map',
        children: [
          { label: 'global.menu.sidebar.lookup', link: '/location/tree', exact: true },
          { label: 'global.menu.sidebar.index', link: '/location/index', exact: true },
          { label: 'global.menu.sidebar.list', link: '/location', exact: true },
        ],
      },
    ],
  },
  {
    label: 'global.menu.groups.content',
    items: [
      { label: 'global.menu.entities.blogPost', icon: 'newspaper', link: '/blog-post' },
      { label: 'global.menu.entities.blogCategory', icon: 'folder', link: '/blog-category' },
      { label: 'global.menu.entities.tag', icon: 'tags', link: '/tag' },
      {
        label: 'global.menu.entities.galleryGroup',
        icon: 'images',
        children: [
          { label: 'global.menu.entities.gallery', link: '/gallery' },
          { label: 'global.menu.entities.galleryImage', link: '/gallery-image' },
        ],
      },
    ],
  },
  {
    label: 'global.menu.groups.system',
    items: [
      { label: 'global.menu.admin.userManagement', icon: 'users', link: '/user-management', adminOnly: true },
      { label: 'global.menu.entities.adminAuthority', icon: 'user-shield', link: '/authority', adminOnly: true },
      {
        label: 'global.menu.admin.monitoring',
        icon: 'heart-pulse',
        adminOnly: true,
        children: [
          { label: 'global.menu.admin.health', link: '/admin/health' },
          { label: 'global.menu.admin.metrics', link: '/admin/metrics' },
          { label: 'global.menu.admin.logs', link: '/admin/logs' },
          { label: 'global.menu.admin.configuration', link: '/admin/configuration' },
        ],
      },
      { label: 'global.menu.admin.apidocs', icon: 'book', link: '/admin/docs', adminOnly: true, openApi: true },
    ],
  },
];

/** Đúng trang đó hoặc trang con của nó (/category/5/edit thuộc /category; /gallery-image không thuộc /gallery). */
export function isUnder(url: string, link: string): boolean {
  return url === link || url.startsWith(`${link}/`);
}
