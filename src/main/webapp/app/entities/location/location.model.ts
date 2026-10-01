import dayjs from 'dayjs/esm';

import { IListing } from 'app/entities/listing/listing.model';

export interface ILocation {
  id: number;
  wpTermId?: number | null;
  name?: string | null;
  slug?: string | null;
  type?: string | null;
  code?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  createdAt?: dayjs.Dayjs | null;
  parent?: Pick<ILocation, 'id' | 'name'> | null;
  listings?: Pick<IListing, 'id'>[] | null;
}

export type NewLocation = Omit<ILocation, 'id'> & { id: null };
