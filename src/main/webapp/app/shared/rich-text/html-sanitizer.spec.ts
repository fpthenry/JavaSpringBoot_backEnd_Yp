import { describe, expect, it } from 'vitest';

import { extractImageUrls, sanitizeHtml } from './html-sanitizer';

describe('sanitizeHtml', () => {
  it('bỏ thẻ và thuộc tính nguy hiểm, giữ nội dung', () => {
    const html = sanitizeHtml(
      '<p onclick="x()">Chào <b>bạn</b></p><script>alert(1)</script><img src="https://a/b.jpg" onerror="hack()"><a href="javascript:alert(1)">x</a><iframe src="https://evil"></iframe>',
    );
    expect(html).toContain('<b>bạn</b>');
    expect(html).toContain('<img src="https://a/b.jpg">');
    expect(html).not.toMatch(/script|onclick|onerror|javascript:|iframe/i);
  });

  it('giữ class/style với nội dung có sẵn (không strict) để không vỡ trình bày WordPress', () => {
    expect(sanitizeHtml('<div class="vc_row" style="color:red">A</div>')).toBe('<div class="vc_row" style="color:red">A</div>');
  });

  it('khi dán (strict): bỏ class/style/id, bóc span/font, bỏ đoạn rỗng, giữ ảnh, bảng, link', () => {
    const html = sanitizeHtml(
      '<!--StartFragment--><p class="MsoNormal" style="margin:0"><span style="font-size:12pt"><font color="red">Nội dung</font></span><o:p></o:p></p>' +
        '<p class="x"> </p><table class="t"><tr><td colspan="2" style="width:10px">Ô</td></tr></table>' +
        '<a href="https://yp.com.vn" target="_blank" class="btn">link</a><img src="https://a/c.png" alt="ảnh" style="width:100px" data-id="1">',
      { strict: true },
    );
    expect(html).toBe(
      '<p>Nội dung</p><table><tbody><tr><td colspan="2">Ô</td></tr></tbody></table>' +
        '<a href="https://yp.com.vn" target="_blank" rel="noopener noreferrer">link</a><img src="https://a/c.png" alt="ảnh">',
    );
  });
});

describe('extractImageUrls', () => {
  it('lấy URL ảnh http(s) không trùng, bỏ ảnh base64', () => {
    expect(
      extractImageUrls(
        '<img src="https://a/1.jpg"><p><img src="https://a/1.jpg"><img src="data:image/png;base64,AAA"><img src="http://b/2.png"></p>',
      ),
    ).toEqual(['https://a/1.jpg', 'http://b/2.png']);
    expect(extractImageUrls(null)).toEqual([]);
  });
});
