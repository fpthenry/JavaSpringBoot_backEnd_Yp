import { describe, expect, it } from 'vitest';

import {
  completenessChecks,
  completenessPercent,
  mapsHref,
  mapsSearchHref,
  parseLocationJson,
  splitEmails,
  splitPhones,
  statusLabelKey,
  telHref,
  websiteHref,
} from './listing-display';

describe('listing-display', () => {
  it('statusLabelKey: known statuses are translated, others shown as is', () => {
    expect(statusLabelKey('publish')).toBe('javaSpringBootBackEndApp.listing.statusValue.publish');
    expect(statusLabelKey('pending')).toBe('pending');
  });

  it('splits several emails and phones in one field', () => {
    expect(splitEmails('hoadauvietnam@gmail.com;trung2001@gmail.com')).toEqual(['hoadauvietnam@gmail.com', 'trung2001@gmail.com']);
    expect(splitEmails('a@x.com, b@y.com')).toEqual(['a@x.com', 'b@y.com']);
    expect(splitEmails('khong co')).toEqual([]);
    expect(splitPhones('(028)39448202 - 0909123456; 024-3816')).toEqual(['(028)39448202', '0909123456', '024-3816']);
    expect(splitPhones(null)).toEqual([]);
    expect(telHref('+84 919 888 897')).toBe('tel:+84919888897');
  });

  it('websiteHref adds https:// and keeps existing protocol', () => {
    expect(websiteHref('www.rupedl.vn')).toBe('https://www.rupedl.vn');
    expect(websiteHref('http://abc.vn')).toBe('http://abc.vn');
    expect(websiteHref('  ')).toBeNull();
  });

  it('parseLocationJson reads the original place; broken JSON gives null', () => {
    expect(parseLocationJson('{"province":"Thành phố Hà Nội","ward":"Xã Yên Lãng","coordinate":"21.1721:105.662"}')).toEqual({
      province: 'Thành phố Hà Nội',
      ward: 'Xã Yên Lãng',
      street: null,
      coordinate: '21.1721:105.662',
    });
    expect(parseLocationJson('{oops')).toBeNull();
    expect(parseLocationJson('{}')).toBeNull();
  });

  it('maps links keep coordinate digits exactly; fall back to address search', () => {
    expect(mapsHref('21.17210000001:105.662')).toBe('https://www.google.com/maps?q=21.17210000001,105.662');
    expect(mapsHref('lat:long')).toBeNull();
    expect(mapsSearchHref('1 Lãng Yên, Hà Nội')).toBe(
      'https://www.google.com/maps/search/?api=1&query=1%20L%C3%A3ng%20Y%C3%AAn%2C%20H%C3%A0%20N%E1%BB%99i',
    );
  });

  it('completeness: empty HTML description counts as missing', () => {
    const checks = completenessChecks({ id: 1, phone: '', mobile: '0909', description: '<p> </p>', categories: [{ id: 4 }] }, false);
    expect(checks.filter(check => !check.ok).map(check => check.key)).toEqual([
      'addressNew',
      'email',
      'website',
      'taxCode',
      'newUnit',
      'description',
    ]);
    expect(completenessPercent(checks)).toBe(25);
  });
});
