import { describe, expect, it } from 'vitest';

import { blogStatusLabelKey, categoryTreeOptions, plainExcerpt } from './blog-post-display';

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
});
