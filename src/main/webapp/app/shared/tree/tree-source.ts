import { HttpResponse } from '@angular/common/http';

import { Observable, map } from 'rxjs';

/** Một nút cây: entity có quan hệ ManyToOne `parent` tới chính nó (Location, Category...). */
export interface TreeItem {
  id: number;
  /** Mã hiện trước tên (mã ngành VSIC); entity không có mã thì bỏ trống. */
  code?: string | null;
  name?: string | null;
  parent?: Pick<TreeItem, 'id'> | null;
}

/** Nguồn dữ liệu cho cây tải dần từng cấp. */
export interface TreeSource<T extends TreeItem = TreeItem> {
  roots(): Observable<T[]>;
  children(parentId: number): Observable<T[]>;
  find(id: number): Observable<T>;
}

interface TreeEntityService<T> {
  query(req?: Record<string, unknown>): Observable<HttpResponse<T[]>>;
  find(id: number): Observable<T>;
}

/** Một cấp có tối đa vài trăm nút con, tải một lần là đủ. */
const CHILDREN_PAGE_SIZE = 1000;

/**
 * Tạo nguồn cây từ service entity JHipster (dùng filter `parentId` có sẵn của API).
 * `rootCriteria`: điều kiện thêm cho cấp gốc, ví dụ chỉ lấy tỉnh mới `{ 'slug.contains': '-2025' }`.
 */
export const createTreeSource = <T extends TreeItem>(
  service: TreeEntityService<T>,
  {
    rootSort = 'id,asc',
    childSort = 'name,asc',
    rootCriteria = {},
  }: { rootSort?: string; childSort?: string; rootCriteria?: Record<string, unknown> } = {},
): TreeSource<T> => {
  const list = (criteria: Record<string, unknown>): Observable<T[]> =>
    service.query({ ...criteria, page: 0, size: CHILDREN_PAGE_SIZE }).pipe(map(res => res.body ?? []));
  return {
    roots: () => list({ ...rootCriteria, 'parentId.specified': false, sort: [rootSort] }),
    children: parentId => list({ 'parentId.equals': parentId, sort: [childSort] }),
    find: id => service.find(id),
  };
};
