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
type FormValueOf<T extends ILocation | NewLocation> = Omit<T, 'createdAt'> & {
  createdAt?: string | null;
};

type LocationFormRawValue = FormValueOf<ILocation>;

type NewLocationFormRawValue = FormValueOf<NewLocation>;

type LocationFormDefaults = Pick<NewLocation, 'id' | 'createdAt' | 'listings'>;

type LocationFormGroupContent = {
  id: FormControl<LocationFormRawValue['id'] | NewLocation['id']>;
  wpTermId: FormControl<LocationFormRawValue['wpTermId']>;
  name: FormControl<LocationFormRawValue['name']>;
  slug: FormControl<LocationFormRawValue['slug']>;
  type: FormControl<LocationFormRawValue['type']>;
  code: FormControl<LocationFormRawValue['code']>;
  latitude: FormControl<LocationFormRawValue['latitude']>;
  longitude: FormControl<LocationFormRawValue['longitude']>;
  createdAt: FormControl<LocationFormRawValue['createdAt']>;
  parent: FormControl<LocationFormRawValue['parent']>;
  listings: FormControl<LocationFormRawValue['listings']>;
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
      wpTermId: new FormControl(locationRawValue.wpTermId),
      name: new FormControl(locationRawValue.name, {
        validators: [Validators.required, Validators.maxLength(255)],
      }),
      slug: new FormControl(locationRawValue.slug, {
        validators: [Validators.maxLength(255)],
      }),
      type: new FormControl(locationRawValue.type, {
        validators: [Validators.maxLength(50)],
      }),
      code: new FormControl(locationRawValue.code, {
        validators: [Validators.maxLength(50)],
      }),
      latitude: new FormControl(locationRawValue.latitude),
      longitude: new FormControl(locationRawValue.longitude),
      createdAt: new FormControl(locationRawValue.createdAt),
      parent: new FormControl(locationRawValue.parent),
      listings: new FormControl(locationRawValue.listings ?? []),
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
      listings: [],
    };
  }

  private convertLocationRawValueToLocation(rawLocation: LocationFormRawValue | NewLocationFormRawValue): ILocation | NewLocation {
    return {
      ...rawLocation,
      createdAt: dayjs(rawLocation.createdAt, DATE_TIME_FORMAT),
    };
  }

  private convertLocationToLocationRawValue(
    location: ILocation | (Partial<NewLocation> & LocationFormDefaults),
  ): LocationFormRawValue | PartialWithRequiredKeyOf<NewLocationFormRawValue> {
    return {
      ...location,
      createdAt: location.createdAt ? location.createdAt.format(DATE_TIME_FORMAT) : undefined,
      listings: location.listings ?? [],
    };
  }
}
