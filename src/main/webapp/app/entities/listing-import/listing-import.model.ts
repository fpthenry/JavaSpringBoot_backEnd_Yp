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
  /** Các trường đã bổ sung, cách nhau bởi dấu phẩy */
  filledFields?: string | null;
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

/** Một dòng kèm dữ liệu gốc (field chuẩn → giá trị) và doanh nghiệp bị khớp. */
export interface IImportRowView {
  row: IImportRow;
  data?: Record<string, string> | null;
  matched?: IMatchedListing | null;
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
