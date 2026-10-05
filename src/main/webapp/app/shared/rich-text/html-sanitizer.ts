/**
 * Làm sạch HTML cho trình soạn thảo bài viết (không dùng thư viện ngoài).
 *
 * - Luôn bỏ: thẻ nguy hiểm (script, style, iframe, object, form…), thuộc tính sự kiện on*, link javascript:/vbscript:.
 * - Khi dán (strict): bỏ thêm class, id, style, data-*, thẻ rác của Word (o:p, font, span rỗng…) để nội dung gọn.
 */

const DANGEROUS_TAGS = [
  'script',
  'style',
  'iframe',
  'object',
  'embed',
  'form',
  'input',
  'button',
  'select',
  'textarea',
  'link',
  'meta',
  'base',
  'noscript',
  'template',
  'svg',
  'math',
];
const URL_ATTRS = ['href', 'src', 'action', 'formaction', 'xlink:href'];
/** Thuộc tính được giữ khi dán (strict). */
const PASTE_ALLOWED_ATTRS = new Set(['href', 'src', 'alt', 'title', 'width', 'height', 'colspan', 'rowspan', 'target', 'rel']);
/** Thẻ chỉ bóc vỏ (giữ nội dung) khi dán. */
const PASTE_UNWRAP_TAGS = new Set(['font', 'span', 'o:p', 'center', 'section', 'article', 'header', 'footer', 'main', 'aside', 'nav']);

const isUnsafeUrl = (value: string): boolean => /^\s*(javascript|vbscript|data:text\/html)/i.test(value);

export function sanitizeHtml(html: string, options: { strict?: boolean } = {}): string {
  const doc = new DOMParser().parseFromString(`<body>${html}</body>`, 'text/html');
  const body = doc.body;
  body.querySelectorAll(DANGEROUS_TAGS.join(',')).forEach(el => el.remove());
  // Bình luận HTML (Word chèn rất nhiều)
  const walker = doc.createTreeWalker(body, NodeFilter.SHOW_COMMENT);
  const comments: Node[] = [];
  while (walker.nextNode()) {
    comments.push(walker.currentNode);
  }
  comments.forEach(c => c.parentNode?.removeChild(c));

  body.querySelectorAll('*').forEach(el => {
    for (const attr of Array.from(el.attributes)) {
      const name = attr.name.toLowerCase();
      if (name.startsWith('on') || (URL_ATTRS.includes(name) && isUnsafeUrl(attr.value))) {
        el.removeAttribute(attr.name);
      } else if (options.strict && !PASTE_ALLOWED_ATTRS.has(name)) {
        el.removeAttribute(attr.name);
      }
    }
    if (el.tagName === 'A' && el.getAttribute('target') === '_blank') {
      el.setAttribute('rel', 'noopener noreferrer');
    }
  });

  if (options.strict) {
    // Bóc vỏ thẻ trình bày, giữ nội dung bên trong
    Array.from(body.querySelectorAll('*'))
      .filter(el => PASTE_UNWRAP_TAGS.has(el.tagName.toLowerCase()))
      .reverse()
      .forEach(el => el.replaceWith(...Array.from(el.childNodes)));
    // Đoạn rỗng thừa
    body.querySelectorAll('p, div').forEach(el => {
      if (!el.textContent.trim() && !el.querySelector('img, br, table, hr')) {
        el.remove();
      }
    });
  }
  return body.innerHTML.trim();
}

/** Lấy URL các ảnh trong nội dung (để chọn làm ảnh đại diện). */
export function extractImageUrls(html: string | null | undefined): string[] {
  if (!html) {
    return [];
  }
  const doc = new DOMParser().parseFromString(html, 'text/html');
  const urls = Array.from(doc.querySelectorAll('img'))
    .map(img => img.getAttribute('src') ?? '')
    .filter(src => /^https?:\/\//i.test(src));
  return Array.from(new Set(urls));
}
