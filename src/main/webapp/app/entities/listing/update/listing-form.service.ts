import { Service } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import dayjs from 'dayjs/esm';

import { DATE_TIME_FORMAT } from 'app/config';
import { IListing, NewListing } from '../listing.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IListing for edit and NewListingFormGroupInput for create.
 */
type ListingFormGroupInput = IListing | PartialWithRequiredKeyOf<NewListing>;

/**
 * Type that converts some properties for forms.
 */
type FormValueOf<T extends IListing | NewListing> = Omit<T, 'publishedAt' | 'modifiedAt' | 'createdAt' | 'updatedAt'> & {
  publishedAt?: string | null;
  modifiedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

type ListingFormRawValue = FormValueOf<IListing>;

type NewListingFormRawValue = FormValueOf<NewListing>;

type ListingFormDefaults = Pick<
  NewListing,
  'id' | 'isFeatured' | 'publishedAt' | 'modifiedAt' | 'createdAt' | 'updatedAt' | 'esIndexed' | 'categories' | 'locations'
>;

type ListingFormGroupContent = {
  id: FormControl<ListingFormRawValue['id'] | NewListing['id']>;
  wpId: FormControl<ListingFormRawValue['wpId']>;
  apiId: FormControl<ListingFormRawValue['apiId']>;
  name: FormControl<ListingFormRawValue['name']>;
  nameEn: FormControl<ListingFormRawValue['nameEn']>;
  nameAlias: FormControl<ListingFormRawValue['nameAlias']>;
  slug: FormControl<ListingFormRawValue['slug']>;
  description: FormControl<ListingFormRawValue['description']>;
  phone: FormControl<ListingFormRawValue['phone']>;
  mobile: FormControl<ListingFormRawValue['mobile']>;
  email: FormControl<ListingFormRawValue['email']>;
  website: FormControl<ListingFormRawValue['website']>;
  address: FormControl<ListingFormRawValue['address']>;
  locationJson: FormControl<ListingFormRawValue['locationJson']>;
  taxCode: FormControl<ListingFormRawValue['taxCode']>;
  representative: FormControl<ListingFormRawValue['representative']>;
  capital: FormControl<ListingFormRawValue['capital']>;
  foundedYear: FormControl<ListingFormRawValue['foundedYear']>;
  businessType: FormControl<ListingFormRawValue['businessType']>;
  businessStatus: FormControl<ListingFormRawValue['businessStatus']>;
  industryCode: FormControl<ListingFormRawValue['industryCode']>;
  managedBy: FormControl<ListingFormRawValue['managedBy']>;
  thumbnail: FormControl<ListingFormRawValue['thumbnail']>;
  images: FormControl<ListingFormRawValue['images']>;
  viewCount: FormControl<ListingFormRawValue['viewCount']>;
  isFeatured: FormControl<ListingFormRawValue['isFeatured']>;
  status: FormControl<ListingFormRawValue['status']>;
  publishedAt: FormControl<ListingFormRawValue['publishedAt']>;
  modifiedAt: FormControl<ListingFormRawValue['modifiedAt']>;
  createdAt: FormControl<ListingFormRawValue['createdAt']>;
  updatedAt: FormControl<ListingFormRawValue['updatedAt']>;
  esIndexed: FormControl<ListingFormRawValue['esIndexed']>;
  categories: FormControl<ListingFormRawValue['categories']>;
  locations: FormControl<ListingFormRawValue['locations']>;
};

export type ListingFormGroup = FormGroup<ListingFormGroupContent>;

@Service()
export class ListingFormService {
  createListingFormGroup(listing?: ListingFormGroupInput): ListingFormGroup {
    const listingRawValue = this.convertListingToListingRawValue({
      ...this.getFormDefaults(),
      ...(listing ?? { id: null }),
    });

    return new FormGroup<ListingFormGroupContent>({
      id: new FormControl(
        { value: listingRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      wpId: new FormControl(listingRawValue.wpId, {
        validators: [Validators.required],
      }),
      apiId: new FormControl(listingRawValue.apiId, {
        validators: [Validators.maxLength(100)],
      }),
      name: new FormControl(listingRawValue.name, {
        validators: [Validators.required, Validators.maxLength(500)],
      }),
      nameEn: new FormControl(listingRawValue.nameEn, {
        validators: [Validators.maxLength(500)],
      }),
      nameAlias: new FormControl(listingRawValue.nameAlias, {
        validators: [Validators.maxLength(500)],
      }),
      slug: new FormControl(listingRawValue.slug, {
        validators: [Validators.maxLength(500)],
      }),
      description: new FormControl(listingRawValue.description),
      phone: new FormControl(listingRawValue.phone, {
        validators: [Validators.maxLength(100)],
      }),
      mobile: new FormControl(listingRawValue.mobile, {
        validators: [Validators.maxLength(100)],
      }),
      email: new FormControl(listingRawValue.email, {
        validators: [Validators.maxLength(255)],
      }),
      website: new FormControl(listingRawValue.website, {
        validators: [Validators.maxLength(500)],
      }),
      address: new FormControl(listingRawValue.address),
      locationJson: new FormControl(listingRawValue.locationJson),
      taxCode: new FormControl(listingRawValue.taxCode, {
        validators: [Validators.maxLength(100)],
      }),
      representative: new FormControl(listingRawValue.representative, {
        validators: [Validators.maxLength(255)],
      }),
      capital: new FormControl(listingRawValue.capital, {
        validators: [Validators.maxLength(100)],
      }),
      foundedYear: new FormControl(listingRawValue.foundedYear, {
        validators: [Validators.maxLength(50)],
      }),
      businessType: new FormControl(listingRawValue.businessType, {
        validators: [Validators.maxLength(100)],
      }),
      businessStatus: new FormControl(listingRawValue.businessStatus, {
        validators: [Validators.maxLength(50)],
      }),
      industryCode: new FormControl(listingRawValue.industryCode, {
        validators: [Validators.maxLength(100)],
      }),
      managedBy: new FormControl(listingRawValue.managedBy, {
        validators: [Validators.maxLength(255)],
      }),
      thumbnail: new FormControl(listingRawValue.thumbnail, {
        validators: [Validators.maxLength(500)],
      }),
      images: new FormControl(listingRawValue.images),
      viewCount: new FormControl(listingRawValue.viewCount),
      isFeatured: new FormControl(listingRawValue.isFeatured),
      status: new FormControl(listingRawValue.status, {
        validators: [Validators.maxLength(50)],
      }),
      publishedAt: new FormControl(listingRawValue.publishedAt),
      modifiedAt: new FormControl(listingRawValue.modifiedAt),
      createdAt: new FormControl(listingRawValue.createdAt),
      updatedAt: new FormControl(listingRawValue.updatedAt),
      esIndexed: new FormControl(listingRawValue.esIndexed),
      categories: new FormControl(listingRawValue.categories ?? []),
      locations: new FormControl(listingRawValue.locations ?? []),
    });
  }

  getListing(form: ListingFormGroup): IListing | NewListing {
    return this.convertListingRawValueToListing(form.getRawValue());
  }

  resetForm(form: ListingFormGroup, listing: ListingFormGroupInput): void {
    const listingRawValue = this.convertListingToListingRawValue({ ...this.getFormDefaults(), ...listing });
    form.reset({
      ...listingRawValue,
      id: { value: listingRawValue.id, disabled: true },
    });
  }

  private getFormDefaults(): ListingFormDefaults {
    const currentTime = dayjs();

    return {
      id: null,
      isFeatured: false,
      publishedAt: currentTime,
      modifiedAt: currentTime,
      createdAt: currentTime,
      updatedAt: currentTime,
      esIndexed: false,
      categories: [],
      locations: [],
    };
  }

  private convertListingRawValueToListing(rawListing: ListingFormRawValue | NewListingFormRawValue): IListing | NewListing {
    return {
      ...rawListing,
      publishedAt: dayjs(rawListing.publishedAt, DATE_TIME_FORMAT),
      modifiedAt: dayjs(rawListing.modifiedAt, DATE_TIME_FORMAT),
      createdAt: dayjs(rawListing.createdAt, DATE_TIME_FORMAT),
      updatedAt: dayjs(rawListing.updatedAt, DATE_TIME_FORMAT),
    };
  }

  private convertListingToListingRawValue(
    listing: IListing | (Partial<NewListing> & ListingFormDefaults),
  ): ListingFormRawValue | PartialWithRequiredKeyOf<NewListingFormRawValue> {
    return {
      ...listing,
      publishedAt: listing.publishedAt ? listing.publishedAt.format(DATE_TIME_FORMAT) : undefined,
      modifiedAt: listing.modifiedAt ? listing.modifiedAt.format(DATE_TIME_FORMAT) : undefined,
      createdAt: listing.createdAt ? listing.createdAt.format(DATE_TIME_FORMAT) : undefined,
      updatedAt: listing.updatedAt ? listing.updatedAt.format(DATE_TIME_FORMAT) : undefined,
      categories: listing.categories ?? [],
      locations: listing.locations ?? [],
    };
  }
}
