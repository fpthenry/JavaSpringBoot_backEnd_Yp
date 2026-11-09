import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Service, inject } from '@angular/core';

import { Observable, map } from 'rxjs';

import { serverApiUrl } from 'app/config';
import {
  IApiImportRequest,
  IDirectusSummary,
  IFieldStat,
  IImportBatch,
  IImportRow,
  IImportRowView,
  IListingHistory,
  ImportAction,
  ImportRowStatus,
} from '../listing-import.model';

/** Sửa tay: gọi REST quản trị nhập doanh nghiệp (/api/admin/listing-imports, chỉ ROLE_ADMIN). */
@Service()
export class ListingImportService {
  protected readonly http = inject(HttpClient);
  protected readonly resourceUrl = `${serverApiUrl}api/admin/listing-imports`;

  /** Các đợt nhập, mới nhất trước (tổng số ở header X-Total-Count). */
  batches(page: number, size: number, dryRun?: boolean): Observable<HttpResponse<IImportBatch[]>> {
    let params = new HttpParams().set('page', page).set('size', size).set('sort', 'id,desc');
    if (dryRun !== undefined) {
      params = params.set('dryRun', dryRun);
    }
    return this.http.get<IImportBatch[]>(this.resourceUrl, { params, observe: 'response' });
  }

  batch(id: number): Observable<IImportBatch> {
    return this.http.get<IImportBatch>(`${this.resourceUrl}/${id}`);
  }

  /** Đợt đang chạy; null nếu không có (204). */
  running(): Observable<IImportBatch | null> {
    return this.http.get<IImportBatch>(`${this.resourceUrl}/running`, { observe: 'response' }).pipe(map(res => res.body ?? null));
  }

  rows(
    batchId: number,
    status: ImportRowStatus | null,
    page: number,
    size: number,
    q = '',
    field: string | null = null,
  ): Observable<HttpResponse<IImportRowView[]>> {
    const params = this.rowFilter(status, q, field).set('page', page).set('size', size).set('sort', 'rowNo,asc');
    return this.http.get<IImportRowView[]>(`${this.resourceUrl}/${batchId}/rows`, { params, observe: 'response' });
  }

  /** Số dòng đã đổi từng trường của đợt. */
  fieldStats(batchId: number): Observable<IFieldStat[]> {
    return this.http.get<IFieldStat[]>(`${this.resourceUrl}/${batchId}/field-stats`);
  }

  /** Các dòng đang lọc dạng CSV (cần token nên tải bằng HttpClient). */
  exportRows(batchId: number, status: ImportRowStatus | null, q: string, field: string | null): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/${batchId}/export`, { params: this.rowFilter(status, q, field), responseType: 'blob' });
  }

  /** Hoàn tác đợt nhập thật (chạy nền). */
  undo(batchId: number): Observable<IImportBatch> {
    return this.http.post<IImportBatch>(`${this.resourceUrl}/${batchId}/undo`, {});
  }

  /** Lịch sử nhập của một doanh nghiệp. */
  history(listingId: number): Observable<IListingHistory> {
    return this.http.get<IListingHistory>(`${this.resourceUrl}/history/${listingId}`);
  }

  private rowFilter(status: ImportRowStatus | null, q: string, field: string | null): HttpParams {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    if (q) {
      params = params.set('q', q);
    }
    if (field) {
      params = params.set('field', field);
    }
    return params;
  }

  /** Nhập từ file .xlsx / .csv (chạy nền, trả đợt vừa tạo). */
  importFile(file: File, dryRun: boolean): Observable<IImportBatch> {
    const body = new FormData();
    body.append('file', file, file.name);
    return this.http.post<IImportBatch>(`${this.resourceUrl}/file`, body, { params: new HttpParams().set('dryRun', dryRun) });
  }

  /** Nhập từ API JSON bên thứ ba (chạy nền). */
  importApi(request: IApiImportRequest): Observable<IImportBatch> {
    return this.http.post<IImportBatch>(`${this.resourceUrl}/api`, request);
  }

  /** Kéo dữ liệu API Trang Vàng về bảng bản gốc (chạy nền); token nằm ở backend. */
  startPull(resume: boolean): Observable<IDirectusSummary> {
    return this.http.post<IDirectusSummary>(`${this.resourceUrl}/directus/pull`, {}, { params: new HttpParams().set('resume', resume) });
  }

  /** Tiến độ kéo, số bản gốc đã lưu, số bản ghi chưa nhập. */
  pullSummary(): Observable<IDirectusSummary> {
    return this.http.get<IDirectusSummary>(`${this.resourceUrl}/directus/pull`);
  }

  /** Nhập từ bảng bản gốc vào doanh nghiệp (chạy nền). */
  importDirectus(dryRun: boolean): Observable<IImportBatch> {
    return this.http.post<IImportBatch>(`${this.resourceUrl}/directus`, {}, { params: new HttpParams().set('dryRun', dryRun) });
  }

  resolve(rowId: number, action: ImportAction, listingId?: number | null): Observable<IImportRow> {
    return this.http.post<IImportRow>(`${this.resourceUrl}/rows/${rowId}/resolve`, { action, listingId: listingId ?? null });
  }

  /** Xuất bản doanh nghiệp nháp do đợt tạo; trả số doanh nghiệp đã xuất bản. */
  publish(batchId: number): Observable<number> {
    return this.http.post<{ published: number }>(`${this.resourceUrl}/${batchId}/publish`, {}).pipe(map(res => res.published));
  }

  /** Hủy đợt đang chạy: backend dừng sau lô đang xử lý. */
  cancel(batchId: number): Observable<void> {
    return this.http.post<void>(`${this.resourceUrl}/${batchId}/cancel`, {});
  }

  /** Số doanh nghiệp nháp do đợt tạo mà chưa xuất bản. */
  drafts(batchId: number): Observable<number> {
    return this.http.get<{ drafts: number }>(`${this.resourceUrl}/${batchId}/drafts`).pipe(map(res => res.drafts));
  }

  /** File CSV mẫu (cần token đăng nhập nên tải bằng HttpClient, không dùng link thường). */
  template(): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/template.csv`, { responseType: 'blob' });
  }
}
