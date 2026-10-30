import { Service } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import dayjs from 'dayjs/esm';

import { DATE_TIME_FORMAT } from 'app/config';
import { VSIC_CODE_PATTERN } from '../category-code';
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

type CategoryFormDefaults = Pick<NewCategory, 'id' | 'createdAt' | 'updatedAt' | 'listings'>;

type CategoryFormGroupContent = {
  id: FormControl<CategoryFormRawValue['id'] | NewCategory['id']>;
  code: FormControl<CategoryFormRawValue['code']>;
  level: FormControl<CategoryFormRawValue['level']>;
  name: FormControl<CategoryFormRawValue['name']>;
  slug: FormControl<CategoryFormRawValue['slug']>;
  description: FormControl<CategoryFormRawValue['description']>;
  listingCount: FormControl<CategoryFormRawValue['listingCount']>;
  createdAt: FormControl<CategoryFormRawValue['createdAt']>;
  updatedAt: FormControl<CategoryFormRawValue['updatedAt']>;
  parent: FormControl<CategoryFormRawValue['parent']>;
  listings: FormControl<CategoryFormRawValue['listings']>;
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
      // Sửa tay: mã ngành VSIC 2025 bắt buộc, đúng dạng; cấp chỉ để hiện (máy chủ tự suy từ mã)
      code: new FormControl(categoryRawValue.code, {
        validators: [Validators.required, Validators.maxLength(10), Validators.pattern(VSIC_CODE_PATTERN)],
      }),
      level: new FormControl({ value: categoryRawValue.level, disabled: true }),
      name: new FormControl(categoryRawValue.name, {
        validators: [Validators.required, Validators.maxLength(255)],
      }),
      slug: new FormControl(categoryRawValue.slug, {
        validators: [Validators.maxLength(255)],
      }),
      description: new FormControl(categoryRawValue.description),
      listingCount: new FormControl(categoryRawValue.listingCount),
      createdAt: new FormControl(categoryRawValue.createdAt),
      updatedAt: new FormControl(categoryRawValue.updatedAt),
      parent: new FormControl(categoryRawValue.parent),
      listings: new FormControl(categoryRawValue.listings ?? []),
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
      level: { value: categoryRawValue.level, disabled: true },
    });
  }

  private getFormDefaults(): CategoryFormDefaults {
    const currentTime = dayjs();

    return {
      id: null,
      createdAt: currentTime,
      updatedAt: currentTime,
      listings: [],
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
      listings: category.listings ?? [],
    };
  }
}
