import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';

import { Observable } from 'rxjs';

import { serverApiUrl } from 'app/config';

/** Trạng thái hiệu ứng website (GET /api/site-settings/effects). */
export interface SiteEffects {
  tetEffect: boolean;
  updatedAt?: string | null;
  updatedBy?: string | null;
}

/** Sửa tay (2026-11-17): bật/tắt hiệu ứng trang trí trên website yp.vn (hiện có: Tết hoa đào). */
@Service()
export class SiteEffectService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = `${serverApiUrl}api/site-settings/effects`;

  get(): Observable<SiteEffects> {
    return this.http.get<SiteEffects>(this.resourceUrl);
  }

  setTet(enabled: boolean): Observable<SiteEffects> {
    return this.http.put<SiteEffects>(`${this.resourceUrl}/tet`, { enabled });
  }
}
