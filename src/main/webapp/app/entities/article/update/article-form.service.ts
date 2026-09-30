import { Service } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import dayjs from 'dayjs/esm';

import { DATE_TIME_FORMAT } from 'app/config';
import { IArticle, NewArticle } from '../article.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IArticle for edit and NewArticleFormGroupInput for create.
 */
type ArticleFormGroupInput = IArticle | PartialWithRequiredKeyOf<NewArticle>;

/**
 * Type that converts some properties for forms.
 */
type FormValueOf<T extends IArticle | NewArticle> = Omit<T, 'createdAt' | 'updatedAt'> & {
  createdAt?: string | null;
  updatedAt?: string | null;
};

type ArticleFormRawValue = FormValueOf<IArticle>;

type NewArticleFormRawValue = FormValueOf<NewArticle>;

type ArticleFormDefaults = Pick<NewArticle, 'id' | 'createdAt' | 'updatedAt' | 'categorieses' | 'tagses'>;

type ArticleFormGroupContent = {
  id: FormControl<ArticleFormRawValue['id'] | NewArticle['id']>;
  title: FormControl<ArticleFormRawValue['title']>;
  slug: FormControl<ArticleFormRawValue['slug']>;
  excerpt: FormControl<ArticleFormRawValue['excerpt']>;
  content: FormControl<ArticleFormRawValue['content']>;
  status: FormControl<ArticleFormRawValue['status']>;
  featuredImageUrl: FormControl<ArticleFormRawValue['featuredImageUrl']>;
  createdAt: FormControl<ArticleFormRawValue['createdAt']>;
  updatedAt: FormControl<ArticleFormRawValue['updatedAt']>;
  author: FormControl<ArticleFormRawValue['author']>;
  categorieses: FormControl<ArticleFormRawValue['categorieses']>;
  tagses: FormControl<ArticleFormRawValue['tagses']>;
};

export type ArticleFormGroup = FormGroup<ArticleFormGroupContent>;

@Service()
export class ArticleFormService {
  createArticleFormGroup(article?: ArticleFormGroupInput): ArticleFormGroup {
    const articleRawValue = this.convertArticleToArticleRawValue({
      ...this.getFormDefaults(),
      ...(article ?? { id: null }),
    });

    return new FormGroup<ArticleFormGroupContent>({
      id: new FormControl(
        { value: articleRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      title: new FormControl(articleRawValue.title, {
        validators: [Validators.required, Validators.maxLength(500)],
      }),
      slug: new FormControl(articleRawValue.slug, {
        validators: [Validators.required, Validators.maxLength(500)],
      }),
      excerpt: new FormControl(articleRawValue.excerpt),
      content: new FormControl(articleRawValue.content),
      status: new FormControl(articleRawValue.status, {
        validators: [Validators.maxLength(20)],
      }),
      featuredImageUrl: new FormControl(articleRawValue.featuredImageUrl),
      createdAt: new FormControl(articleRawValue.createdAt),
      updatedAt: new FormControl(articleRawValue.updatedAt),
      author: new FormControl(articleRawValue.author),
      categorieses: new FormControl(articleRawValue.categorieses ?? []),
      tagses: new FormControl(articleRawValue.tagses ?? []),
    });
  }

  getArticle(form: ArticleFormGroup): IArticle | NewArticle {
    return this.convertArticleRawValueToArticle(form.getRawValue());
  }

  resetForm(form: ArticleFormGroup, article: ArticleFormGroupInput): void {
    const articleRawValue = this.convertArticleToArticleRawValue({ ...this.getFormDefaults(), ...article });
    form.reset({
      ...articleRawValue,
      id: { value: articleRawValue.id, disabled: true },
    });
  }

  private getFormDefaults(): ArticleFormDefaults {
    const currentTime = dayjs();

    return {
      id: null,
      createdAt: currentTime,
      updatedAt: currentTime,
      categorieses: [],
      tagses: [],
    };
  }

  private convertArticleRawValueToArticle(rawArticle: ArticleFormRawValue | NewArticleFormRawValue): IArticle | NewArticle {
    return {
      ...rawArticle,
      createdAt: dayjs(rawArticle.createdAt, DATE_TIME_FORMAT),
      updatedAt: dayjs(rawArticle.updatedAt, DATE_TIME_FORMAT),
    };
  }

  private convertArticleToArticleRawValue(
    article: IArticle | (Partial<NewArticle> & ArticleFormDefaults),
  ): ArticleFormRawValue | PartialWithRequiredKeyOf<NewArticleFormRawValue> {
    return {
      ...article,
      createdAt: article.createdAt ? article.createdAt.format(DATE_TIME_FORMAT) : undefined,
      updatedAt: article.updatedAt ? article.updatedAt.format(DATE_TIME_FORMAT) : undefined,
      categorieses: article.categorieses ?? [],
      tagses: article.tagses ?? [],
    };
  }
}
