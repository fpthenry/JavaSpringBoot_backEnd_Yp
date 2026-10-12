import { describe, expect, it } from 'vitest';

import {
  blogStatusLabelKey,
  categoryTreeOptions,
  cleanArticleHtml,
  excerptWithoutLink,
  httpsUrl,
  plainExcerpt,
  publicUrlFromExcerpt,
  readingStats,
} from './blog-post-display';

describe('blog-post-display', () => {
  it('plainExcerpt strips HTML, WordPress shortcodes and entities, cuts at a word', () => {
    expect(plainExcerpt('[vc_row][vc_column_text]<p>Khuyến mại <b>tháng 7</b> &amp; quà</p>[/vc_column_text][/vc_row]')).toBe(
      'Khuyến mại tháng 7 & quà',
    );
    expect(plainExcerpt('<style>p{color:red}</style><p>Chào</p>')).toBe('Chào');
    const long = plainExcerpt('Một hai ba bốn năm sáu bảy tám chín mười', 20);
    expect(long.endsWith('…')).toBe(true);
    expect(long.length).toBeLessThanOrEqual(21);
    expect(long).not.toContain('bả');
    expect(plainExcerpt('nối&#8220;Bán&#8221;')).toBe('nối“Bán”');
    expect(plainExcerpt(null)).toBe('');
  });

  it('categoryTreeOptions orders parents before children, indented', () => {
    expect(
      categoryTreeOptions([
        { id: 3, name: 'Sự kiện', parent: null },
        { id: 2, name: 'Khuyến mại', parent: { id: 1 } },
        { id: 1, name: 'Bài cũ', parent: null },
        { id: 9, name: 'Mồ côi', parent: { id: 99 } },
      ]).map(option => option.label),
    ).toEqual(['Bài cũ', '— Khuyến mại', 'Mồ côi', 'Sự kiện']);
  });

  it('status labels', () => {
    expect(blogStatusLabelKey('draft')).toBe('javaSpringBootBackEndApp.blogPost.statusValue.draft');
    expect(blogStatusLabelKey('pending')).toBe('pending');
  });
  it('reads the public link from the WordPress excerpt and removes it from the text', () => {
    const excerpt = '<p>Tóm tắt&#8230; <a class="view-article" href="https://yp.com.vn/bai-a/">Xem bài viết</a></p>';
    expect(publicUrlFromExcerpt(excerpt)).toBe('https://yp.com.vn/bai-a/');
    expect(publicUrlFromExcerpt('<p>không có link</p>')).toBeNull();
    expect(excerptWithoutLink(excerpt)).toBe('<p>Tóm tắt&#8230; </p>');
    expect(plainExcerpt(excerpt)).toBe('Tóm tắt…');
  });

  it('cleanArticleHtml drops shortcodes and data-* attributes; readingStats counts words', () => {
    expect(cleanArticleHtml('[vc_row][vc_column_text]<p data-index-in-node="3">A <b>B</b></p>[/vc_column_text][/vc_row]')).toBe(
      '<p>A <b>B</b></p>',
    );
    expect(readingStats('<p>' + 'từ '.repeat(450) + '</p>')).toEqual({ words: 450, minutes: 2 });
    expect(readingStats(null)).toEqual({ words: 0, minutes: 1 });
  });

  it('upgrades http:// and //host images to https:// (CSP allows https images only); https stays', () => {
    expect(cleanArticleHtml(`<img src="http://a.vn/x.jpg"><img src='//b.vn/y.png'><img src="https://c.vn/z.jpg">`)).toBe(
      `<img src="https://a.vn/x.jpg"><img src='https://b.vn/y.png'><img src="https://c.vn/z.jpg">`,
    );
    expect(cleanArticleHtml('<a href="http://a.vn">link</a>')).toBe('<a href="http://a.vn">link</a>');
    expect(httpsUrl('http://yp.vn/a.jpg')).toBe('https://yp.vn/a.jpg');
    expect(httpsUrl('//yp.vn/a.jpg')).toBe('https://yp.vn/a.jpg');
    expect(httpsUrl(' ')).toBeNull();
  });
});
