// Sửa tay: nhập doanh nghiệp từ nguồn ngoài (REST /api/admin/listing-imports, xem ListingImportResource.java). Không qua JDL.

/** Kết quả xử lý một dòng (enum ListingImportRow.Status). Ở đợt chạy thử: kết quả SẼ xảy ra. */
export type ImportRowStatus = 'CREATED' | 'MERGED' | 'UNCHANGED' | 'SUSPECT' | 'DUPLICATE_IN_BATCH' | 'INVALID' | 'SKIPPED';

/** Căn cứ khớp với doanh nghiệp đã có (enum ListingImportRow.MatchReason). */
export type ImportMatchReason = 'TAX_CODE' | 'SOURCE_REF' | 'NAME_PHONE' | 'NAME_ADDRESS' | 'NAME';

/** Quyết định cho dòng nghi trùng. */
export type ImportAction = 'MERGE' | 'CREATE' | 'SKIP';

export interface IImportBatch {
  id: number;
  /** file | api:<mã> | json:<mã> */
  source: string;
  /** Tên file hoặc URL */
  sourceName?: string | null;
  dryRun: boolean;
  status: 'RUNNING' | 'DONE' | 'FAILED' | string;
  totalRows: number;
  createdCount: number;
  mergedCount: number;
  unchangedCount: number;
  suspectCount: number;
  duplicateInBatchCount: number;
  invalidCount: number;
  error?: string | null;
  createdBy?: string | null;
  startedAt?: string | null;
  finishedAt?: string | null;
  undoneAt?: string | null;
  undoneBy?: string | null;
  /** Số trường giữ nguyên khi hoàn tác vì đã đổi sau đợt này */
  undoSkipped?: number | null;
}

/** Cách lấy dữ liệu (DirectusPullService.Mode): NEW chỉ phần thay đổi, FULL cả bảng, RESUME đọc tiếp khi lỗi. */
export type PullMode = 'NEW' | 'FULL' | 'RESUME';

/** Tình trạng bảng bản gốc API Trang Vàng (GET /directus/pull). */
export interface IDirectusSummary {
  /** Lần kéo gần nhất từ khi backend khởi động; null nếu chưa kéo */
  /** Lần lấy gần nhất (lưu ở DB, còn sau khi backend khởi động lại); null nếu chưa lấy lần nào */
  pull?: {
    id: number;
    state: 'RUNNING' | 'DONE' | 'FAILED' | 'CANCELLED' | 'INTERRUPTED';
    read: number;
    changed: number;
    lastId: number;
    error?: string | null;
    mode: PullMode;
    startedAt?: string | null;
    finishedAt?: string | null;
  } | null;
  stored: number;
  /** Bản ghi mới hoặc đã đổi so với lần nhập thật gần nhất */
  pending: number;
  lastFetchedAt?: string | null;
  /** Lần lấy toàn bộ gần nhất nếu nó dừng giữa chừng (lấy tiếp được) */
  unfinishedFull?: IDirectusSummary['pull'];
}

export interface IImportRow {
  id: number;
  batchId: number;
  rowNo: number;
  sourceRef?: string | null;
  name?: string | null;
  taxCode?: string | null;
  phone?: string | null;
  status: ImportRowStatus;
  matchReason?: ImportMatchReason | null;
  matchScore?: number | null;
  matchedListingId?: number | null;
  /** Doanh nghiệp được tạo / bổ sung (đợt chạy thật) */
  listingId?: number | null;
  /** Các trường đã đổi, cách nhau bởi dấu phẩy */
  filledFields?: string | null;
  /** Trong đó các trường bị ghi đè */
  updatedFields?: string | null;
  /** Ngày cập nhật của bản nguồn */
  sourceUpdatedAt?: string | null;
  /** Ghi chú: không nhận ra tỉnh/ngành, tọa độ sai… (cách nhau bởi "; ") */
  message?: string | null;
  resolvedBy?: string | null;
  resolvedAt?: string | null;
}

/** Doanh nghiệp đã có bị khớp / nghi trùng (để so cạnh dòng nhập). */
export interface IMatchedListing {
  id: number;
  name?: string | null;
  slug?: string | null;
  taxCode?: string | null;
  phone?: string | null;
  mobile?: string | null;
  email?: string | null;
  website?: string | null;
  address?: string | null;
  addressNew?: string | null;
  representative?: string | null;
  status?: string | null;
  source?: string | null;
}

/** Một trường đã đổi; updated: ghi đè giá trị đang có, không thì điền ô trống. */
export interface IFieldChange {
  field: string;
  oldValue?: string | null;
  newValue?: string | null;
  updated: boolean;
}

/** Những gì một dòng nhập đã đổi ở doanh nghiệp (ListingChanges.java). */
export interface IListingChanges {
  fields: IFieldChange[];
  categories: number[];
  locations: number[];
}

/** Một dòng kèm dữ liệu gốc (field chuẩn → giá trị) và doanh nghiệp bị khớp. */
export interface IImportRowView {
  row: IImportRow;
  data?: Record<string, string> | null;
  matched?: IMatchedListing | null;
  changes?: IListingChanges | null;
}

/** Số dòng đã đổi một trường. */
export interface IFieldStat {
  field: string;
  filled: number;
  updated: number;
}

export interface IHistoryEntry {
  rowId: number;
  batchId: number;
  source: string;
  importedAt?: string | null;
  sourceRef?: string | null;
  sourceUpdatedAt?: string | null;
  status: ImportRowStatus;
  undone: boolean;
  changes?: IListingChanges | null;
}

/** Lịch sử nhập của một doanh nghiệp và các bản nguồn đang nối. */
export interface IListingHistory {
  entries: IHistoryEntry[];
  sources: { source: string; sourceId: string; sourceUpdatedAt?: string | null; lastFetchedAt?: string | null }[];
}

/** Nhập từ API JSON của bên thứ ba (ApiRecordSource.Request). */
export interface IApiImportRequest {
  source: string;
  url: string;
  headers?: Record<string, string>;
  itemsPath?: string;
  pageParam?: string;
  firstPage?: number;
  maxPages?: number;
  delayMs?: number;
  fieldMapping?: Record<string, string>;
  dryRun: boolean;
}
