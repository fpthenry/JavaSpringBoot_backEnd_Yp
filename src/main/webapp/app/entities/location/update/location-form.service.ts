import { Service } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import dayjs from 'dayjs/esm';

import { DATE_TIME_FORMAT } from 'app/config';
import { ILocation, NewLocation } from '../location.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts ILocation for edit and NewLocationFormGroupInput for create.
 */
type LocationFormGroupInput = ILocation | PartialWithRequiredKeyOf<NewLocation>;

/**
 * Type that converts some properties for forms.
 */
type FormValueOf<T extends ILocation | NewLocation> = Omit<T, 'createdAt' | 'updatedAt'> & {
  createdAt?: string | null;
  updatedAt?: string | null;
};

type LocationFormRawValue = FormValueOf<ILocation>;

type NewLocationFormRawValue = FormValueOf<NewLocation>;

type LocationFormDefaults = Pick<NewLocation, 'id' | 'createdAt' | 'updatedAt' | 'listingses'>;

type LocationFormGroupContent = {
  id: FormControl<LocationFormRawValue['id'] | NewLocation['id']>;
  name: FormControl<LocationFormRawValue['name']>;
  slug: FormControl<LocationFormRawValue['slug']>;
  type: FormControl<LocationFormRawValue['type']>;
  provinceCode: FormControl<LocationFormRawValue['provinceCode']>;
  districtCode: FormControl<LocationFormRawValue['districtCode']>;
  wardCode: FormControl<LocationFormRawValue['wardCode']>;
  count: FormControl<LocationFormRawValue['count']>;
  displayOrder: FormControl<LocationFormRawValue['displayOrder']>;
  createdAt: FormControl<LocationFormRawValue['createdAt']>;
  updatedAt: FormControl<LocationFormRawValue['updatedAt']>;
  parent: FormControl<LocationFormRawValue['parent']>;
  listingses: FormControl<LocationFormRawValue['listingses']>;
};

export type LocationFormGroup = FormGroup<LocationFormGroupContent>;

@Service()
export class LocationFormService {
  createLocationFormGroup(location?: LocationFormGroupInput): LocationFormGroup {
    const locationRawValue = this.convertLocationToLocationRawValue({
      ...this.getFormDefaults(),
      ...(location ?? { id: null }),
    });

    return new FormGroup<LocationFormGroupContent>({
      id: new FormControl(
        { value: locationRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      name: new FormControl(locationRawValue.name, {
        validators: [Validators.required, Validators.maxLength(255)],
      }),
      slug: new FormControl(locationRawValue.slug, {
        validators: [Validators.required, Validators.maxLength(255)],
      }),
      type: new FormControl(locationRawValue.type, {
        validators: [Validators.maxLength(50)],
      }),
      provinceCode: new FormControl(locationRawValue.provinceCode, {
        validators: [Validators.maxLength(10)],
      }),
      districtCode: new FormControl(locationRawValue.districtCode, {
        validators: [Validators.maxLength(10)],
      }),
      wardCode: new FormControl(locationRawValue.wardCode, {
        validators: [Validators.maxLength(10)],
      }),
      count: new FormControl(locationRawValue.count, {
        validators: [Validators.min(0)],
      }),
      displayOrder: new FormControl(locationRawValue.displayOrder, {
        validators: [Validators.min(0)],
      }),
      createdAt: new FormControl(locationRawValue.createdAt),
      updatedAt: new FormControl(locationRawValue.updatedAt),
      parent: new FormControl(locationRawValue.parent),
      listingses: new FormControl(locationRawValue.listingses ?? []),
    });
  }

  getLocation(form: LocationFormGroup): ILocation | NewLocation {
    return this.convertLocationRawValueToLocation(form.getRawValue());
  }

  resetForm(form: LocationFormGroup, location: LocationFormGroupInput): void {
    const locationRawValue = this.convertLocationToLocationRawValue({ ...this.getFormDefaults(), ...location });
    form.reset({
      ...locationRawValue,
      id: { value: locationRawValue.id, disabled: true },
    });
  }

  private getFormDefaults(): LocationFormDefaults {
    const currentTime = dayjs();

    return {
      id: null,
      createdAt: currentTime,
      updatedAt: currentTime,
      listingses: [],
    };
  }

  private convertLocationRawValueToLocation(rawLocation: LocationFormRawValue | NewLocationFormRawValue): ILocation | NewLocation {
    return {
      ...rawLocation,
      createdAt: dayjs(rawLocation.createdAt, DATE_TIME_FORMAT),
      updatedAt: dayjs(rawLocation.updatedAt, DATE_TIME_FORMAT),
    };
  }

  private convertLocationToLocationRawValue(
    location: ILocation | (Partial<NewLocation> & LocationFormDefaults),
  ): LocationFormRawValue | PartialWithRequiredKeyOf<NewLocationFormRawValue> {
    return {
      ...location,
      createdAt: location.createdAt ? location.createdAt.format(DATE_TIME_FORMAT) : undefined,
      updatedAt: location.updatedAt ? location.updatedAt.format(DATE_TIME_FORMAT) : undefined,
      listingses: location.listingses ?? [],
    };
  }
}
