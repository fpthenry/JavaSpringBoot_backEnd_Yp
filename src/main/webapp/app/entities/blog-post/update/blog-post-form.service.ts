import { Service } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import dayjs from 'dayjs/esm';

import { DATE_TIME_FORMAT } from 'app/config';
import { IBlogPost, NewBlogPost } from '../blog-post.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IBlogPost for edit and NewBlogPostFormGroupInput for create.
 */
type BlogPostFormGroupInput = IBlogPost | PartialWithRequiredKeyOf<NewBlogPost>;

/**
 * Type that converts some properties for forms.
 */
type FormValueOf<T extends IBlogPost | NewBlogPost> = Omit<T, 'publishedAt' | 'createdAt' | 'updatedAt'> & {
  publishedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

type BlogPostFormRawValue = FormValueOf<IBlogPost>;

type NewBlogPostFormRawValue = FormValueOf<NewBlogPost>;

type BlogPostFormDefaults = Pick<NewBlogPost, 'id' | 'publishedAt' | 'createdAt' | 'updatedAt'>;

type BlogPostFormGroupContent = {
  id: FormControl<BlogPostFormRawValue['id'] | NewBlogPost['id']>;
  wpId: FormControl<BlogPostFormRawValue['wpId']>;
  title: FormControl<BlogPostFormRawValue['title']>;
  slug: FormControl<BlogPostFormRawValue['slug']>;
  content: FormControl<BlogPostFormRawValue['content']>;
  excerpt: FormControl<BlogPostFormRawValue['excerpt']>;
  thumbnail: FormControl<BlogPostFormRawValue['thumbnail']>;
  status: FormControl<BlogPostFormRawValue['status']>;
  viewCount: FormControl<BlogPostFormRawValue['viewCount']>;
  publishedAt: FormControl<BlogPostFormRawValue['publishedAt']>;
  createdAt: FormControl<BlogPostFormRawValue['createdAt']>;
  updatedAt: FormControl<BlogPostFormRawValue['updatedAt']>;
};

export type BlogPostFormGroup = FormGroup<BlogPostFormGroupContent>;

@Service()
export class BlogPostFormService {
  createBlogPostFormGroup(blogPost?: BlogPostFormGroupInput): BlogPostFormGroup {
    const blogPostRawValue = this.convertBlogPostToBlogPostRawValue({
      ...this.getFormDefaults(),
      ...(blogPost ?? { id: null }),
    });

    return new FormGroup<BlogPostFormGroupContent>({
      id: new FormControl(
        { value: blogPostRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      wpId: new FormControl(blogPostRawValue.wpId, {
        validators: [Validators.required],
      }),
      title: new FormControl(blogPostRawValue.title, {
        validators: [Validators.required, Validators.maxLength(500)],
      }),
      slug: new FormControl(blogPostRawValue.slug, {
        validators: [Validators.maxLength(500)],
      }),
      content: new FormControl(blogPostRawValue.content),
      excerpt: new FormControl(blogPostRawValue.excerpt),
      thumbnail: new FormControl(blogPostRawValue.thumbnail, {
        validators: [Validators.maxLength(500)],
      }),
      status: new FormControl(blogPostRawValue.status, {
        validators: [Validators.maxLength(50)],
      }),
      viewCount: new FormControl(blogPostRawValue.viewCount),
      publishedAt: new FormControl(blogPostRawValue.publishedAt),
      createdAt: new FormControl(blogPostRawValue.createdAt),
      updatedAt: new FormControl(blogPostRawValue.updatedAt),
    });
  }

  getBlogPost(form: BlogPostFormGroup): IBlogPost | NewBlogPost {
    return this.convertBlogPostRawValueToBlogPost(form.getRawValue());
  }

  resetForm(form: BlogPostFormGroup, blogPost: BlogPostFormGroupInput): void {
    const blogPostRawValue = this.convertBlogPostToBlogPostRawValue({ ...this.getFormDefaults(), ...blogPost });
    form.reset({
      ...blogPostRawValue,
      id: { value: blogPostRawValue.id, disabled: true },
    });
  }

  private getFormDefaults(): BlogPostFormDefaults {
    const currentTime = dayjs();

    return {
      id: null,
      publishedAt: currentTime,
      createdAt: currentTime,
      updatedAt: currentTime,
    };
  }

  private convertBlogPostRawValueToBlogPost(rawBlogPost: BlogPostFormRawValue | NewBlogPostFormRawValue): IBlogPost | NewBlogPost {
    return {
      ...rawBlogPost,
      publishedAt: dayjs(rawBlogPost.publishedAt, DATE_TIME_FORMAT),
      createdAt: dayjs(rawBlogPost.createdAt, DATE_TIME_FORMAT),
      updatedAt: dayjs(rawBlogPost.updatedAt, DATE_TIME_FORMAT),
    };
  }

  private convertBlogPostToBlogPostRawValue(
    blogPost: IBlogPost | (Partial<NewBlogPost> & BlogPostFormDefaults),
  ): BlogPostFormRawValue | PartialWithRequiredKeyOf<NewBlogPostFormRawValue> {
    return {
      ...blogPost,
      publishedAt: blogPost.publishedAt ? blogPost.publishedAt.format(DATE_TIME_FORMAT) : undefined,
      createdAt: blogPost.createdAt ? blogPost.createdAt.format(DATE_TIME_FORMAT) : undefined,
      updatedAt: blogPost.updatedAt ? blogPost.updatedAt.format(DATE_TIME_FORMAT) : undefined,
    };
  }
}
