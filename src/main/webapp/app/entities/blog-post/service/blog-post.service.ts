import { HttpClient, HttpResponse, httpResource } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';

import dayjs from 'dayjs/esm';
import { Observable, asapScheduler, catchError, map, scheduled } from 'rxjs';

import { serverApiUrl } from 'app/config';
import { SearchWithPagination, createRequestOption } from 'app/core/request';
import { IBlogPost, NewBlogPost } from '../blog-post.model';

export type PartialUpdateBlogPost = Partial<IBlogPost> & Pick<IBlogPost, 'id'>;

type RestOf<T extends IBlogPost | NewBlogPost> = Omit<T, 'publishedAt' | 'createdAt' | 'updatedAt'> & {
  publishedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type RestBlogPost = RestOf<IBlogPost>;

export type NewRestBlogPost = RestOf<NewBlogPost>;

export type PartialUpdateRestBlogPost = RestOf<PartialUpdateBlogPost>;

@Service()
export class BlogPostsService {
  readonly blogPostsParams = signal<Record<string, string | number | boolean | readonly (string | number | boolean)[]> | undefined>(
    undefined,
  );
  readonly blogPostsResource = httpResource<RestBlogPost[]>(() => {
    const params = this.blogPostsParams();
    if (!params) {
      return undefined;
    }
    return { url: params.query ? this.resourceSearchUrl : this.resourceUrl, params };
  });
  /**
   * This signal holds the list of blogPost that have been fetched. It is updated when the blogPostsResource emits a new value.
   * In case of error while fetching the blogPosts, the signal is set to an empty array.
   */
  readonly blogPosts = computed(() =>
    (this.blogPostsResource.hasValue() ? this.blogPostsResource.value() : []).map(item => this.convertValueFromServer(item)),
  );
  protected readonly resourceUrl = `${serverApiUrl}api/blog-posts`;
  protected readonly resourceSearchUrl = `${serverApiUrl}api/blog-posts/_search`;

  protected convertValueFromServer(restBlogPost: RestBlogPost): IBlogPost {
    return {
      ...restBlogPost,
      publishedAt: restBlogPost.publishedAt ? dayjs(restBlogPost.publishedAt) : undefined,
      createdAt: restBlogPost.createdAt ? dayjs(restBlogPost.createdAt) : undefined,
      updatedAt: restBlogPost.updatedAt ? dayjs(restBlogPost.updatedAt) : undefined,
    };
  }
}

@Service()
export class BlogPostService extends BlogPostsService {
  protected readonly http = inject(HttpClient);

  create(blogPost: NewBlogPost): Observable<IBlogPost> {
    const copy = this.convertValueFromClient(blogPost);
    return this.http.post<RestBlogPost>(this.resourceUrl, copy).pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(blogPost: IBlogPost): Observable<IBlogPost> {
    const copy = this.convertValueFromClient(blogPost);
    return this.http
      .put<RestBlogPost>(`${this.resourceUrl}/${encodeURIComponent(this.getBlogPostIdentifier(blogPost))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(blogPost: PartialUpdateBlogPost): Observable<IBlogPost> {
    const copy = this.convertValueFromClient(blogPost);
    return this.http
      .patch<RestBlogPost>(`${this.resourceUrl}/${encodeURIComponent(this.getBlogPostIdentifier(blogPost))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<IBlogPost> {
    return this.http
      .get<RestBlogPost>(`${this.resourceUrl}/${encodeURIComponent(id)}`)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<HttpResponse<IBlogPost[]>> {
    const options = createRequestOption(req);
    return this.http
      .get<RestBlogPost[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => res.clone({ body: this.convertResponseArrayFromServer(res.body!) })));
  }

  delete(id: number): Observable<undefined> {
    return this.http.delete<undefined>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  search(req: SearchWithPagination): Observable<IBlogPost[]> {
    const options = createRequestOption(req);
    return this.http.get<RestBlogPost[]>(this.resourceSearchUrl, { params: options }).pipe(
      map(res => this.convertResponseArrayFromServer(res)),
      catchError(() => scheduled([], asapScheduler)),
    );
  }

  getBlogPostIdentifier(blogPost: Pick<IBlogPost, 'id'>): number {
    return blogPost.id;
  }

  compareBlogPost(o1: Pick<IBlogPost, 'id'> | null, o2: Pick<IBlogPost, 'id'> | null): boolean {
    return o1 && o2 ? this.getBlogPostIdentifier(o1) === this.getBlogPostIdentifier(o2) : o1 === o2;
  }

  addBlogPostToCollectionIfMissing<Type extends Pick<IBlogPost, 'id'>>(
    blogPostCollection: Type[],
    ...blogPostsToCheck: (Type | null | undefined)[]
  ): Type[] {
    const blogPosts: Type[] = blogPostsToCheck.filter(blogPostItem => blogPostItem !== null && blogPostItem !== undefined);
    if (blogPosts.length > 0) {
      const blogPostCollectionIdentifiers = blogPostCollection.map(blogPostItem => this.getBlogPostIdentifier(blogPostItem));
      const blogPostsToAdd = blogPosts.filter(blogPostItem => {
        const blogPostIdentifier = this.getBlogPostIdentifier(blogPostItem);
        if (blogPostCollectionIdentifiers.includes(blogPostIdentifier)) {
          return false;
        }
        blogPostCollectionIdentifiers.push(blogPostIdentifier);
        return true;
      });
      return [...blogPostsToAdd, ...blogPostCollection];
    }
    return blogPostCollection;
  }

  protected convertValueFromClient<T extends IBlogPost | NewBlogPost | PartialUpdateBlogPost>(blogPost: T): RestOf<T> {
    return {
      ...blogPost,
      publishedAt: blogPost.publishedAt?.toJSON() ?? null,
      createdAt: blogPost.createdAt?.toJSON() ?? null,
      updatedAt: blogPost.updatedAt?.toJSON() ?? null,
    };
  }

  protected convertResponseFromServer(res: RestBlogPost): IBlogPost {
    return this.convertValueFromServer(res);
  }

  protected convertResponseArrayFromServer(res: RestBlogPost[]): IBlogPost[] {
    return res.map(item => this.convertValueFromServer(item));
  }
}
