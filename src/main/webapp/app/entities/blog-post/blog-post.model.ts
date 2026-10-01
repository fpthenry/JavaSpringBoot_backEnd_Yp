import dayjs from 'dayjs/esm';

export interface IBlogPost {
  id: number;
  wpId?: number | null;
  title?: string | null;
  slug?: string | null;
  content?: string | null;
  excerpt?: string | null;
  thumbnail?: string | null;
  status?: string | null;
  viewCount?: number | null;
  publishedAt?: dayjs.Dayjs | null;
  createdAt?: dayjs.Dayjs | null;
  updatedAt?: dayjs.Dayjs | null;
}

export type NewBlogPost = Omit<IBlogPost, 'id'> & { id: null };
