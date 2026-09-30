import { IArticle } from 'app/entities/article/article.model';

export interface IArticleTag {
  id: number;
  name?: string | null;
  slug?: string | null;
  count?: number | null;
  articleses?: Pick<IArticle, 'id'>[] | null;
}

export type NewArticleTag = Omit<IArticleTag, 'id'> & { id: null };
