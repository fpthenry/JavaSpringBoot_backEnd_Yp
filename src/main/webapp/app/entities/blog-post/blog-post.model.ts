import dayjs from 'dayjs/esm';

import { IBlogCategory } from 'app/entities/blog-category/blog-category.model';
import { ITag } from 'app/entities/tag/tag.model';

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
  authorName?: string | null;
  categories?: Pick<IBlogCategory, 'id' | 'name'>[] | null;
  tags?: Pick<ITag, 'id' | 'name'>[] | null;
}

export type NewBlogPost = Omit<IBlogPost, 'id'> & { id: null };
