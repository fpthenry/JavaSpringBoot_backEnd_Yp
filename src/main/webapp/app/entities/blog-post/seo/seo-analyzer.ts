/**
 * Sửa tay (2026-11-17): chấm điểm SEO bài viết ngay trên trình duyệt, theo kiểu Yoast/Rank Math của WordPress.
 * Hàm thuần (không gọi server) để soạn bài thấy điểm thay đổi khi gõ, và để test dễ.
 *
 * Khớp với cách website (Next.js, src/lib/seo/blog-page.ts) dựng thẻ meta:
 * - <title> = tiêu đề bài + " | Trang Vàng Việt Nam"
 * - meta description = Tóm tắt (excerpt); bỏ trống thì lấy đoạn đầu nội dung, cắt 160 ký tự
 * - đường dẫn = /blog/{slug}
 */

export type SeoStatus = 'good' | 'ok' | 'bad';

export interface SeoCheck {
  id: string;
  status: SeoStatus;
  /** Khóa dịch `javaSpringBootBackEndApp.blogPost.seo.checks.<id>.<status>`. */
  message: string;
  params?: Record<string, string | number>;
  /** Trọng số khi tính điểm. */
  weight: number;
}

export interface SeoInput {
  focusKeyword?: string | null;
  title?: string | null;
  slug?: string | null;
  excerpt?: string | null;
  content?: string | null;
  thumbnail?: string | null;
  categoryCount?: number;
  tagCount?: number;
}

export interface SeoResult {
  /** 0–100. */
  score: number;
  level: SeoStatus;
  checks: SeoCheck[];
  /** Xem trước kết quả trên Google. */
  snippet: { title: string; url: string; description: string; descriptionFromContent: boolean };
  stats: { words: number; keywordCount: number; density: number };
}

export const SITE_TITLE_SUFFIX = ' | Trang Vàng Việt Nam';
export const SITE_HOST = 'yp.vn';

/** Giới hạn độ dài (ký tự) Google thường hiển thị. */
export const TITLE_RANGE = { min: 30, max: 60 };
export const DESCRIPTION_RANGE = { min: 120, max: 160 };
export const CONTENT_WORDS = { ok: 300, good: 600 };
export const DENSITY_RANGE = { min: 0.5, max: 2.5 };
const LONG_PARAGRAPH_WORDS = 150;
const SLUG_MAX = 75;

/** Chữ thường, chuẩn Unicode NFC, gộp khoảng trắng: để so khớp tiếng Việt có dấu. */
export function normalize(text: string | null | undefined): string {
  return (text ?? '').normalize('NFC').toLowerCase().replace(/\s+/g, ' ').trim();
}

/** Bỏ dấu tiếng Việt (đ → d): dùng khi so với đường dẫn. */
export function fold(text: string | null | undefined): string {
  return normalize(text).normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/đ/g, 'd');
}

function escapeRegExp(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/** Số lần từ khóa xuất hiện như một cụm từ trọn vẹn (không dính vào chữ khác). */
export function countKeyword(text: string, keyword: string): number {
  const kw = normalize(keyword);
  if (!kw) {
    return 0;
  }
  const pattern = new RegExp(`(?<![\\p{L}\\p{N}])${escapeRegExp(kw).replace(/ /g, '\\s+')}(?![\\p{L}\\p{N}])`, 'gu');
  return normalize(text).match(pattern)?.length ?? 0;
}

export function containsKeyword(text: string | null | undefined, keyword: string): boolean {
  return countKeyword(text ?? '', keyword) > 0;
}

export function wordCount(text: string): number {
  const words = normalize(text).match(/[\p{L}\p{N}]+/gu);
  return words?.length ?? 0;
}

interface ParsedContent {
  text: string;
  paragraphs: string[];
  headings: string[];
  images: { src: string; alt: string }[];
  links: string[];
}

/** Tách nội dung HTML: chữ, đoạn văn, tiêu đề phụ H2–H4, ảnh, liên kết. */
export function parseContent(html: string | null | undefined): ParsedContent {
  const doc = new DOMParser().parseFromString(html ?? '', 'text/html');
  const textOf = (el: Element): string => el.textContent.replace(/\s+/g, ' ').trim();
  const paragraphs = Array.from(doc.querySelectorAll('p, li')).map(textOf).filter(Boolean);
  return {
    text: doc.body.textContent.replace(/\s+/g, ' ').trim(),
    paragraphs: paragraphs.length ? paragraphs : [textOf(doc.body)].filter(Boolean),
    headings: Array.from(doc.querySelectorAll('h2, h3, h4')).map(textOf),
    images: Array.from(doc.querySelectorAll('img')).map(img => ({
      src: img.getAttribute('src') ?? '',
      alt: img.getAttribute('alt') ?? '',
    })),
    links: Array.from(doc.querySelectorAll('a[href]')).map(a => a.getAttribute('href') ?? ''),
  };
}

/** Liên kết nội bộ: đường dẫn tương đối hoặc trỏ về yp.vn. */
export function isInternalLink(href: string): boolean {
  if (href.startsWith('#') || href.startsWith('mailto:') || href.startsWith('tel:')) {
    return false;
  }
  if (href.startsWith('/')) {
    return true;
  }
  try {
    const host = new URL(href).hostname;
    return host === SITE_HOST || host.endsWith(`.${SITE_HOST}`);
  } catch {
    return false;
  }
}

function truncate(text: string, max: number): string {
  return text.length <= max ? text : `${text.slice(0, max - 1).trimEnd()}…`;
}

const SCORE: Record<SeoStatus, number> = { good: 1, ok: 0.5, bad: 0 };

export function analyzeSeo(input: SeoInput): SeoResult {
  const keyword = normalize(input.focusKeyword);
  const title = (input.title ?? '').trim();
  const slug = (input.slug ?? '').trim();
  const excerpt = (input.excerpt ?? '').replace(/\s+/g, ' ').trim();
  const content = parseContent(input.content);
  const words = wordCount(content.text);
  const checks: SeoCheck[] = [];
  const add = (id: string, status: SeoStatus, weight: number, params?: Record<string, string | number>): void => {
    checks.push({ id, status, weight, message: `javaSpringBootBackEndApp.blogPost.seo.checks.${id}.${status}`, params });
  };

  // ---- Từ khóa chính ----
  let keywordCount = 0;
  let density = 0;
  if (!keyword) {
    add('keyword', 'bad', 4);
  } else {
    keywordCount = countKeyword(content.text, keyword);
    // Như Rank Math: số lần xuất hiện / tổng số từ. Không nhân số chữ của cụm từ vì tiếng Việt đếm theo tiếng (âm tiết).
    density = words ? Math.round((keywordCount / words) * 1000) / 10 : 0;

    const titlePos = normalize(title).indexOf(keyword);
    add('kwTitle', titlePos === -1 ? 'bad' : titlePos <= 10 ? 'good' : 'ok', 3);
    add('kwSlug', fold(slug).replace(/-/g, ' ').includes(fold(keyword)) ? 'good' : 'bad', 2);
    add('kwDescription', containsKeyword(excerpt, keyword) ? 'good' : 'bad', 2);
    const intro = content.paragraphs.slice(0, 2).join(' ');
    add('kwIntro', containsKeyword(intro, keyword) ? 'good' : 'bad', 2);
    if (keywordCount === 0) {
      add('kwDensity', 'bad', 2, { count: 0, density: 0 });
    } else {
      add('kwDensity', density > DENSITY_RANGE.max ? 'bad' : density >= DENSITY_RANGE.min ? 'good' : 'ok', 2, {
        count: keywordCount,
        density,
      });
    }
    add('kwSubheading', content.headings.some(heading => containsKeyword(heading, keyword)) ? 'good' : 'ok', 1);
    if (content.images.length) {
      add('kwImageAlt', content.images.some(image => containsKeyword(image.alt, keyword)) ? 'good' : 'ok', 1);
    }
  }

  // ---- Cơ bản ----
  const titleLength = title.length;
  add(
    'titleLength',
    titleLength >= TITLE_RANGE.min && titleLength <= TITLE_RANGE.max ? 'good' : titleLength >= 20 && titleLength <= 70 ? 'ok' : 'bad',
    2,
    { length: titleLength, min: TITLE_RANGE.min, max: TITLE_RANGE.max },
  );

  const descLength = excerpt.length;
  add(
    'descriptionLength',
    !descLength
      ? 'bad'
      : descLength >= DESCRIPTION_RANGE.min && descLength <= DESCRIPTION_RANGE.max
        ? 'good'
        : descLength >= 70 && descLength <= 200
          ? 'ok'
          : 'bad',
    2,
    { length: descLength, min: DESCRIPTION_RANGE.min, max: DESCRIPTION_RANGE.max },
  );

  add('contentLength', words >= CONTENT_WORDS.good ? 'good' : words >= CONTENT_WORDS.ok ? 'ok' : 'bad', 2, {
    words,
    min: CONTENT_WORDS.ok,
    good: CONTENT_WORDS.good,
  });

  add('subheadings', content.headings.length ? 'good' : words >= CONTENT_WORDS.ok ? 'bad' : 'ok', 1, { count: content.headings.length });

  const longParagraphs = content.paragraphs.filter(paragraph => wordCount(paragraph) > LONG_PARAGRAPH_WORDS).length;
  add('paragraphs', longParagraphs ? 'ok' : 'good', 1, { count: longParagraphs, max: LONG_PARAGRAPH_WORDS });

  add('thumbnail', input.thumbnail?.trim() ? 'good' : 'bad', 1);

  const missingAlt = content.images.filter(image => !image.alt.trim()).length;
  add('imageAlt', !content.images.length ? 'ok' : missingAlt ? 'ok' : 'good', 1, { missing: missingAlt, total: content.images.length });

  const internal = content.links.filter(isInternalLink).length;
  const external = content.links.filter(href => /^https?:\/\//.test(href) && !isInternalLink(href)).length;
  add('internalLinks', internal ? 'good' : 'ok', 1, { count: internal });
  add('externalLinks', external ? 'good' : 'ok', 1, { count: external });

  add('slug', !slug ? 'bad' : slug.length <= SLUG_MAX ? 'good' : 'ok', 1, { length: slug.length, max: SLUG_MAX });
  add('categories', (input.categoryCount ?? 0) > 0 ? 'good' : 'bad', 1);
  add('tags', (input.tagCount ?? 0) > 0 ? 'good' : 'ok', 1);

  const totalWeight = checks.reduce((sum, check) => sum + check.weight, 0);
  const score = Math.round((checks.reduce((sum, check) => sum + check.weight * SCORE[check.status], 0) / totalWeight) * 100);
  const order: Record<SeoStatus, number> = { bad: 0, ok: 1, good: 2 };
  checks.sort((a, b) => order[a.status] - order[b.status]);

  // Mô tả Google: giống website — Tóm tắt, không có thì đoạn đầu nội dung (bỏ phần lặp tiêu đề).
  let description = excerpt;
  const descriptionFromContent = !description;
  if (descriptionFromContent) {
    description = content.text;
    if (title && description.toLowerCase().startsWith(title.toLowerCase())) {
      description = description.slice(title.length).replace(/^[\s.:,;–-]+/, '');
    }
  }

  return {
    score,
    level: score >= 80 ? 'good' : score >= 50 ? 'ok' : 'bad',
    checks,
    snippet: {
      title: truncate(`${title || '…'}${SITE_TITLE_SUFFIX}`, 65),
      url: `${SITE_HOST} › blog › ${slug || '…'}`,
      description: truncate(description || title, DESCRIPTION_RANGE.max),
      descriptionFromContent,
    },
    stats: { words, keywordCount, density },
  };
}
