import { Service } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import dayjs from 'dayjs/esm';

import { DATE_TIME_FORMAT } from 'app/config';
import { IListingImage, NewListingImage } from '../listing-image.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IListingImage for edit and NewListingImageFormGroupInput for create.
 */
type ListingImageFormGroupInput = IListingImage | PartialWithRequiredKeyOf<NewListingImage>;

/**
 * Type that converts some properties for forms.
 */
type FormValueOf<T extends IListingImage | NewListingImage> = Omit<T, 'createdAt' | 'updatedAt'> & {
  createdAt?: string | null;
  updatedAt?: string | null;
};

type ListingImageFormRawValue = FormValueOf<IListingImage>;

type NewListingImageFormRawValue = FormValueOf<NewListingImage>;

type ListingImageFormDefaults = Pick<NewListingImage, 'id' | 'isFeatured' | 'createdAt' | 'updatedAt'>;

type ListingImageFormGroupContent = {
  id: FormControl<ListingImageFormRawValue['id'] | NewListingImage['id']>;
  imageUrl: FormControl<ListingImageFormRawValue['imageUrl']>;
  thumbnailUrl: FormControl<ListingImageFormRawValue['thumbnailUrl']>;
  altText: FormControl<ListingImageFormRawValue['altText']>;
  displayOrder: FormControl<ListingImageFormRawValue['displayOrder']>;
  isFeatured: FormControl<ListingImageFormRawValue['isFeatured']>;
  createdAt: FormControl<ListingImageFormRawValue['createdAt']>;
  updatedAt: FormControl<ListingImageFormRawValue['updatedAt']>;
  listing: FormControl<ListingImageFormRawValue['listing']>;
  gallery: FormControl<ListingImageFormRawValue['gallery']>;
};

export type ListingImageFormGroup = FormGroup<ListingImageFormGroupContent>;

@Service()
export class ListingImageFormService {
  createListingImageFormGroup(listingImage?: ListingImageFormGroupInput): ListingImageFormGroup {
    const listingImageRawValue = this.convertListingImageToListingImageRawValue({
      ...this.getFormDefaults(),
      ...(listingImage ?? { id: null }),
    });

    return new FormGroup<ListingImageFormGroupContent>({
      id: new FormControl(
        { value: listingImageRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      imageUrl: new FormControl(listingImageRawValue.imageUrl, {
        validators: [Validators.required],
      }),
      thumbnailUrl: new FormControl(listingImageRawValue.thumbnailUrl),
      altText: new FormControl(listingImageRawValue.altText, {
        validators: [Validators.maxLength(500)],
      }),
      displayOrder: new FormControl(listingImageRawValue.displayOrder, {
        validators: [Validators.min(0)],
      }),
      isFeatured: new FormControl(listingImageRawValue.isFeatured),
      createdAt: new FormControl(listingImageRawValue.createdAt),
      updatedAt: new FormControl(listingImageRawValue.updatedAt),
      listing: new FormControl(listingImageRawValue.listing, {
        validators: [Validators.required],
      }),
      gallery: new FormControl(listingImageRawValue.gallery),
    });
  }

  getListingImage(form: ListingImageFormGroup): IListingImage | NewListingImage {
    return this.convertListingImageRawValueToListingImage(form.getRawValue());
  }

  resetForm(form: ListingImageFormGroup, listingImage: ListingImageFormGroupInput): void {
    const listingImageRawValue = this.convertListingImageToListingImageRawValue({ ...this.getFormDefaults(), ...listingImage });
    form.reset({
      ...listingImageRawValue,
      id: { value: listingImageRawValue.id, disabled: true },
    });
  }

  private getFormDefaults(): ListingImageFormDefaults {
    const currentTime = dayjs();

    return {
      id: null,
      isFeatured: false,
      createdAt: currentTime,
      updatedAt: currentTime,
    };
  }

  private convertListingImageRawValueToListingImage(
    rawListingImage: ListingImageFormRawValue | NewListingImageFormRawValue,
  ): IListingImage | NewListingImage {
    return {
      ...rawListingImage,
      createdAt: dayjs(rawListingImage.createdAt, DATE_TIME_FORMAT),
      updatedAt: dayjs(rawListingImage.updatedAt, DATE_TIME_FORMAT),
    };
  }

  private convertListingImageToListingImageRawValue(
    listingImage: IListingImage | (Partial<NewListingImage> & ListingImageFormDefaults),
  ): ListingImageFormRawValue | PartialWithRequiredKeyOf<NewListingImageFormRawValue> {
    return {
      ...listingImage,
      createdAt: listingImage.createdAt ? listingImage.createdAt.format(DATE_TIME_FORMAT) : undefined,
      updatedAt: listingImage.updatedAt ? listingImage.updatedAt.format(DATE_TIME_FORMAT) : undefined,
    };
  }
}
