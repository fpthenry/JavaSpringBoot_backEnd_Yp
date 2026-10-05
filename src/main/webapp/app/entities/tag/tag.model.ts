import { IBlogPost } from 'app/entities/blog-post/blog-post.model';

export interface ITag {
  id: number;
  wpTermId?: number | null;
  name?: string | null;
  slug?: string | null;
  blogPosts?: Pick<IBlogPost, 'id'>[] | null;
}

export type NewTag = Omit<ITag, 'id'> & { id: null };
