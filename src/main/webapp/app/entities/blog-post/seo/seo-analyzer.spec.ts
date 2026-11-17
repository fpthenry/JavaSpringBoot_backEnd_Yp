import { describe, expect, it } from 'vitest';

import { SeoInput, analyzeSeo, countKeyword, fold, isInternalLink, parseContent, wordCount } from './seo-analyzer';

const statusOf = (input: SeoInput, id: string): string | undefined => analyzeSeo(input).checks.find(check => check.id === id)?.status;

const words = (n: number, word = 'chữ'): string => Array.from({ length: n }, () => word).join(' ');

/** Bài mẫu đạt chuẩn: từ khóa ở đầu tiêu đề, đường dẫn, tóm tắt, đoạn đầu, H2, alt ảnh; đủ dài; có link. */
const GOOD_POST: SeoInput = {
  focusKeyword: 'Thành lập công ty',
  title: 'Thành lập công ty năm 2027: thủ tục và chi phí mới nhất',
  slug: 'thanh-lap-cong-ty-nam-2027',
  excerpt:
    'Hướng dẫn thành lập công ty năm 2027 từ A đến Z: hồ sơ cần chuẩn bị, các bước nộp online, lệ phí và thời gian nhận giấy phép kinh doanh.',
  content: `
    <p>Thành lập công ty là bước đầu tiên khi khởi nghiệp. ${words(80)}</p>
    <h2>Hồ sơ thành lập công ty</h2>
    <p>${words(100)}</p>
    <p>${words(50)} thành lập công ty ${words(40)}</p>
    <img src="/a.png" alt="Hồ sơ thành lập công ty" />
    <h2>Chi phí</h2>
    <p>${words(100)}</p>
    <p>${words(60)} thành lập công ty ${words(40)}</p>
    <p>${words(140)} <a href="/blog/giay-phep">giấy phép</a> <a href="https://dangkykinhdoanh.gov.vn">cổng đăng ký</a></p>
  `,
  thumbnail: 'https://cdn.yp.vn/thanh-lap.png',
  categoryCount: 1,
  tagCount: 2,
};

describe('seo-analyzer helpers', () => {
  it('matches a Vietnamese phrase as a whole, case-insensitive', () => {
    expect(countKeyword('Thành lập công ty. THÀNH LẬP CÔNG TY mới', 'thành lập công ty')).toBe(2);
    expect(countKeyword('thành lập công tyxyz', 'thành lập công ty')).toBe(0);
    expect(countKeyword('nhà hàng', 'nhà')).toBe(1);
    expect(countKeyword('nhàhàng', 'nhà')).toBe(0);
  });

  it('folds accents for URL comparison', () => {
    expect(fold('Đường Thành Lập')).toBe('duong thanh lap');
  });

  it('counts words and parses HTML', () => {
    expect(wordCount('Một hai, ba!  bốn')).toBe(4);
    const parsed = parseContent('<h2>A</h2><p>x <a href="/b">y</a></p><img src="1" alt="">');
    expect(parsed.headings).toEqual(['A']);
    expect(parsed.paragraphs).toEqual(['x y']);
    expect(parsed.images).toEqual([{ src: '1', alt: '' }]);
    expect(parsed.links).toEqual(['/b']);
  });

  it('tells internal links (relative, yp.vn) from external', () => {
    expect(isInternalLink('/blog/a')).toBe(true);
    expect(isInternalLink('https://yp.vn/x')).toBe(true);
    expect(isInternalLink('https://www.yp.vn/x')).toBe(true);
    expect(isInternalLink('https://google.com')).toBe(false);
    expect(isInternalLink('#top')).toBe(false);
  });
});

describe('analyzeSeo', () => {
  it('scores a well-optimised post as good', () => {
    const result = analyzeSeo(GOOD_POST);
    const notGood = result.checks.filter(check => check.status !== 'good').map(check => check.id);
    expect(notGood).toEqual([]);
    expect(result.score).toBe(100);
    expect(result.level).toBe('good');
  });

  it('scores an empty post as poor and asks for a keyword first', () => {
    const result = analyzeSeo({});
    expect(result.level).toBe('bad');
    expect(result.checks[0].status).toBe('bad');
    expect(result.checks.some(check => check.id === 'keyword')).toBe(true);
  });

  it('flags the keyword missing from title, URL, excerpt and introduction', () => {
    const input = { ...GOOD_POST, focusKeyword: 'giấy phép kinh doanh' };
    expect(statusOf(input, 'kwTitle')).toBe('bad');
    expect(statusOf(input, 'kwSlug')).toBe('bad');
    expect(statusOf(input, 'kwDescription')).toBe('good');
    expect(statusOf(input, 'kwIntro')).toBe('bad');
  });

  it('keyword late in the title is only ok', () => {
    expect(statusOf({ ...GOOD_POST, title: 'Hướng dẫn chi tiết mới nhất về thành lập công ty' }, 'kwTitle')).toBe('ok');
  });

  it('detects keyword stuffing', () => {
    const stuffed = { ...GOOD_POST, content: `<p>${Array.from({ length: 30 }, () => 'thành lập công ty').join(' ')} ${words(100)}</p>` };
    expect(statusOf(stuffed, 'kwDensity')).toBe('bad');
  });

  it('checks title and excerpt length', () => {
    expect(statusOf({ ...GOOD_POST, title: 'Ngắn' }, 'titleLength')).toBe('bad');
    expect(statusOf({ ...GOOD_POST, excerpt: '' }, 'descriptionLength')).toBe('bad');
    expect(statusOf({ ...GOOD_POST, excerpt: 'x'.repeat(100) }, 'descriptionLength')).toBe('ok');
  });

  it('builds the Google preview like the website does', () => {
    const result = analyzeSeo({ ...GOOD_POST, excerpt: '', content: '<p>Đoạn đầu của bài.</p>' });
    expect(result.snippet.url).toBe('yp.vn › blog › thanh-lap-cong-ty-nam-2027');
    expect(result.snippet.title.endsWith('…') || result.snippet.title.includes('| Trang Vàng Việt Nam')).toBe(true);
    expect(result.snippet.description).toBe('Đoạn đầu của bài.');
    expect(result.snippet.descriptionFromContent).toBe(true);
  });
});
