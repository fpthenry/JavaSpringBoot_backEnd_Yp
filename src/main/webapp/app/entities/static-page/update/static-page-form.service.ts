import { Service } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import dayjs from 'dayjs/esm';

import { DATE_TIME_FORMAT } from 'app/config';
import { IStaticPage, NewStaticPage } from '../static-page.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IStaticPage for edit and NewStaticPageFormGroupInput for create.
 */
type StaticPageFormGroupInput = IStaticPage | PartialWithRequiredKeyOf<NewStaticPage>;

/**
 * Type that converts some properties for forms.
 */
type FormValueOf<T extends IStaticPage | NewStaticPage> = Omit<T, 'createdAt' | 'updatedAt'> & {
  createdAt?: string | null;
  updatedAt?: string | null;
};

type StaticPageFormRawValue = FormValueOf<IStaticPage>;

type NewStaticPageFormRawValue = FormValueOf<NewStaticPage>;

type StaticPageFormDefaults = Pick<NewStaticPage, 'id' | 'createdAt' | 'updatedAt'>;

type StaticPageFormGroupContent = {
  id: FormControl<StaticPageFormRawValue['id'] | NewStaticPage['id']>;
  title: FormControl<StaticPageFormRawValue['title']>;
  slug: FormControl<StaticPageFormRawValue['slug']>;
  content: FormControl<StaticPageFormRawValue['content']>;
  status: FormControl<StaticPageFormRawValue['status']>;
  template: FormControl<StaticPageFormRawValue['template']>;
  menuOrder: FormControl<StaticPageFormRawValue['menuOrder']>;
  createdAt: FormControl<StaticPageFormRawValue['createdAt']>;
  updatedAt: FormControl<StaticPageFormRawValue['updatedAt']>;
  author: FormControl<StaticPageFormRawValue['author']>;
  parent: FormControl<StaticPageFormRawValue['parent']>;
};

export type StaticPageFormGroup = FormGroup<StaticPageFormGroupContent>;

@Service()
export class StaticPageFormService {
  createStaticPageFormGroup(staticPage?: StaticPageFormGroupInput): StaticPageFormGroup {
    const staticPageRawValue = this.convertStaticPageToStaticPageRawValue({
      ...this.getFormDefaults(),
      ...(staticPage ?? { id: null }),
    });

    return new FormGroup<StaticPageFormGroupContent>({
      id: new FormControl(
        { value: staticPageRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      title: new FormControl(staticPageRawValue.title, {
        validators: [Validators.required, Validators.maxLength(500)],
      }),
      slug: new FormControl(staticPageRawValue.slug, {
        validators: [Validators.required, Validators.maxLength(500)],
      }),
      content: new FormControl(staticPageRawValue.content),
      status: new FormControl(staticPageRawValue.status, {
        validators: [Validators.maxLength(20)],
      }),
      template: new FormControl(staticPageRawValue.template, {
        validators: [Validators.maxLength(255)],
      }),
      menuOrder: new FormControl(staticPageRawValue.menuOrder),
      createdAt: new FormControl(staticPageRawValue.createdAt),
      updatedAt: new FormControl(staticPageRawValue.updatedAt),
      author: new FormControl(staticPageRawValue.author),
      parent: new FormControl(staticPageRawValue.parent),
    });
  }

  getStaticPage(form: StaticPageFormGroup): IStaticPage | NewStaticPage {
    return this.convertStaticPageRawValueToStaticPage(form.getRawValue());
  }

  resetForm(form: StaticPageFormGroup, staticPage: StaticPageFormGroupInput): void {
    const staticPageRawValue = this.convertStaticPageToStaticPageRawValue({ ...this.getFormDefaults(), ...staticPage });
    form.reset({
      ...staticPageRawValue,
      id: { value: staticPageRawValue.id, disabled: true },
    });
  }

  private getFormDefaults(): StaticPageFormDefaults {
    const currentTime = dayjs();

    return {
      id: null,
      createdAt: currentTime,
      updatedAt: currentTime,
    };
  }

  private convertStaticPageRawValueToStaticPage(
    rawStaticPage: StaticPageFormRawValue | NewStaticPageFormRawValue,
  ): IStaticPage | NewStaticPage {
    return {
      ...rawStaticPage,
      createdAt: dayjs(rawStaticPage.createdAt, DATE_TIME_FORMAT),
      updatedAt: dayjs(rawStaticPage.updatedAt, DATE_TIME_FORMAT),
    };
  }

  private convertStaticPageToStaticPageRawValue(
    staticPage: IStaticPage | (Partial<NewStaticPage> & StaticPageFormDefaults),
  ): StaticPageFormRawValue | PartialWithRequiredKeyOf<NewStaticPageFormRawValue> {
    return {
      ...staticPage,
      createdAt: staticPage.createdAt ? staticPage.createdAt.format(DATE_TIME_FORMAT) : undefined,
      updatedAt: staticPage.updatedAt ? staticPage.updatedAt.format(DATE_TIME_FORMAT) : undefined,
    };
  }
}
