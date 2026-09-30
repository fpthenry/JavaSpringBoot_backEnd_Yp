import { Service } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import dayjs from 'dayjs/esm';

import { DATE_TIME_FORMAT } from 'app/config';
import { ICachedContent, NewCachedContent } from '../cached-content.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts ICachedContent for edit and NewCachedContentFormGroupInput for create.
 */
type CachedContentFormGroupInput = ICachedContent | PartialWithRequiredKeyOf<NewCachedContent>;

/**
 * Type that converts some properties for forms.
 */
type FormValueOf<T extends ICachedContent | NewCachedContent> = Omit<T, 'expired'> & {
  expired?: string | null;
};

type CachedContentFormRawValue = FormValueOf<ICachedContent>;

type NewCachedContentFormRawValue = FormValueOf<NewCachedContent>;

type CachedContentFormDefaults = Pick<NewCachedContent, 'id' | 'expired'>;

type CachedContentFormGroupContent = {
  id: FormControl<CachedContentFormRawValue['id'] | NewCachedContent['id']>;
  termSlug: FormControl<CachedContentFormRawValue['termSlug']>;
  expired: FormControl<CachedContentFormRawValue['expired']>;
  content: FormControl<CachedContentFormRawValue['content']>;
};

export type CachedContentFormGroup = FormGroup<CachedContentFormGroupContent>;

@Service()
export class CachedContentFormService {
  createCachedContentFormGroup(cachedContent?: CachedContentFormGroupInput): CachedContentFormGroup {
    const cachedContentRawValue = this.convertCachedContentToCachedContentRawValue({
      ...this.getFormDefaults(),
      ...(cachedContent ?? { id: null }),
    });

    return new FormGroup<CachedContentFormGroupContent>({
      id: new FormControl(
        { value: cachedContentRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      termSlug: new FormControl(cachedContentRawValue.termSlug, {
        validators: [Validators.required, Validators.maxLength(255)],
      }),
      expired: new FormControl(cachedContentRawValue.expired),
      content: new FormControl(cachedContentRawValue.content),
    });
  }

  getCachedContent(form: CachedContentFormGroup): ICachedContent | NewCachedContent {
    return this.convertCachedContentRawValueToCachedContent(form.getRawValue());
  }

  resetForm(form: CachedContentFormGroup, cachedContent: CachedContentFormGroupInput): void {
    const cachedContentRawValue = this.convertCachedContentToCachedContentRawValue({ ...this.getFormDefaults(), ...cachedContent });
    form.reset({
      ...cachedContentRawValue,
      id: { value: cachedContentRawValue.id, disabled: true },
    });
  }

  private getFormDefaults(): CachedContentFormDefaults {
    const currentTime = dayjs();

    return {
      id: null,
      expired: currentTime,
    };
  }

  private convertCachedContentRawValueToCachedContent(
    rawCachedContent: CachedContentFormRawValue | NewCachedContentFormRawValue,
  ): ICachedContent | NewCachedContent {
    return {
      ...rawCachedContent,
      expired: dayjs(rawCachedContent.expired, DATE_TIME_FORMAT),
    };
  }

  private convertCachedContentToCachedContentRawValue(
    cachedContent: ICachedContent | (Partial<NewCachedContent> & CachedContentFormDefaults),
  ): CachedContentFormRawValue | PartialWithRequiredKeyOf<NewCachedContentFormRawValue> {
    return {
      ...cachedContent,
      expired: cachedContent.expired ? cachedContent.expired.format(DATE_TIME_FORMAT) : undefined,
    };
  }
}
