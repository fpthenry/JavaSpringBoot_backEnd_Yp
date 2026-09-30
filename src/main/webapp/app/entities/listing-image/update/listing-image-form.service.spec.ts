import { beforeEach, describe, expect, it } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../listing-image.test-samples';

import { ListingImageFormService } from './listing-image-form.service';

describe('ListingImage Form Service', () => {
  let service: ListingImageFormService;

  beforeEach(() => {
    service = TestBed.inject(ListingImageFormService);
  });

  describe('Service methods', () => {
    describe('createListingImageFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createListingImageFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            imageUrl: expect.any(Object),
            thumbnailUrl: expect.any(Object),
            altText: expect.any(Object),
            displayOrder: expect.any(Object),
            isFeatured: expect.any(Object),
            createdAt: expect.any(Object),
            updatedAt: expect.any(Object),
            listing: expect.any(Object),
            gallery: expect.any(Object),
          }),
        );
      });

      it('passing IListingImage should create a new form with FormGroup', () => {
        const formGroup = service.createListingImageFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            imageUrl: expect.any(Object),
            thumbnailUrl: expect.any(Object),
            altText: expect.any(Object),
            displayOrder: expect.any(Object),
            isFeatured: expect.any(Object),
            createdAt: expect.any(Object),
            updatedAt: expect.any(Object),
            listing: expect.any(Object),
            gallery: expect.any(Object),
          }),
        );
      });
    });

    describe('getListingImage', () => {
      it('should return NewListingImage for default ListingImage initial value', () => {
        const formGroup = service.createListingImageFormGroup(sampleWithNewData);

        const listingImage = service.getListingImage(formGroup);

        expect(listingImage).toMatchObject(sampleWithNewData);
      });

      it('should return NewListingImage for empty ListingImage initial value', () => {
        const formGroup = service.createListingImageFormGroup();

        const listingImage = service.getListingImage(formGroup);

        expect(listingImage).toMatchObject({});
      });

      it('should return IListingImage', () => {
        const formGroup = service.createListingImageFormGroup(sampleWithRequiredData);

        const listingImage = service.getListingImage(formGroup);

        expect(listingImage).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IListingImage should not enable id FormControl', () => {
        const formGroup = service.createListingImageFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewListingImage should disable id FormControl', () => {
        const formGroup = service.createListingImageFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
