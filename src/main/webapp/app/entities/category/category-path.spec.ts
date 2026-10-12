import { describe, expect, it, vi } from 'vitest';

import { firstValueFrom, of, throwError } from 'rxjs';

import { loadCategoryPath } from './category-path';
import { ICategory } from './category.model';
import { CategoryService } from './service/category.service';

const service = (tree: Record<number, ICategory>): CategoryService =>
  ({ find: vi.fn((id: number) => (id in tree ? of(tree[id]) : throwError(() => new Error('404')))) }) as unknown as CategoryService;

describe('loadCategoryPath', () => {
  it('returns root → start, including start', async () => {
    const tree: Record<number, ICategory> = {
      1: { id: 1, name: 'A', parent: null },
      2: { id: 2, name: 'B', parent: { id: 1 } },
      3: { id: 3, name: 'C', parent: { id: 2 } },
    };
    expect(await firstValueFrom(loadCategoryPath(service(tree), { id: 3 }))).toEqual([
      { id: 1, name: 'A' },
      { id: 2, name: 'B' },
      { id: 3, name: 'C' },
    ]);
    expect(await firstValueFrom(loadCategoryPath(service(tree), null))).toEqual([]);
  });

  it('stops on a cycle in broken data and returns [] on API errors', async () => {
    const loop: Record<number, ICategory> = {
      8: { id: 8, name: 'X', parent: { id: 9 } },
      9: { id: 9, name: 'Y', parent: { id: 8 } },
    };
    const path = await firstValueFrom(loadCategoryPath(service(loop), { id: 8 }));
    expect(path.length).toBeLessThanOrEqual(3);
    expect(await firstValueFrom(loadCategoryPath(service({}), { id: 77 }))).toEqual([]);
  });
});
