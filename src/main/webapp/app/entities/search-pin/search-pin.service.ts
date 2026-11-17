import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';

import { Observable } from 'rxjs';

import { serverApiUrl } from 'app/config';

/** Một doanh nghiệp đang được ghim cho từ khóa. */
export interface PinItem {
  id: number;
  listingId: number;
  name?: string | null;
  slug?: string | null;
  taxCode?: string | null;
  status?: string | null;
  position: number;
}

/** Một từ khóa và các doanh nghiệp ghim theo thứ tự. `key` = từ khóa bỏ dấu, chữ thường (dùng để so khớp). */
export interface PinGroup {
  keyword: string;
  key: string;
  items: PinItem[];
}

/** Sửa tay (2026-11-17): ghim doanh nghiệp lên đầu kết quả tìm kiếm theo từ khóa (/api/search-pins). */
@Service()
export class SearchPinService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = `${serverApiUrl}api/search-pins`;

  groups(): Observable<PinGroup[]> {
    return this.http.get<PinGroup[]>(this.resourceUrl);
  }

  add(keyword: string, listingId: number): Observable<PinGroup> {
    return this.http.post<PinGroup>(this.resourceUrl, { keyword, listingId });
  }

  reorder(pinIds: number[]): Observable<PinGroup> {
    return this.http.put<PinGroup>(`${this.resourceUrl}/order`, { pinIds });
  }

  remove(id: number): Observable<undefined> {
    return this.http.delete<undefined>(`${this.resourceUrl}/${id}`);
  }

  removeGroup(keyword: string): Observable<undefined> {
    return this.http.delete<undefined>(this.resourceUrl, { params: { keyword } });
  }
}

/** Khóa so khớp giống backend (SearchPinService.key): bỏ dấu, chữ thường, chỉ giữ chữ và số. */
export function pinKey(text: string | null | undefined): string {
  return (text ?? '')
    .normalize('NFD')
    .replace(/\p{M}+/gu, '')
    .replace(/[đĐ]/g, 'd')
    .toLowerCase()
    .split(/[^\p{L}\p{N}]+/u)
    .filter(Boolean)
    .join(' ');
}
