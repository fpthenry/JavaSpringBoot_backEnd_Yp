import dayjs from 'dayjs/esm';

import { IListing } from 'app/entities/listing/listing.model';

export interface ICategory {
  id: number;
  name?: string | null;
  slug?: string | null;
  description?: string | null;
  count?: number | null;
  industryCode?: string | null;
  displayOrder?: number | null;
  createdAt?: dayjs.Dayjs | null;
  updatedAt?: dayjs.Dayjs | null;
  parent?: Pick<ICategory, 'id' | 'name'> | null;
  listingses?: Pick<IListing, 'id'>[] | null;
}

export type NewCategory = Omit<ICategory, 'id'> & { id: null };
