import { beforeEach, describe, expect, it } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../listing.test-samples';

import { ListingFormService } from './listing-form.service';

describe('Listing Form Service', () => {
  let service: ListingFormService;

  beforeEach(() => {
    service = TestBed.inject(ListingFormService);
  });

  describe('Service methods', () => {
    describe('createListingFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createListingFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            title: expect.any(Object),
            slug: expect.any(Object),
            content: expect.any(Object),
            status: expect.any(Object),
            email: expect.any(Object),
            telephone: expect.any(Object),
            mobile: expect.any(Object),
            website: expect.any(Object),
            fax: expect.any(Object),
            taxCode: expect.any(Object),
            nameAlias: expect.any(Object),
            nameEn: expect.any(Object),
            representative: expect.any(Object),
            mainIndustry: expect.any(Object),
            managedBy: expect.any(Object),
            businessType: expect.any(Object),
            statusYp: expect.any(Object),
            foundedDate: expect.any(Object),
            licenseModifiedDate: expect.any(Object),
            address: expect.any(Object),
            addressAlternative: expect.any(Object),
            latitude: expect.any(Object),
            longitude: expect.any(Object),
            apiId: expect.any(Object),
            createdAt: expect.any(Object),
            updatedAt: expect.any(Object),
            author: expect.any(Object),
            categorieses: expect.any(Object),
            locationses: expect.any(Object),
          }),
        );
      });

      it('passing IListing should create a new form with FormGroup', () => {
        const formGroup = service.createListingFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            title: expect.any(Object),
            slug: expect.any(Object),
            content: expect.any(Object),
            status: expect.any(Object),
            email: expect.any(Object),
            telephone: expect.any(Object),
            mobile: expect.any(Object),
            website: expect.any(Object),
            fax: expect.any(Object),
            taxCode: expect.any(Object),
            nameAlias: expect.any(Object),
            nameEn: expect.any(Object),
            representative: expect.any(Object),
            mainIndustry: expect.any(Object),
            managedBy: expect.any(Object),
            businessType: expect.any(Object),
            statusYp: expect.any(Object),
            foundedDate: expect.any(Object),
            licenseModifiedDate: expect.any(Object),
            address: expect.any(Object),
            addressAlternative: expect.any(Object),
            latitude: expect.any(Object),
            longitude: expect.any(Object),
            apiId: expect.any(Object),
            createdAt: expect.any(Object),
            updatedAt: expect.any(Object),
            author: expect.any(Object),
            categorieses: expect.any(Object),
            locationses: expect.any(Object),
          }),
        );
      });
    });

    describe('getListing', () => {
      it('should return NewListing for default Listing initial value', () => {
        const formGroup = service.createListingFormGroup(sampleWithNewData);

        const listing = service.getListing(formGroup);

        expect(listing).toMatchObject(sampleWithNewData);
      });

      it('should return NewListing for empty Listing initial value', () => {
        const formGroup = service.createListingFormGroup();

        const listing = service.getListing(formGroup);

        expect(listing).toMatchObject({});
      });

      it('should return IListing', () => {
        const formGroup = service.createListingFormGroup(sampleWithRequiredData);

        const listing = service.getListing(formGroup);

        expect(listing).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IListing should not enable id FormControl', () => {
        const formGroup = service.createListingFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewListing should disable id FormControl', () => {
        const formGroup = service.createListingFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
