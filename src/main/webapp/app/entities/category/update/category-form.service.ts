import { Service } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import dayjs from 'dayjs/esm';

import { DATE_TIME_FORMAT } from 'app/config';
import { ICategory, NewCategory } from '../category.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts ICategory for edit and NewCategoryFormGroupInput for create.
 */
type CategoryFormGroupInput = ICategory | PartialWithRequiredKeyOf<NewCategory>;

/**
 * Type that converts some properties for forms.
 */
type FormValueOf<T extends ICategory | NewCategory> = Omit<T, 'createdAt' | 'updatedAt'> & {
  createdAt?: string | null;
  updatedAt?: string | null;
};

type CategoryFormRawValue = FormValueOf<ICategory>;

type NewCategoryFormRawValue = FormValueOf<NewCategory>;

type CategoryFormDefaults = Pick<NewCategory, 'id' | 'createdAt' | 'updatedAt' | 'listingses'>;

type CategoryFormGroupContent = {
  id: FormControl<CategoryFormRawValue['id'] | NewCategory['id']>;
  name: FormControl<CategoryFormRawValue['name']>;
  slug: FormControl<CategoryFormRawValue['slug']>;
  description: FormControl<CategoryFormRawValue['description']>;
  count: FormControl<CategoryFormRawValue['count']>;
  industryCode: FormControl<CategoryFormRawValue['industryCode']>;
  displayOrder: FormControl<CategoryFormRawValue['displayOrder']>;
  createdAt: FormControl<CategoryFormRawValue['createdAt']>;
  updatedAt: FormControl<CategoryFormRawValue['updatedAt']>;
  parent: FormControl<CategoryFormRawValue['parent']>;
  listingses: FormControl<CategoryFormRawValue['listingses']>;
};

export type CategoryFormGroup = FormGroup<CategoryFormGroupContent>;

@Service()
export class CategoryFormService {
  createCategoryFormGroup(category?: CategoryFormGroupInput): CategoryFormGroup {
    const categoryRawValue = this.convertCategoryToCategoryRawValue({
      ...this.getFormDefaults(),
      ...(category ?? { id: null }),
    });

    return new FormGroup<CategoryFormGroupContent>({
      id: new FormControl(
        { value: categoryRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      name: new FormControl(categoryRawValue.name, {
        validators: [Validators.required, Validators.maxLength(500)],
      }),
      slug: new FormControl(categoryRawValue.slug, {
        validators: [Validators.required, Validators.maxLength(500)],
      }),
      description: new FormControl(categoryRawValue.description),
      count: new FormControl(categoryRawValue.count, {
        validators: [Validators.min(0)],
      }),
      industryCode: new FormControl(categoryRawValue.industryCode, {
        validators: [Validators.maxLength(50)],
      }),
      displayOrder: new FormControl(categoryRawValue.displayOrder, {
        validators: [Validators.min(0)],
      }),
      createdAt: new FormControl(categoryRawValue.createdAt),
      updatedAt: new FormControl(categoryRawValue.updatedAt),
      parent: new FormControl(categoryRawValue.parent),
      listingses: new FormControl(categoryRawValue.listingses ?? []),
    });
  }

  getCategory(form: CategoryFormGroup): ICategory | NewCategory {
    return this.convertCategoryRawValueToCategory(form.getRawValue());
  }

  resetForm(form: CategoryFormGroup, category: CategoryFormGroupInput): void {
    const categoryRawValue = this.convertCategoryToCategoryRawValue({ ...this.getFormDefaults(), ...category });
    form.reset({
      ...categoryRawValue,
      id: { value: categoryRawValue.id, disabled: true },
    });
  }

  private getFormDefaults(): CategoryFormDefaults {
    const currentTime = dayjs();

    return {
      id: null,
      createdAt: currentTime,
      updatedAt: currentTime,
      listingses: [],
    };
  }

  private convertCategoryRawValueToCategory(rawCategory: CategoryFormRawValue | NewCategoryFormRawValue): ICategory | NewCategory {
    return {
      ...rawCategory,
      createdAt: dayjs(rawCategory.createdAt, DATE_TIME_FORMAT),
      updatedAt: dayjs(rawCategory.updatedAt, DATE_TIME_FORMAT),
    };
  }

  private convertCategoryToCategoryRawValue(
    category: ICategory | (Partial<NewCategory> & CategoryFormDefaults),
  ): CategoryFormRawValue | PartialWithRequiredKeyOf<NewCategoryFormRawValue> {
    return {
      ...category,
      createdAt: category.createdAt ? category.createdAt.format(DATE_TIME_FORMAT) : undefined,
      updatedAt: category.updatedAt ? category.updatedAt.format(DATE_TIME_FORMAT) : undefined,
      listingses: category.listingses ?? [],
    };
  }
}
