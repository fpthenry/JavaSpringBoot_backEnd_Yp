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
type FormValueOf<T extends IListing | NewListing> = Omit<T, 'foundedDate' | 'licenseModifiedDate' | 'createdAt' | 'updatedAt'> & {
  foundedDate?: string | null;
  licenseModifiedDate?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

type ListingFormRawValue = FormValueOf<IListing>;

type NewListingFormRawValue = FormValueOf<NewListing>;

type ListingFormDefaults = Pick<
  NewListing,
  'id' | 'foundedDate' | 'licenseModifiedDate' | 'createdAt' | 'updatedAt' | 'categorieses' | 'locationses'
>;

type ListingFormGroupContent = {
  id: FormControl<ListingFormRawValue['id'] | NewListing['id']>;
  title: FormControl<ListingFormRawValue['title']>;
  slug: FormControl<ListingFormRawValue['slug']>;
  content: FormControl<ListingFormRawValue['content']>;
  status: FormControl<ListingFormRawValue['status']>;
  email: FormControl<ListingFormRawValue['email']>;
  telephone: FormControl<ListingFormRawValue['telephone']>;
  mobile: FormControl<ListingFormRawValue['mobile']>;
  website: FormControl<ListingFormRawValue['website']>;
  fax: FormControl<ListingFormRawValue['fax']>;
  taxCode: FormControl<ListingFormRawValue['taxCode']>;
  nameAlias: FormControl<ListingFormRawValue['nameAlias']>;
  nameEn: FormControl<ListingFormRawValue['nameEn']>;
  representative: FormControl<ListingFormRawValue['representative']>;
  mainIndustry: FormControl<ListingFormRawValue['mainIndustry']>;
  managedBy: FormControl<ListingFormRawValue['managedBy']>;
  businessType: FormControl<ListingFormRawValue['businessType']>;
  statusYp: FormControl<ListingFormRawValue['statusYp']>;
  foundedDate: FormControl<ListingFormRawValue['foundedDate']>;
  licenseModifiedDate: FormControl<ListingFormRawValue['licenseModifiedDate']>;
  address: FormControl<ListingFormRawValue['address']>;
  addressAlternative: FormControl<ListingFormRawValue['addressAlternative']>;
  latitude: FormControl<ListingFormRawValue['latitude']>;
  longitude: FormControl<ListingFormRawValue['longitude']>;
  apiId: FormControl<ListingFormRawValue['apiId']>;
  createdAt: FormControl<ListingFormRawValue['createdAt']>;
  updatedAt: FormControl<ListingFormRawValue['updatedAt']>;
  author: FormControl<ListingFormRawValue['author']>;
  categorieses: FormControl<ListingFormRawValue['categorieses']>;
  locationses: FormControl<ListingFormRawValue['locationses']>;
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
      title: new FormControl(listingRawValue.title, {
        validators: [Validators.required, Validators.maxLength(500)],
      }),
      slug: new FormControl(listingRawValue.slug, {
        validators: [Validators.required, Validators.maxLength(500)],
      }),
      content: new FormControl(listingRawValue.content),
      status: new FormControl(listingRawValue.status, {
        validators: [Validators.maxLength(20)],
      }),
      email: new FormControl(listingRawValue.email, {
        validators: [Validators.maxLength(255)],
      }),
      telephone: new FormControl(listingRawValue.telephone, {
        validators: [Validators.maxLength(50)],
      }),
      mobile: new FormControl(listingRawValue.mobile, {
        validators: [Validators.maxLength(50)],
      }),
      website: new FormControl(listingRawValue.website, {
        validators: [Validators.maxLength(255)],
      }),
      fax: new FormControl(listingRawValue.fax, {
        validators: [Validators.maxLength(50)],
      }),
      taxCode: new FormControl(listingRawValue.taxCode, {
        validators: [Validators.maxLength(50)],
      }),
      nameAlias: new FormControl(listingRawValue.nameAlias, {
        validators: [Validators.maxLength(500)],
      }),
      nameEn: new FormControl(listingRawValue.nameEn, {
        validators: [Validators.maxLength(500)],
      }),
      representative: new FormControl(listingRawValue.representative, {
        validators: [Validators.maxLength(500)],
      }),
      mainIndustry: new FormControl(listingRawValue.mainIndustry, {
        validators: [Validators.maxLength(500)],
      }),
      managedBy: new FormControl(listingRawValue.managedBy, {
        validators: [Validators.maxLength(500)],
      }),
      businessType: new FormControl(listingRawValue.businessType, {
        validators: [Validators.maxLength(100)],
      }),
      statusYp: new FormControl(listingRawValue.statusYp, {
        validators: [Validators.maxLength(50)],
      }),
      foundedDate: new FormControl(listingRawValue.foundedDate),
      licenseModifiedDate: new FormControl(listingRawValue.licenseModifiedDate),
      address: new FormControl(listingRawValue.address),
      addressAlternative: new FormControl(listingRawValue.addressAlternative),
      latitude: new FormControl(listingRawValue.latitude),
      longitude: new FormControl(listingRawValue.longitude),
      apiId: new FormControl(listingRawValue.apiId),
      createdAt: new FormControl(listingRawValue.createdAt),
      updatedAt: new FormControl(listingRawValue.updatedAt),
      author: new FormControl(listingRawValue.author),
      categorieses: new FormControl(listingRawValue.categorieses ?? []),
      locationses: new FormControl(listingRawValue.locationses ?? []),
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
      foundedDate: currentTime,
      licenseModifiedDate: currentTime,
      createdAt: currentTime,
      updatedAt: currentTime,
      categorieses: [],
      locationses: [],
    };
  }

  private convertListingRawValueToListing(rawListing: ListingFormRawValue | NewListingFormRawValue): IListing | NewListing {
    return {
      ...rawListing,
      foundedDate: dayjs(rawListing.foundedDate, DATE_TIME_FORMAT),
      licenseModifiedDate: dayjs(rawListing.licenseModifiedDate, DATE_TIME_FORMAT),
      createdAt: dayjs(rawListing.createdAt, DATE_TIME_FORMAT),
      updatedAt: dayjs(rawListing.updatedAt, DATE_TIME_FORMAT),
    };
  }

  private convertListingToListingRawValue(
    listing: IListing | (Partial<NewListing> & ListingFormDefaults),
  ): ListingFormRawValue | PartialWithRequiredKeyOf<NewListingFormRawValue> {
    return {
      ...listing,
      foundedDate: listing.foundedDate ? listing.foundedDate.format(DATE_TIME_FORMAT) : undefined,
      licenseModifiedDate: listing.licenseModifiedDate ? listing.licenseModifiedDate.format(DATE_TIME_FORMAT) : undefined,
      createdAt: listing.createdAt ? listing.createdAt.format(DATE_TIME_FORMAT) : undefined,
      updatedAt: listing.updatedAt ? listing.updatedAt.format(DATE_TIME_FORMAT) : undefined,
      categorieses: listing.categorieses ?? [],
      locationses: listing.locationses ?? [],
    };
  }
}
