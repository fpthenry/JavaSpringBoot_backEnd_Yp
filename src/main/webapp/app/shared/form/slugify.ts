/**
 * Đường dẫn (slug) từ tên tiếng Việt, cùng kiểu với slug của dữ liệu WordPress:
 * "Lập trình máy vi tính" → "lap-trinh-may-vi-tinh"; "BAO BÌ &amp; GIẤY" → "bao-bi-giay".
 */
export function slugify(text: string | null | undefined): string {
  return (text ?? '')
    .replace(/&amp;/g, ' ')
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .replace(/[đĐ]/g, 'd')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}
