import { slugify } from 'app/shared/form';

/**
 * Mã ngành VSIC 2025 (QĐ 36/2025/QĐ-TTg): cấp 1 là một chữ cái A–V, cấp 2–5 là 2–5 chữ số
 * (46 → 463 → 4632 → 46321). Cùng quy tắc với Category.levelOf ở máy chủ.
 */
export const VSIC_CODE_PATTERN = /^([A-Z]|\d{2,5})$/;

/** Cây ngành (trang cây, bộ lọc cây ở trang doanh nghiệp): mỗi cấp xếp theo mã. */
export const VSIC_TREE_SORT = { rootSort: 'code,asc', childSort: 'code,asc' };

/** Số cấp của hệ thống ngành VSIC 2025. */
export const VSIC_LEVELS = [1, 2, 3, 4, 5] as const;

/** Cấp của mã: chữ cái → 1, n chữ số → n; sai dạng → null. */
export function levelOfCode(code: string | null | undefined): number | null {
  const value = (code ?? '').trim().toUpperCase();
  if (!VSIC_CODE_PATTERN.test(value)) {
    return null;
  }
  return /^[A-Z]$/.test(value) ? 1 : value.length;
}

/**
 * Mã ngành cha suy được từ mã: cấp 3–5 bỏ chữ số cuối (4632 → 463). Cấp 2 thuộc ngành cấp 1 (chữ cái) theo
 * bảng VSIC, không suy được từ mã; cấp 1 không có cha → null.
 */
export function parentCodeOf(code: string | null | undefined): string | null {
  const level = levelOfCode(code);
  return level !== null && level >= 3 ? (code ?? '').trim().slice(0, -1) : null;
}

/** Slug trang ngành = tên + mã (không trùng vì mã là duy nhất): "Bán buôn thực phẩm", 4632 → ban-buon-thuc-pham-4632. */
export function categorySlug(name: string | null | undefined, code: string | null | undefined): string {
  const base = slugify((name ?? '').replace(/&amp;/g, '&').replace(/&/g, ' va '));
  const suffix = (code ?? '').trim().toLowerCase();
  return [base, suffix].filter(Boolean).join('-');
}

/** "4632 · Bán buôn thực phẩm"; chưa có mã thì chỉ tên. */
export function codeAndName(category: { code?: string | null; name?: string | null } | null | undefined): string {
  const name = category?.name ?? '';
  return category?.code ? `${category.code} · ${name}` : name;
}
