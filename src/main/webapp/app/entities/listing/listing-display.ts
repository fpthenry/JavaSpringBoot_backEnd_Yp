import { IListing } from './listing.model';

/**
 * Sửa tay: hàm hiển thị dùng chung cho trang xem và sửa doanh nghiệp (trạng thái, liên hệ bấm được, vị trí gốc,
 * mức độ đầy đủ hồ sơ). Hàm thuần, không phụ thuộc Angular.
 */

/** Trạng thái hay dùng; giá trị khác có sẵn trong dữ liệu vẫn được giữ và hiện nguyên văn. */
export const LISTING_STATUSES = ['publish', 'draft'];

/** Đuôi slug của đơn vị hành chính mới (từ 01/7/2025). */
export const NEW_UNIT_SUFFIX = '-2025';

export function statusLabelKey(status: string): string {
  return LISTING_STATUSES.includes(status) ? `javaSpringBootBackEndApp.listing.statusValue.${status}` : status;
}

/** Ô email trong dữ liệu có thể chứa nhiều địa chỉ: "a@x.com;b@y.com", "a@x.com, b@y.com". */
export function splitEmails(value: string | null | undefined): string[] {
  return (value ?? '')
    .split(/[;,\s]+/)
    .map(email => email.trim())
    .filter(email => email.includes('@'));
}

/** Nhiều số điện thoại trong một ô: tách theo ; , / hoặc " - " (giữ dấu gạch trong số như 024-3816). */
export function splitPhones(value: string | null | undefined): string[] {
  return (value ?? '')
    .split(/[;,/]|\s-\s/)
    .map(phone => phone.trim())
    .filter(phone => /\d/.test(phone));
}

/** "(024)38164331" → "tel:02438164331"; "+84 919 888 897" → "tel:+84919888897". */
export function telHref(phone: string): string {
  const digits = phone.replace(/[^\d+]/g, '');
  return `tel:${digits}`;
}

/** Website trong dữ liệu thường thiếu giao thức ("www.abc.vn"): thêm https:// để mở được. */
export function websiteHref(value: string | null | undefined): string | null {
  const site = (value ?? '').trim().split(/[\s;,]+/)[0];
  if (!site) {
    return null;
  }
  return /^https?:\/\//i.test(site) ? site : `https://${site}`;
}

/** Thông tin vị trí gốc trong locationJson (tỉnh, phường, đường, tọa độ dạng chữ "lat:long"). */
export interface ListingPlace {
  province: string | null;
  ward: string | null;
  street: string | null;
  /** Giữ nguyên chữ như dữ liệu gốc, không làm tròn. */
  coordinate: string | null;
}

export function parseLocationJson(value: string | null | undefined): ListingPlace | null {
  if (!value) {
    return null;
  }
  try {
    const data = JSON.parse(value) as Record<string, unknown>;
    const text = (key: string): string | null => {
      const field = data[key];
      return typeof field === 'string' && field.trim() ? field.trim() : null;
    };
    const place = { province: text('province'), ward: text('ward'), street: text('street'), coordinate: text('coordinate') };
    return Object.values(place).some(Boolean) ? place : null;
  } catch {
    return null;
  }
}

/**
 * Link Google Maps từ tọa độ "21.1721:105.662" (chép nguyên chữ, không đổi sang số để không mất chữ số).
 * Không phải tọa độ hợp lệ → null.
 */
export function mapsHref(coordinate: string | null | undefined): string | null {
  const match = /^\s*(-?\d+(?:\.\d+)?)\s*[:,]\s*(-?\d+(?:\.\d+)?)\s*$/.exec(coordinate ?? '');
  return match ? `https://www.google.com/maps?q=${match[1]},${match[2]}` : null;
}

/** Bản đồ theo địa chỉ khi không có tọa độ. */
export function mapsSearchHref(address: string | null | undefined): string | null {
  const text = (address ?? '').trim();
  return text ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(text)}` : null;
}

/** Một mục kiểm tra hồ sơ; field là tên field trong form sửa để nhảy tới khi bấm "Bổ sung". */
export interface CompletenessCheck {
  key: string;
  field: string;
  ok: boolean;
}

/**
 * Hồ sơ doanh nghiệp đủ các thông tin khách hay tìm: địa chỉ mới, điện thoại, email, website, mã số thuế, ngành nghề,
 * đơn vị hành chính mới, giới thiệu.
 */
export function completenessChecks(listing: IListing, hasNewUnit: boolean): CompletenessCheck[] {
  const has = (value: string | null | undefined): boolean => !!value?.trim();
  return [
    { key: 'addressNew', field: 'addressNew', ok: has(listing.addressNew) },
    { key: 'phone', field: 'phone', ok: has(listing.phone) || has(listing.mobile) },
    { key: 'email', field: 'email', ok: splitEmails(listing.email).length > 0 },
    { key: 'website', field: 'website', ok: has(listing.website) },
    { key: 'taxCode', field: 'taxCode', ok: has(listing.taxCode) },
    { key: 'categories', field: 'categories', ok: (listing.categories?.length ?? 0) > 0 },
    { key: 'newUnit', field: 'locations', ok: hasNewUnit },
    { key: 'description', field: 'description', ok: has(listing.description?.replace(/<[^>]*>/g, '')) },
  ];
}

export function completenessPercent(checks: CompletenessCheck[]): number {
  return checks.length ? Math.round((checks.filter(check => check.ok).length / checks.length) * 100) : 100;
}
