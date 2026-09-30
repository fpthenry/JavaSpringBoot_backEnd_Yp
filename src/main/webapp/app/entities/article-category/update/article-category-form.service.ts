import { Service } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { IArticleCategory, NewArticleCategory } from '../article-category.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IArticleCategory for edit and NewArticleCategoryFormGroupInput for create.
 */
type ArticleCategoryFormGroupInput = IArticleCategory | PartialWithRequiredKeyOf<NewArticleCategory>;

type ArticleCategoryFormDefaults = Pick<NewArticleCategory, 'id' | 'articleses'>;

type ArticleCategoryFormGroupContent = {
  id: FormControl<IArticleCategory['id'] | NewArticleCategory['id']>;
  name: FormControl<IArticleCategory['name']>;
  slug: FormControl<IArticleCategory['slug']>;
  description: FormControl<IArticleCategory['description']>;
  count: FormControl<IArticleCategory['count']>;
  parent: FormControl<IArticleCategory['parent']>;
  articleses: FormControl<IArticleCategory['articleses']>;
};

export type ArticleCategoryFormGroup = FormGroup<ArticleCategoryFormGroupContent>;

@Service()
export class ArticleCategoryFormService {
  createArticleCategoryFormGroup(articleCategory?: ArticleCategoryFormGroupInput): ArticleCategoryFormGroup {
    const articleCategoryRawValue = {
      ...this.getFormDefaults(),
      ...(articleCategory ?? { id: null }),
    };

    return new FormGroup<ArticleCategoryFormGroupContent>({
      id: new FormControl(
        { value: articleCategoryRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      name: new FormControl(articleCategoryRawValue.name, {
        validators: [Validators.required, Validators.maxLength(255)],
      }),
      slug: new FormControl(articleCategoryRawValue.slug, {
        validators: [Validators.required, Validators.maxLength(255)],
      }),
      description: new FormControl(articleCategoryRawValue.description),
      count: new FormControl(articleCategoryRawValue.count, {
        validators: [Validators.min(0)],
      }),
      parent: new FormControl(articleCategoryRawValue.parent),
      articleses: new FormControl(articleCategoryRawValue.articleses ?? []),
    });
  }

  getArticleCategory(form: ArticleCategoryFormGroup): IArticleCategory | NewArticleCategory {
    return form.getRawValue();
  }

  resetForm(form: ArticleCategoryFormGroup, articleCategory: ArticleCategoryFormGroupInput): void {
    const articleCategoryRawValue = { ...this.getFormDefaults(), ...articleCategory };
    form.reset({
      ...articleCategoryRawValue,
      id: { value: articleCategoryRawValue.id, disabled: true },
    });
  }

  private getFormDefaults(): ArticleCategoryFormDefaults {
    return {
      id: null,
      articleses: [],
    };
  }
}
