// Sửa tay: hàm thuần hiển thị đợt nhập / dòng nhập (nhãn, màu, so sánh dòng nhập với doanh nghiệp đã có). Có spec.
import { IImportBatch, IImportRowView, IMatchedListing, ImportMatchReason, ImportRowStatus } from './listing-import.model';

/** Thứ tự hiện các trạng thái (bộ đếm, bộ lọc). */
export const ROW_STATUSES: ImportRowStatus[] = ['SUSPECT', 'CREATED', 'MERGED', 'UNCHANGED', 'DUPLICATE_IN_BATCH', 'INVALID', 'SKIPPED'];

/** Màu nhãn Bootstrap theo trạng thái dòng. */
export const STATUS_BADGE: Record<ImportRowStatus, string> = {
  SUSPECT: 'bg-warning text-dark',
  CREATED: 'bg-success',
  MERGED: 'bg-primary',
  UNCHANGED: 'bg-secondary',
  DUPLICATE_IN_BATCH: 'bg-light text-dark border',
  INVALID: 'bg-danger',
  SKIPPED: 'bg-light text-muted border',
};

/** Số dòng của đợt theo trạng thái (SKIPPED không có bộ đếm riêng ở đợt). */
export function statusCount(batch: IImportBatch, status: ImportRowStatus): number | null {
  switch (status) {
    case 'CREATED':
      return batch.createdCount;
    case 'MERGED':
      return batch.mergedCount;
    case 'UNCHANGED':
      return batch.unchangedCount;
    case 'SUSPECT':
      return batch.suspectCount;
    case 'DUPLICATE_IN_BATCH':
      return batch.duplicateInBatchCount;
    case 'INVALID':
      return batch.invalidCount;
    default:
      return null;
  }
}

/** Khóa dịch nhãn trạng thái dòng; đợt chạy thử dùng thì tương lai ("Sẽ tạo mới"). */
export function statusLabelKey(status: ImportRowStatus, dryRun: boolean): string {
  const base = `javaSpringBootBackEndApp.listingImport.status.${status}`;
  return dryRun && (status === 'CREATED' || status === 'MERGED') ? `${base}_DRY` : base;
}

export function reasonLabelKey(reason: ImportMatchReason): string {
  return `javaSpringBootBackEndApp.listingImport.reason.${reason}`;
}

/** Nguồn đợt: "file" → File; "api:masothue" → API · masothue. */
export function sourceKind(source: string): 'file' | 'api' | 'json' {
  if (source.startsWith('api:')) {
    return 'api';
  }
  return source.startsWith('json:') ? 'json' : 'file';
}

export function sourceCode(source: string): string | null {
  const i = source.indexOf(':');
  return i >= 0 ? source.slice(i + 1) : null;
}

/** "taxCode,addressNew,images" → ["taxCode", "addressNew", "images"]. */
export function filledList(filled: string | null | undefined): string[] {
  return (filled ?? '')
    .split(',')
    .map(s => s.trim())
    .filter(Boolean);
}

/** Ghi chú "a; b" → ["a", "b"]. */
export function messageList(message: string | null | undefined): string[] {
  return (message ?? '')
    .split('; ')
    .map(s => s.trim())
    .filter(Boolean);
}

/** Phần trăm điểm giống nhau: 0.7 → "70%". */
export function scorePercent(score: number | null | undefined): string {
  return score == null ? '' : `${Math.round(score * 100)}%`;
}

/** Thời gian chạy một đợt: "12 giây", "3 phút 05 giây"; đang chạy thì tính tới bây giờ. */
export function durationText(batch: Pick<IImportBatch, 'startedAt' | 'finishedAt'>, now = Date.now()): string {
  if (!batch.startedAt) {
    return '';
  }
  const end = batch.finishedAt ? new Date(batch.finishedAt).getTime() : now;
  const seconds = Math.max(0, Math.round((end - new Date(batch.startedAt).getTime()) / 1000));
  if (seconds < 60) {
    return `${seconds} giây`;
  }
  const m = Math.floor(seconds / 60);
  return `${m} phút ${String(seconds % 60).padStart(2, '0')} giây`;
}

/** Một dòng của bảng so sánh: field, giá trị dòng nhập, giá trị doanh nghiệp đã có, giống/khác. */
export interface CompareLine {
  field: string;
  imported: string | null;
  existing: string | null;
  /** same: giống (bỏ dấu cách, hoa thường, ký tự ngoài chữ/số); diff: khác; fill: dòng nhập sẽ bổ sung ô trống; none */
  state: 'same' | 'diff' | 'fill' | 'none';
}

/** Các field so sánh (theo thứ tự hiện) — field dòng nhập ↔ field doanh nghiệp đã có. */
export const COMPARE_FIELDS: { field: string; matched: keyof IMatchedListing }[] = [
  { field: 'name', matched: 'name' },
  { field: 'taxCode', matched: 'taxCode' },
  { field: 'phone', matched: 'phone' },
  { field: 'mobile', matched: 'mobile' },
  { field: 'email', matched: 'email' },
  { field: 'website', matched: 'website' },
  { field: 'address', matched: 'address' },
  { field: 'addressNew', matched: 'addressNew' },
  { field: 'representative', matched: 'representative' },
];

const compareKey = (v: string): string =>
  v
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/đ/gi, 'd')
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '');

/** Bảng so sánh dòng nhập với doanh nghiệp nghi trùng; bỏ field cả hai bên đều trống. */
export function compareLines(view: IImportRowView): CompareLine[] {
  const data = view.data ?? {};
  const matched = view.matched;
  return COMPARE_FIELDS.map(({ field, matched: key }) => {
    const imported = (data[field] ?? '').trim() || null;
    const existingRaw = matched?.[key];
    const existing = existingRaw == null ? null : String(existingRaw).trim() || null;
    let state: CompareLine['state'] = 'none';
    if (imported && existing) {
      state = compareKey(imported) === compareKey(existing) ? 'same' : 'diff';
    } else if (imported && !existing) {
      state = 'fill';
    }
    return { field, imported, existing, state };
  }).filter(l => l.imported !== null || l.existing !== null);
}

/** Biến danh sách dòng "Khóa: giá trị" (header) hoặc "field = đường dẫn" (fieldMapping) thành object; dòng sai bị bỏ. */
export function parsePairs(text: string, separator: ':' | '='): Record<string, string> {
  const out: Record<string, string> = {};
  for (const line of text.split(/\r?\n/)) {
    const i = line.indexOf(separator);
    if (i <= 0) {
      continue;
    }
    const k = line.slice(0, i).trim();
    const v = line.slice(i + 1).trim();
    if (k && v) {
      out[k] = v;
    }
  }
  return out;
}
