import dayjs from 'dayjs/esm';

export interface ICachedContent {
  id: number;
  termSlug?: string | null;
  expired?: dayjs.Dayjs | null;
  content?: string | null;
}

export type NewCachedContent = Omit<ICachedContent, 'id'> & { id: null };
