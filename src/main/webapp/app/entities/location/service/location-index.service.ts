import { HttpClient, HttpParams } from '@angular/common/http';
import { Service, inject } from '@angular/core';

import { Observable } from 'rxjs';

import { serverApiUrl } from 'app/config';

/** Một chữ cái trong mục lục và số đơn vị hành chính bắt đầu bằng chữ đó (Đ là chữ riêng). */
export interface LocationLetter {
  letter: string;
  count: number;
}

/** Địa giới: new (từ 01/7/2025, mặc định), old, all. */
export type LocationGeneration = 'new' | 'old' | 'all';

export type LocationLevel = 'province' | 'district' | 'ward';

/** Một đơn vị trong mục lục: kèm đơn vị cha, tỉnh (phân biệt xã trùng tên) và số doanh nghiệp (gồm đơn vị con). */
export interface LocationIndexItem {
  id: number;
  name: string;
  slug: string | null;
  type: LocationLevel | null;
  isNew: boolean;
  letter: string;
  parentId: number | null;
  parentName: string | null;
  provinceName: string | null;
  listingCount: number;
}

export interface LocationIndexPage {
  items: LocationIndexItem[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface LocationIndexFilter {
  generation?: LocationGeneration;
  level?: LocationLevel | null;
  hideEmpty?: boolean;
}

export interface LocationIndexQuery extends LocationIndexFilter {
  letter?: string | null;
  q?: string | null;
  /** Bắt đầu từ 0. */
  page?: number;
  size?: number;
  /** name,asc (mặc định) hoặc listingCount,desc. */
  sort?: string;
}

/** Mục lục tỉnh thành theo chữ cái (API quản trị /api/location-index). */
@Service()
export class LocationIndexService {
  protected readonly http = inject(HttpClient);
  protected readonly resourceUrl = `${serverApiUrl}api/location-index`;

  letters(filter: LocationIndexFilter): Observable<LocationLetter[]> {
    return this.http.get<LocationLetter[]>(`${this.resourceUrl}/letters`, { params: toParams(filter) });
  }

  search(query: LocationIndexQuery): Observable<LocationIndexPage> {
    return this.http.get<LocationIndexPage>(this.resourceUrl, { params: toParams(query) });
  }
}

function toParams(values: object): HttpParams {
  let params = new HttpParams();
  for (const [key, value] of Object.entries(values)) {
    if (value !== null && value !== undefined && value !== '') {
      params = params.set(key, String(value));
    }
  }
  return params;
}
