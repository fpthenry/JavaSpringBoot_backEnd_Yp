import dayjs from 'dayjs/esm';

import { IListing } from 'app/entities/listing/listing.model';

/** Sửa tay: ngành nghề theo VSIC 2025, có mã ngành (code) và cấp (level); bỏ wpTermId, icon. */
export interface ICategory {
  id: number;
  /** Mã ngành VSIC 2025: A–V (cấp 1) hoặc 2–5 chữ số (cấp 2–5). */
  code?: string | null;
  /** Cấp 1–5, máy chủ suy ra từ mã khi lưu. */
  level?: number | null;
  name?: string | null;
  slug?: string | null;
  description?: string | null;
  listingCount?: number | null;
  createdAt?: dayjs.Dayjs | null;
  updatedAt?: dayjs.Dayjs | null;
  parent?: Pick<ICategory, 'id' | 'name' | 'code'> | null;
  listings?: Pick<IListing, 'id'>[] | null;
}

export type NewCategory = Omit<ICategory, 'id'> & { id: null };
