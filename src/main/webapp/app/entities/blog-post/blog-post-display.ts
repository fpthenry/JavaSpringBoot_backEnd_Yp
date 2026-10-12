import { decodeEntities } from 'app/shared/form';

/**
 * Sửa tay: hàm hiển thị bài viết dùng ở trang danh sách (đoạn trích chữ thường, nhãn trạng thái). Hàm thuần.
 */

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
