/** Từ chỉ loại hình, bỏ qua khi lấy chữ viết tắt: "CÔNG TY TNHH ABC" → "AB" thay vì "CT". */
const SKIP_WORDS = new Set([
  'cong',
  'ty',
  'cty',
  'tnhh',
  'cp',
  'co',
  'phan',
  'tm',
  'dv',
  'xd',
  'mtv',
  'ch',
  'cs',
  'dntn',
  'tcty',
  'chi',
  'nhanh',
  'cn',
  'hkd',
  'ho',
  'kinh',
  'doanh',
  'van',
  'phong',
  'vp',
  'dai',
  'dien',
]);

const fold = (word: string): string => word.normalize('NFD').replace(/\p{M}/gu, '').replace(/đ/gi, 'd').toLowerCase();

/** Hai chữ cái đại diện cho ô ảnh đại diện: "CTY TNHH TM ĐẦU TƯ HÓA DẦU" → "ĐT"; tên toàn từ loại hình thì lấy 2 từ đầu. */
export function initials(name: string | null | undefined): string {
  const words = (name ?? '').split(/[\s\-–.,&()/]+/).filter(word => /\p{L}|\d/u.test(word));
  const meaningful = words.filter(word => !SKIP_WORDS.has(fold(word)));
  const picked = (meaningful.length > 0 ? meaningful : words).slice(0, 2);
  return picked.map(word => [...word][0].toUpperCase()).join('') || '?';
}

/** Màu nền ô viết tắt ổn định theo tên (cùng tên luôn cùng màu). */
export function avatarHue(name: string | null | undefined): number {
  let hash = 0;
  for (const char of name ?? '') {
    hash = (hash * 31 + char.charCodeAt(0)) % 360;
  }
  return hash;
}
