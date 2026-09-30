import dayjs from 'dayjs/esm';

import { IGallery } from 'app/entities/gallery/gallery.model';
import { IListing } from 'app/entities/listing/listing.model';

export interface IListingImage {
  id: number;
  imageUrl?: string | null;
  thumbnailUrl?: string | null;
  altText?: string | null;
  displayOrder?: number | null;
  isFeatured?: boolean | null;
  createdAt?: dayjs.Dayjs | null;
  updatedAt?: dayjs.Dayjs | null;
  listing?: Pick<IListing, 'id' | 'title'> | null;
  gallery?: Pick<IGallery, 'id' | 'title'> | null;
}

export type NewListingImage = Omit<IListingImage, 'id'> & { id: null };
