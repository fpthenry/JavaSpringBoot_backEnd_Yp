import { describe, expect, it } from 'vitest';

import { categorySlug, codeAndName, levelOfCode, parentCodeOf } from './category-code';

describe('category-code (VSIC 2025)', () => {
  it('levelOfCode: chữ cái là cấp 1, n chữ số là cấp n', () => {
    expect(levelOfCode('G')).toBe(1);
    expect(levelOfCode('g')).toBe(1);
    expect(levelOfCode('46')).toBe(2);
    expect(levelOfCode('463')).toBe(3);
    expect(levelOfCode('4632')).toBe(4);
    expect(levelOfCode('46321')).toBe(5);
    expect(levelOfCode('463210')).toBeNull();
    expect(levelOfCode('4')).toBeNull();
    expect(levelOfCode('GG')).toBeNull();
    expect(levelOfCode(null)).toBeNull();
  });

  it('parentCodeOf: cấp 3–5 bỏ chữ số cuối, cấp 1–2 không suy được', () => {
    expect(parentCodeOf('46321')).toBe('4632');
    expect(parentCodeOf('463')).toBe('46');
    expect(parentCodeOf('46')).toBeNull();
    expect(parentCodeOf('G')).toBeNull();
    expect(parentCodeOf('abc')).toBeNull();
  });

  it('categorySlug: tên + mã, & thành "va"', () => {
    expect(categorySlug('Bán buôn thực phẩm', '4632')).toBe('ban-buon-thuc-pham-4632');
    expect(categorySlug('Bán buôn và bán lẻ; sửa chữa ô tô, mô tô, xe máy', 'G')).toBe('ban-buon-va-ban-le-sua-chua-o-to-mo-to-xe-may-g');
    expect(categorySlug('Điện &amp; nước', '35')).toBe('dien-va-nuoc-35');
    expect(categorySlug('Đường', null)).toBe('duong');
  });

  it('codeAndName', () => {
    expect(codeAndName({ code: '4632', name: 'Bán buôn thực phẩm' })).toBe('4632 · Bán buôn thực phẩm');
    expect(codeAndName({ name: 'Ngành cũ' })).toBe('Ngành cũ');
    expect(codeAndName(null)).toBe('');
  });
});
