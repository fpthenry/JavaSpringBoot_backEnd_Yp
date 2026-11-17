import { Authority } from 'app/shared/jhipster/constants';

/**
 * Sửa tay: quyền theo từng mục của trang quản trị. Mỗi mục có 2 mức: chỉ xem (`_VIEW`) và quản lý (`_EDIT`: thêm, sửa, xóa).
 * ROLE_ADMIN luôn có toàn quyền. Phải khớp với AuthoritiesConstants và SecurityConfiguration ở backend.
 */
export type PermissionModuleKey = 'listing' | 'catalog' | 'post' | 'banner';

export type PermissionLevel = 'none' | 'view' | 'edit';

export interface PermissionModule {
  key: PermissionModuleKey;
  /** Khóa dịch tên mục. */
  label: string;
  /** Khóa dịch mô tả ngắn: mục này gồm những gì. */
  hint: string;
  view: string;
  edit: string;
}

export const PERMISSION_MODULES: PermissionModule[] = [
  {
    key: 'listing',
    label: 'userManagement.permissions.modules.listing',
    hint: 'userManagement.permissions.hints.listing',
    view: 'ROLE_LISTING_VIEW',
    edit: 'ROLE_LISTING_EDIT',
  },
  {
    key: 'catalog',
    label: 'userManagement.permissions.modules.catalog',
    hint: 'userManagement.permissions.hints.catalog',
    view: 'ROLE_CATALOG_VIEW',
    edit: 'ROLE_CATALOG_EDIT',
  },
  {
    key: 'post',
    label: 'userManagement.permissions.modules.post',
    hint: 'userManagement.permissions.hints.post',
    view: 'ROLE_POST_VIEW',
    edit: 'ROLE_POST_EDIT',
  },
  {
    key: 'banner',
    label: 'userManagement.permissions.modules.banner',
    hint: 'userManagement.permissions.hints.banner',
    view: 'ROLE_BANNER_VIEW',
    edit: 'ROLE_BANNER_EDIT',
  },
];

export const PERMISSION_LEVELS: PermissionLevel[] = ['none', 'view', 'edit'];

const moduleOf = (key: PermissionModuleKey): PermissionModule => PERMISSION_MODULES.find(module => module.key === key)!;

/** Quyền được xem mục `key` (dùng cho route, menu): admin, xem, quản lý. */
export function canViewAuthorities(key: PermissionModuleKey): string[] {
  const module = moduleOf(key);
  return [Authority.ADMIN, module.view, module.edit];
}

/** Quyền được thêm/sửa/xóa trong mục `key` (dùng cho trang tạo/sửa và các nút): admin, quản lý. */
export function canEditAuthorities(key: PermissionModuleKey): string[] {
  return [Authority.ADMIN, moduleOf(key).edit];
}

/** Mức quyền của một tài khoản với một mục (không tính admin). */
export function levelOf(authorities: readonly string[] | null | undefined, module: PermissionModule): PermissionLevel {
  if (authorities?.includes(module.edit)) {
    return 'edit';
  }
  return authorities?.includes(module.view) ? 'view' : 'none';
}

/** Ngược lại của levelOf: từ lựa chọn trên form ra danh sách quyền gửi lên server. Luôn kèm ROLE_USER. */
export function authoritiesFrom(isAdmin: boolean, levels: Partial<Record<PermissionModuleKey, PermissionLevel>>): string[] {
  const result: string[] = [Authority.USER];
  if (isAdmin) {
    result.push(Authority.ADMIN);
  }
  for (const module of PERMISSION_MODULES) {
    const level = levels[module.key] ?? 'none';
    if (level === 'view') {
      result.push(module.view);
    } else if (level === 'edit') {
      result.push(module.edit);
    }
  }
  return result;
}
