import { IBlogPost } from 'app/entities/blog-post/blog-post.model';

export interface IBlogCategory {
  id: number;
  wpTermId?: number | null;
  name?: string | null;
  slug?: string | null;
  description?: string | null;
  postCount?: number | null;
  parent?: Pick<IBlogCategory, 'id' | 'name'> | null;
  blogPosts?: Pick<IBlogPost, 'id'>[] | null;
}

export type NewBlogCategory = Omit<IBlogCategory, 'id'> & { id: null };
