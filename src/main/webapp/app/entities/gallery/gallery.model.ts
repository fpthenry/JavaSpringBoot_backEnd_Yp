import dayjs from 'dayjs/esm';

import { IListing } from 'app/entities/listing/listing.model';
import { IUser } from 'app/entities/user/user.model';

export interface IGallery {
  id: number;
  title?: string | null;
  slug?: string | null;
  content?: string | null;
  status?: string | null;
  createdAt?: dayjs.Dayjs | null;
  updatedAt?: dayjs.Dayjs | null;
  author?: Pick<IUser, 'id' | 'login'> | null;
  listing?: Pick<IListing, 'id' | 'title'> | null;
}

export type NewGallery = Omit<IGallery, 'id'> & { id: null };
