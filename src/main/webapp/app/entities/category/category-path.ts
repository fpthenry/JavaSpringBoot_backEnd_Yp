import { EMPTY, Observable, catchError, expand, map, of, toArray } from 'rxjs';

import { ICategory } from './category.model';
import { CategoryService } from './service/category.service';

/** Một nút trên đường dẫn cây ngành. */
export interface CategoryRef {
  id: number;
  name: string;
}

/** Cây ngành tối đa 4 cấp (VSIC); chặn thêm để dữ liệu lỗi có vòng lặp không gọi API mãi. */
const MAX_DEPTH = 8;

/**
 * Đường dẫn từ ngành gốc tới {@code start} (gồm cả start): đọc lần lượt ngành cha bằng /api/categories/{id}.
 * Dùng cho dòng "Ngành gốc › … › Ngành cha" và để chặn chọn ngành con làm ngành cha (tạo vòng lặp).
 */
export function loadCategoryPath(service: CategoryService, start: Pick<ICategory, 'id'> | null | undefined): Observable<CategoryRef[]> {
  if (!start) {
    return of([]);
  }
  const seen = new Set<number>();
  return service.find(start.id).pipe(
    expand((category: ICategory, index) => {
      const parent = category.parent;
      if (!parent || index >= MAX_DEPTH || seen.has(category.id)) {
        return EMPTY;
      }
      seen.add(category.id);
      return service.find(parent.id);
    }),
    toArray(),
    map(chain => chain.map(category => ({ id: category.id, name: category.name ?? '' })).reverse()),
    catchError(() => of([])),
  );
}
