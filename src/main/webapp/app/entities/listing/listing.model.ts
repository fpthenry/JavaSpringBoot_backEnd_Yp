import dayjs from 'dayjs/esm';

import { ICategory } from 'app/entities/category/category.model';
import { ILocation } from 'app/entities/location/location.model';
import { IUser } from 'app/entities/user/user.model';

export interface IListing {
  id: number;
  title?: string | null;
  slug?: string | null;
  content?: string | null;
  status?: string | null;
  email?: string | null;
  telephone?: string | null;
  mobile?: string | null;
  website?: string | null;
  fax?: string | null;
  taxCode?: string | null;
  nameAlias?: string | null;
  nameEn?: string | null;
  representative?: string | null;
  mainIndustry?: string | null;
  managedBy?: string | null;
  businessType?: string | null;
  statusYp?: string | null;
  foundedDate?: dayjs.Dayjs | null;
  licenseModifiedDate?: dayjs.Dayjs | null;
  address?: string | null;
  addressAlternative?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  apiId?: number | null;
  createdAt?: dayjs.Dayjs | null;
  updatedAt?: dayjs.Dayjs | null;
  author?: Pick<IUser, 'id' | 'login'> | null;
  categorieses?: Pick<ICategory, 'id' | 'name'>[] | null;
  locationses?: Pick<ILocation, 'id' | 'name'>[] | null;
}

export type NewListing = Omit<IListing, 'id'> & { id: null };
