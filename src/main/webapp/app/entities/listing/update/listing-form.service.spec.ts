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
            wpId: expect.any(Object),
            apiId: expect.any(Object),
            name: expect.any(Object),
            nameEn: expect.any(Object),
            nameAlias: expect.any(Object),
            slug: expect.any(Object),
            description: expect.any(Object),
            phone: expect.any(Object),
            mobile: expect.any(Object),
            email: expect.any(Object),
            website: expect.any(Object),
            address: expect.any(Object),
            locationJson: expect.any(Object),
            taxCode: expect.any(Object),
            representative: expect.any(Object),
            capital: expect.any(Object),
            foundedYear: expect.any(Object),
            businessType: expect.any(Object),
            businessStatus: expect.any(Object),
            industryCode: expect.any(Object),
            managedBy: expect.any(Object),
            thumbnail: expect.any(Object),
            images: expect.any(Object),
            viewCount: expect.any(Object),
            isFeatured: expect.any(Object),
            status: expect.any(Object),
            publishedAt: expect.any(Object),
            modifiedAt: expect.any(Object),
            createdAt: expect.any(Object),
            updatedAt: expect.any(Object),
            esIndexed: expect.any(Object),
            categories: expect.any(Object),
            locations: expect.any(Object),
          }),
        );
      });

      it('passing IListing should create a new form with FormGroup', () => {
        const formGroup = service.createListingFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            wpId: expect.any(Object),
            apiId: expect.any(Object),
            name: expect.any(Object),
            nameEn: expect.any(Object),
            nameAlias: expect.any(Object),
            slug: expect.any(Object),
            description: expect.any(Object),
            phone: expect.any(Object),
            mobile: expect.any(Object),
            email: expect.any(Object),
            website: expect.any(Object),
            address: expect.any(Object),
            locationJson: expect.any(Object),
            taxCode: expect.any(Object),
            representative: expect.any(Object),
            capital: expect.any(Object),
            foundedYear: expect.any(Object),
            businessType: expect.any(Object),
            businessStatus: expect.any(Object),
            industryCode: expect.any(Object),
            managedBy: expect.any(Object),
            thumbnail: expect.any(Object),
            images: expect.any(Object),
            viewCount: expect.any(Object),
            isFeatured: expect.any(Object),
            status: expect.any(Object),
            publishedAt: expect.any(Object),
            modifiedAt: expect.any(Object),
            createdAt: expect.any(Object),
            updatedAt: expect.any(Object),
            esIndexed: expect.any(Object),
            categories: expect.any(Object),
            locations: expect.any(Object),
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
