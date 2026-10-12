import { decodeEntities } from 'app/shared/form';

/**
 * Sửa tay: hàm hiển thị bài viết dùng ở trang danh sách (đoạn trích chữ thường, nhãn trạng thái). Hàm thuần.
 */

/** Liên kết "Xem bài viết" WordPress để sẵn trong đoạn tóm tắt: <a class="view-article" href="https://yp.com.vn/slug/">. */
const VIEW_ARTICLE_LINK = /<a\b[^>]*class="[^"]*view-article[^"]*"[^>]*>[\s\S]*?<\/a>/gi;

/** Trạng thái hay dùng; giá trị khác hiện nguyên văn. */
export const BLOG_POST_STATUSES = ['publish', 'draft'];

export function blogStatusLabelKey(status: string): string {
  return BLOG_POST_STATUSES.includes(status) ? `javaSpringBootBackEndApp.blogPost.statusValue.${status}` : status;
}

/**
 * Chữ thường từ HTML bài viết WordPress: bỏ thẻ HTML và shortcode ([vc_row]…), giải mã &amp;…, gộp khoảng trắng,
 * cắt ở ranh giới từ (thêm "…"). Dùng cho đoạn trích dưới tiêu đề.
 */
export function plainExcerpt(html: string | null | undefined, maxLength = 180): string {
  const text = decodeEntities(
    (html ?? '')
      .replace(VIEW_ARTICLE_LINK, ' ')
      .replace(/<(script|style)[\s\S]*?<\/\1>/gi, ' ')
      .replace(/<[^>]*>/g, ' ')
      .replace(/\[\/?[a-z_][\w-]*(?:\s[^\]]*)?\]/gi, ' '),
  )
    .replace(/\s+/g, ' ')
    .trim();
  if (text.length <= maxLength) {
    return text;
  }
  const cut = text.slice(0, maxLength);
  const lastSpace = cut.lastIndexOf(' ');
  return `${(lastSpace > maxLength * 0.6 ? cut.slice(0, lastSpace) : cut).replace(/[\s,.;:–-]+$/, '')}…`;
}

/** Một dòng trong ô chọn danh mục: tên kèm thụt lề theo cấp (danh mục con dưới danh mục cha). */
export interface CategoryOption {
  id: number;
  label: string;
  depth: number;
}

/** Sắp danh mục theo cây (cha rồi tới con, cùng cấp theo tên); danh mục mồ côi coi như gốc. */
export function categoryTreeOptions(categories: { id: number; name?: string | null; parent?: { id: number } | null }[]): CategoryOption[] {
  const ids = new Set(categories.map(category => category.id));
  const children = new Map<number | null, typeof categories>();
  for (const category of categories) {
    const parentId = category.parent && ids.has(category.parent.id) ? category.parent.id : null;
    children.set(parentId, [...(children.get(parentId) ?? []), category]);
  }
  const result: CategoryOption[] = [];
  const seen = new Set<number>();
  const walk = (parentId: number | null, depth: number): void => {
    const list = [...(children.get(parentId) ?? [])].sort((a, b) => (a.name ?? '').localeCompare(b.name ?? '', 'vi'));
    for (const category of list) {
      if (seen.has(category.id)) {
        continue;
      }
      seen.add(category.id);
      result.push({ id: category.id, label: `${'— '.repeat(depth)}${category.name ?? ''}`, depth });
      walk(category.id, depth + 1);
    }
  };
  walk(null, 0);
  return result;
}

/** Địa chỉ bài trên website, lấy từ liên kết "Xem bài viết" trong tóm tắt (không đoán tên miền). */
export function publicUrlFromExcerpt(excerpt: string | null | undefined): string | null {
  const link = (excerpt ?? '').match(VIEW_ARTICLE_LINK)?.[0];
  const href = link?.match(/href="(https?:\/\/[^"]+)"/i)?.[1];
  return href ?? null;
}

/** Tóm tắt bỏ liên kết "Xem bài viết" (đã có nút riêng). */
export function excerptWithoutLink(excerpt: string | null | undefined): string {
  return (excerpt ?? '').replace(VIEW_ARTICLE_LINK, '');
}

/**
 * Nội dung bài để xem trong trang quản trị: bỏ shortcode WPBakery/WordPress ([vc_row], [/vc_column_text]…) nhưng giữ
 * phần chữ, ảnh bên trong; bỏ thuộc tính data-* thừa. Angular vẫn lọc mã nguy hiểm khi gán [innerHTML].
 */
export function cleanArticleHtml(html: string | null | undefined): string {
  return (
    (html ?? '')
      .replace(/\[\/?[a-z_][\w-]*(?:\s[^\]]*)?\]/gi, '')
      .replace(/\sdata-[\w-]+="[^"]*"/gi, '')
      // Ảnh http:// hoặc //host: đổi sang https:// (CSP chỉ cho ảnh https, trang chạy https cũng chặn ảnh http)
      .replace(/(\s(?:src|srcset)=["'])(?:http:)?\/\//gi, '$1https://')
      .trim()
  );
}

/** Địa chỉ ảnh http:// hoặc //host đổi sang https:// để hiện được (ảnh đại diện bài viết). */
export function httpsUrl(url: string | null | undefined): string | null {
  const value = url?.trim();
  if (!value) {
    return null;
  }
  return value.replace(/^(?:http:)?\/\//i, 'https://');
}

/** Số từ và phút đọc (200 từ/phút, ít nhất 1 phút) của nội dung HTML. */
export function readingStats(html: string | null | undefined): { words: number; minutes: number } {
  const words = plainExcerpt(html, Number.MAX_SAFE_INTEGER)
    .split(/\s+/)
    // Chỉ tính chữ, số: dấu câu đứng riêng sau khi bỏ thẻ HTML ("<b>trọng</b>.") không phải là từ
    .filter(word => /[\p{L}\p{N}]/u.test(word)).length;
  return { words, minutes: Math.max(1, Math.round(words / 200)) };
}
