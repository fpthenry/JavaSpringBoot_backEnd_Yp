import dayjs from 'dayjs/esm';

import { ICategory } from 'app/entities/category/category.model';
import { ILocation } from 'app/entities/location/location.model';

export interface IListing {
  id: number;
  wpId?: number | null;
  apiId?: string | null;
  name?: string | null;
  nameEn?: string | null;
  nameAlias?: string | null;
  slug?: string | null;
  description?: string | null;
  phone?: string | null;
  mobile?: string | null;
  email?: string | null;
  website?: string | null;
  address?: string | null;
  locationJson?: string | null;
  taxCode?: string | null;
  representative?: string | null;
  capital?: string | null;
  foundedYear?: string | null;
  businessType?: string | null;
  businessStatus?: string | null;
  industryCode?: string | null;
  managedBy?: string | null;
  thumbnail?: string | null;
  images?: string | null;
  viewCount?: number | null;
  isFeatured?: boolean | null;
  status?: string | null;
  publishedAt?: dayjs.Dayjs | null;
  modifiedAt?: dayjs.Dayjs | null;
  createdAt?: dayjs.Dayjs | null;
  updatedAt?: dayjs.Dayjs | null;
  esIndexed?: boolean | null;
  categories?: Pick<ICategory, 'id' | 'name'>[] | null;
  locations?: Pick<ILocation, 'id' | 'name'>[] | null;
}

export type NewListing = Omit<IListing, 'id'> & { id: null };
