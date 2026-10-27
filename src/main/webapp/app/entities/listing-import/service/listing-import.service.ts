import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Service, inject } from '@angular/core';

import { Observable, map } from 'rxjs';

import { serverApiUrl } from 'app/config';
import { IApiImportRequest, IImportBatch, IImportRow, IImportRowView, ImportAction, ImportRowStatus } from '../listing-import.model';

/** Sửa tay: gọi REST quản trị nhập doanh nghiệp (/api/admin/listing-imports, chỉ ROLE_ADMIN). */
@Service()
export class ListingImportService {
  protected readonly http = inject(HttpClient);
  protected readonly resourceUrl = `${serverApiUrl}api/admin/listing-imports`;

  /** Các đợt nhập, mới nhất trước (tổng số ở header X-Total-Count). */
  batches(page: number, size: number): Observable<HttpResponse<IImportBatch[]>> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', 'id,desc');
    return this.http.get<IImportBatch[]>(this.resourceUrl, { params, observe: 'response' });
  }

  batch(id: number): Observable<IImportBatch> {
    return this.http.get<IImportBatch>(`${this.resourceUrl}/${id}`);
  }

  /** Đợt đang chạy; null nếu không có (204). */
  running(): Observable<IImportBatch | null> {
    return this.http.get<IImportBatch>(`${this.resourceUrl}/running`, { observe: 'response' }).pipe(map(res => res.body ?? null));
  }

  rows(batchId: number, status: ImportRowStatus | null, page: number, size: number): Observable<HttpResponse<IImportRowView[]>> {
    let params = new HttpParams().set('page', page).set('size', size).set('sort', 'rowNo,asc');
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<IImportRowView[]>(`${this.resourceUrl}/${batchId}/rows`, { params, observe: 'response' });
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

  resolve(rowId: number, action: ImportAction, listingId?: number | null): Observable<IImportRow> {
    return this.http.post<IImportRow>(`${this.resourceUrl}/rows/${rowId}/resolve`, { action, listingId: listingId ?? null });
  }

  /** Xuất bản doanh nghiệp nháp do đợt tạo; trả số doanh nghiệp đã xuất bản. */
  publish(batchId: number): Observable<number> {
    return this.http.post<{ published: number }>(`${this.resourceUrl}/${batchId}/publish`, {}).pipe(map(res => res.published));
  }

  /** File CSV mẫu (cần token đăng nhập nên tải bằng HttpClient, không dùng link thường). */
  template(): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/template.csv`, { responseType: 'blob' });
  }
}
