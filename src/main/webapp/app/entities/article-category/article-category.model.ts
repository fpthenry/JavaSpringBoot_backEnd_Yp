import { IArticle } from 'app/entities/article/article.model';

export interface IArticleCategory {
  id: number;
  name?: string | null;
  slug?: string | null;
  description?: string | null;
  count?: number | null;
  parent?: Pick<IArticleCategory, 'id' | 'name'> | null;
  articleses?: Pick<IArticle, 'id'>[] | null;
}

export type NewArticleCategory = Omit<IArticleCategory, 'id'> & { id: null };
