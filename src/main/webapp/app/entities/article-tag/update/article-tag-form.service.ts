import { Service } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { IArticleTag, NewArticleTag } from '../article-tag.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IArticleTag for edit and NewArticleTagFormGroupInput for create.
 */
type ArticleTagFormGroupInput = IArticleTag | PartialWithRequiredKeyOf<NewArticleTag>;

type ArticleTagFormDefaults = Pick<NewArticleTag, 'id' | 'articleses'>;

type ArticleTagFormGroupContent = {
  id: FormControl<IArticleTag['id'] | NewArticleTag['id']>;
  name: FormControl<IArticleTag['name']>;
  slug: FormControl<IArticleTag['slug']>;
  count: FormControl<IArticleTag['count']>;
  articleses: FormControl<IArticleTag['articleses']>;
};

export type ArticleTagFormGroup = FormGroup<ArticleTagFormGroupContent>;

@Service()
export class ArticleTagFormService {
  createArticleTagFormGroup(articleTag?: ArticleTagFormGroupInput): ArticleTagFormGroup {
    const articleTagRawValue = {
      ...this.getFormDefaults(),
      ...(articleTag ?? { id: null }),
    };

    return new FormGroup<ArticleTagFormGroupContent>({
      id: new FormControl(
        { value: articleTagRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      name: new FormControl(articleTagRawValue.name, {
        validators: [Validators.required, Validators.maxLength(255)],
      }),
      slug: new FormControl(articleTagRawValue.slug, {
        validators: [Validators.required, Validators.maxLength(255)],
      }),
      count: new FormControl(articleTagRawValue.count, {
        validators: [Validators.min(0)],
      }),
      articleses: new FormControl(articleTagRawValue.articleses ?? []),
    });
  }

  getArticleTag(form: ArticleTagFormGroup): IArticleTag | NewArticleTag {
    return form.getRawValue();
  }

  resetForm(form: ArticleTagFormGroup, articleTag: ArticleTagFormGroupInput): void {
    const articleTagRawValue = { ...this.getFormDefaults(), ...articleTag };
    form.reset({
      ...articleTagRawValue,
      id: { value: articleTagRawValue.id, disabled: true },
    });
  }

  private getFormDefaults(): ArticleTagFormDefaults {
    return {
      id: null,
      articleses: [],
    };
  }
}
