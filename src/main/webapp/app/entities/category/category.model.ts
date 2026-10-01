import dayjs from 'dayjs/esm';

import { IListing } from 'app/entities/listing/listing.model';

export interface ICategory {
  id: number;
  wpTermId?: number | null;
  name?: string | null;
  slug?: string | null;
  description?: string | null;
  icon?: string | null;
  listingCount?: number | null;
  createdAt?: dayjs.Dayjs | null;
  updatedAt?: dayjs.Dayjs | null;
  parent?: Pick<ICategory, 'id' | 'name'> | null;
  listings?: Pick<IListing, 'id'>[] | null;
}

export type NewCategory = Omit<ICategory, 'id'> & { id: null };
