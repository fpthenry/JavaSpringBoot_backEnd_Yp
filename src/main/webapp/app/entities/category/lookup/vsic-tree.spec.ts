import { describe, expect, it } from 'vitest';

import { CategoryIndexItem } from '../service/category-index.service';

import { buildVsicTree, expandableIds, foldText, searchVsic } from './vsic-tree';

const item = (id: number, code: string | null, name: string, parentId: number | null): CategoryIndexItem => ({
  id,
  code,
  level: code === null ? null : /^[A-Z]$/.test(code) ? 1 : code.length,
  name,
  slug: null,
  letter: 'B',
  parentId,
  parentName: null,
  listingCount: 0,
});

const ITEMS = [
  item(1, 'G', 'BÁN BUÔN VÀ BÁN LẺ', null),
  item(2, '46', 'Bán buôn (trừ ô tô)', 1),
  item(3, '463', 'Bán buôn lương thực, thực phẩm', 2),
  item(4, '4632', 'Bán buôn thực phẩm', 3),
  item(5, '46321', 'Bán buôn thịt và các sản phẩm từ thịt', 4),
  item(6, '46322', 'Bán buôn thủy sản', 4),
  item(7, '47', 'Bán lẻ (trừ ô tô)', 1),
  item(8, null, 'Ngành cũ chưa có mã', 999),
];

const sorted = (ids: Set<number>): number[] => [...ids].sort((a, b) => a - b);

describe('vsic-tree', () => {
  it('bỏ dấu, chữ thường, gộp khoảng trắng', () => {
    expect(foldText('  Bán buôn  THỊT, Đồ uống ')).toBe('ban buon thit, do uong');
  });

  it('dựng cây theo id cha, giữ thứ tự; cha không có thì thành gốc; vòng lặp không treo', () => {
    const tree = buildVsicTree(ITEMS);
    expect(tree.roots.map(node => node.item.id)).toEqual([1, 8]);
    expect(tree.byId.get(4)!.children.map(node => node.item.code)).toEqual(['46321', '46322']);
    expect(tree.withoutCode).toBe(1);

    const loop = buildVsicTree([item(1, '01', 'A', 2), item(2, '011', 'B', 1)]);
    expect(loop.roots.map(node => node.item.id)).toEqual([2]);
    expect(loop.byId.get(2)!.children.map(node => node.item.id)).toEqual([1]);
  });

  it('trống thì không lọc', () => {
    expect(searchVsic(buildVsicTree(ITEMS), '  ')).toBeNull();
  });

  it('tìm theo đầu mã: mở tới ngành khớp trên cùng, khớp nguyên mã thì mở luôn ngành đó', () => {
    const tree = buildVsicTree(ITEMS);
    const found = searchVsic(tree, '463')!;
    expect(sorted(found.matches)).toEqual([3, 4, 5, 6]);
    expect(sorted(found.keep)).toEqual([1, 2, 3, 4, 5, 6]);
    expect(sorted(found.expand)).toEqual([1, 2, 3]);

    const section = searchVsic(tree, 'g')!;
    expect(sorted(section.matches)).toEqual([1]);
    expect(sorted(section.keep)).toEqual([1, 2, 3, 4, 5, 6, 7]);
    expect(sorted(section.expand)).toEqual([1]);
  });

  it('tìm theo tên không dấu, đủ mọi từ; giữ cả con cháu ngành khớp để mở xem', () => {
    const tree = buildVsicTree(ITEMS);
    const found = searchVsic(tree, 'ban buon THUC pham')!;
    expect(sorted(found.matches)).toEqual([3, 4]);
    expect(sorted(found.keep)).toEqual([1, 2, 3, 4, 5, 6]);
    expect(sorted(found.expand)).toEqual([1, 2, 3]);
    expect(searchVsic(tree, 'không có')!.matches.size).toBe(0);
  });

  it('Mở hết: mọi ngành có con, đang tìm thì chỉ ngành còn hiện', () => {
    const tree = buildVsicTree(ITEMS);
    expect(sorted(expandableIds(tree, null))).toEqual([1, 2, 3, 4]);
    expect(sorted(expandableIds(tree, searchVsic(tree, '47')))).toEqual([1]);
  });
});
