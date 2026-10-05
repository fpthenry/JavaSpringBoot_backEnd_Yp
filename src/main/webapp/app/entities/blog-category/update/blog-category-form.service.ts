import { Service } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { IBlogCategory, NewBlogCategory } from '../blog-category.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IBlogCategory for edit and NewBlogCategoryFormGroupInput for create.
 */
type BlogCategoryFormGroupInput = IBlogCategory | PartialWithRequiredKeyOf<NewBlogCategory>;

type BlogCategoryFormDefaults = Pick<NewBlogCategory, 'id' | 'blogPosts'>;

type BlogCategoryFormGroupContent = {
  id: FormControl<IBlogCategory['id'] | NewBlogCategory['id']>;
  wpTermId: FormControl<IBlogCategory['wpTermId']>;
  name: FormControl<IBlogCategory['name']>;
  slug: FormControl<IBlogCategory['slug']>;
  description: FormControl<IBlogCategory['description']>;
  postCount: FormControl<IBlogCategory['postCount']>;
  parent: FormControl<IBlogCategory['parent']>;
  blogPosts: FormControl<IBlogCategory['blogPosts']>;
};

export type BlogCategoryFormGroup = FormGroup<BlogCategoryFormGroupContent>;

@Service()
export class BlogCategoryFormService {
  createBlogCategoryFormGroup(blogCategory?: BlogCategoryFormGroupInput): BlogCategoryFormGroup {
    const blogCategoryRawValue = {
      ...this.getFormDefaults(),
      ...(blogCategory ?? { id: null }),
    };

    return new FormGroup<BlogCategoryFormGroupContent>({
      id: new FormControl(
        { value: blogCategoryRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      wpTermId: new FormControl(blogCategoryRawValue.wpTermId),
      name: new FormControl(blogCategoryRawValue.name, {
        validators: [Validators.required, Validators.maxLength(255)],
      }),
      slug: new FormControl(blogCategoryRawValue.slug, {
        validators: [Validators.maxLength(255)],
      }),
      description: new FormControl(blogCategoryRawValue.description),
      postCount: new FormControl(blogCategoryRawValue.postCount),
      parent: new FormControl(blogCategoryRawValue.parent),
      blogPosts: new FormControl(blogCategoryRawValue.blogPosts ?? []),
    });
  }

  getBlogCategory(form: BlogCategoryFormGroup): IBlogCategory | NewBlogCategory {
    return form.getRawValue();
  }

  resetForm(form: BlogCategoryFormGroup, blogCategory: BlogCategoryFormGroupInput): void {
    const blogCategoryRawValue = { ...this.getFormDefaults(), ...blogCategory };
    form.reset({
      ...blogCategoryRawValue,
      id: { value: blogCategoryRawValue.id, disabled: true },
    });
  }

  private getFormDefaults(): BlogCategoryFormDefaults {
    return {
      id: null,
      blogPosts: [],
    };
  }
}
