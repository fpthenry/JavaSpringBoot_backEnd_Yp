import dayjs from 'dayjs/esm';

import { IGallery } from 'app/entities/gallery/gallery.model';

export interface IGalleryImage {
  id: number;
  title?: string | null;
  image?: string | null;
  imageContentType?: string | null;
  imageUrl?: string | null;
  linkUrl?: string | null;
  altText?: string | null;
  displayOrder?: number | null;
  active?: boolean | null;
  startAt?: dayjs.Dayjs | null;
  endAt?: dayjs.Dayjs | null;
  openInNewTab?: boolean | null;
  gallery?: Pick<IGallery, 'id' | 'name'> | null;
}

export type NewGalleryImage = Omit<IGalleryImage, 'id'> & { id: null };
