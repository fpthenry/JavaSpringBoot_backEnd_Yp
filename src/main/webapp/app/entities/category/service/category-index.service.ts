import { HttpClient, HttpParams } from '@angular/common/http';
import { Service, inject } from '@angular/core';

import { Observable } from 'rxjs';

import { serverApiUrl } from 'app/config';

/** Một chữ cái trong mục lục và số ngành bắt đầu bằng chữ đó. */
export interface CategoryLetter {
  letter: string;
  count: number;
}

/** Một ngành trong mục lục: kèm tên ngành cha (phân biệt ngành trùng tên) và số doanh nghiệp (gồm ngành con). */
export interface CategoryIndexItem {
  id: number;
  name: string;
  slug: string | null;
  letter: string;
  parentId: number | null;
  parentName: string | null;
  listingCount: number;
}

export interface CategoryIndexPage {
  items: CategoryIndexItem[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface CategoryIndexQuery {
  letter?: string | null;
  q?: string | null;
  hideEmpty?: boolean;
  /** Bắt đầu từ 0. */
  page?: number;
  size?: number;
  /** name,asc (mặc định) hoặc listingCount,desc. */
  sort?: string;
}

/** Mục lục ngành nghề theo chữ cái (API quản trị /api/category-index). */
@Service()
export class CategoryIndexService {
  protected readonly http = inject(HttpClient);
  protected readonly resourceUrl = `${serverApiUrl}api/category-index`;

  letters(hideEmpty: boolean): Observable<CategoryLetter[]> {
    return this.http.get<CategoryLetter[]>(`${this.resourceUrl}/letters`, { params: { hideEmpty } });
  }

  search(query: CategoryIndexQuery): Observable<CategoryIndexPage> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(query)) {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<CategoryIndexPage>(this.resourceUrl, { params });
  }
}
