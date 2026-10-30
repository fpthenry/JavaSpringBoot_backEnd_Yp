import { describe, expect, it } from 'vitest';

import { LocationNodeItem } from '../service/location-index.service';

import { ancestorsOf, buildLocationTree, expandableIds, kindOf, searchLocations, summarize } from './location-lookup-tree';

const item = (
  id: number,
  code: string,
  name: string,
  type: LocationNodeItem['type'],
  parentId: number | null,
  listingCount = 0,
  mapped = true,
): LocationNodeItem => ({ id, code, name, type, parentId, listingCount, mapped });

// Bộ cũ: Hà Nội > Cầu Giấy > Dịch Vọng Hậu, Quan Hoa; Hà Nội > Ba Đình > Trúc Bạch (chưa chuyển đổi)
const OLD = [
  item(2, '01', 'Thành phố Hà Nội', 'province', null, 300),
  item(30, '005', 'Quận Cầu Giấy', 'district', 2, 200),
  item(838, '00167', 'Phường Dịch Vọng Hậu', 'ward', 30, 150),
  item(839, '00169', 'Phường Quan Hoa', 'ward', 30, 50),
  item(20, '001', 'Quận Ba Đình', 'district', 2, 100),
  item(781, '00004', 'Phường Trúc Bạch', 'ward', 20, 0, false),
];

describe('location-lookup-tree', () => {
  it('dựng cây, sắp theo mã hoặc theo tên riêng', () => {
    const byCode = buildLocationTree(OLD, 'code');
    expect(byCode.roots.map(n => n.item.id)).toEqual([2]);
    expect(byCode.roots[0].children.map(n => n.item.code)).toEqual(['001', '005']);
    // Theo tên riêng (bỏ "Quận"): Ba Đình trước Cầu Giấy; Dịch Vọng Hậu trước Quan Hoa
    const byName = buildLocationTree(OLD, 'name');
    expect(byName.roots[0].children.map(n => n.item.name)).toEqual(['Quận Ba Đình', 'Quận Cầu Giấy']);
    expect(ancestorsOf(byName.byId.get(838)!).map(n => n.item.id)).toEqual([2, 30]);
  });

  it('nhận loại đơn vị theo tiền tố tên', () => {
    expect(kindOf('Thành phố Thủ Đức')).toBe('Thành phố');
    expect(kindOf('Đặc khu Phú Quốc')).toBe('Đặc khu');
    expect(kindOf('Thị trấn Đông Anh')).toBe('Thị trấn');
    expect(kindOf('QUỐC TẾ')).toBeNull();
  });

  it('tìm theo tên không dấu, khớp đầu từ, mở sẵn các cấp cha', () => {
    const tree = buildLocationTree(OLD, 'code');
    const found = searchLocations(tree, 'dich vong', 'all')!;
    expect([...found.matches]).toEqual([838]);
    expect([...found.expand].sort()).toEqual([2, 30]);
    // "ich" không phải đầu từ nào
    expect(searchLocations(tree, 'ich', 'all')!.matches.size).toBe(0);
    expect(searchLocations(tree, '  ', 'all')).toBeNull();
  });

  it('gõ mã: theo đầu mã, chỉ mở tới nút khớp trên cùng', () => {
    const tree = buildLocationTree(OLD, 'code');
    const found = searchLocations(tree, '00', 'all')!;
    expect(found.matches.has(838)).toBe(true);
    expect(found.matches.has(30)).toBe(true);
    const exact = searchLocations(tree, '00167', 'all')!;
    expect([...exact.matches]).toEqual([838]);
  });

  it('lọc nơi có doanh nghiệp và đơn vị chưa chuyển đổi', () => {
    const tree = buildLocationTree(OLD, 'code');
    const listings = searchLocations(tree, '', 'listings')!;
    expect(listings.keep.has(781)).toBe(false);
    expect(listings.keep.has(839)).toBe(true);
    const unmapped = searchLocations(tree, '', 'unmapped')!;
    expect([...unmapped.matches]).toEqual([781]);
    expect(unmapped.keep.has(838)).toBe(false);
    expect(expandableIds(tree, unmapped)).toEqual(new Set([2, 20]));
    expect(summarize(tree)).toEqual({
      kinds: new Map([
        ['Thành phố', 1],
        ['Quận', 2],
        ['Phường', 3],
      ]),
      unmapped: 1,
      withListings: 5,
    });
  });
});
