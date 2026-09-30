import dayjs from 'dayjs/esm';

import { IArticleCategory } from 'app/entities/article-category/article-category.model';
import { IArticleTag } from 'app/entities/article-tag/article-tag.model';
import { IUser } from 'app/entities/user/user.model';

export interface IArticle {
  id: number;
  title?: string | null;
  slug?: string | null;
  excerpt?: string | null;
  content?: string | null;
  status?: string | null;
  featuredImageUrl?: string | null;
  createdAt?: dayjs.Dayjs | null;
  updatedAt?: dayjs.Dayjs | null;
  author?: Pick<IUser, 'id' | 'login'> | null;
  categorieses?: Pick<IArticleCategory, 'id' | 'name'>[] | null;
  tagses?: Pick<IArticleTag, 'id' | 'name'>[] | null;
}

export type NewArticle = Omit<IArticle, 'id'> & { id: null };
