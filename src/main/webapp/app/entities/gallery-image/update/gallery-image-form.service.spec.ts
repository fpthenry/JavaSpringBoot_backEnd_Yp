import { beforeEach, describe, expect, it } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../gallery-image.test-samples';

import { GalleryImageFormService } from './gallery-image-form.service';

describe('GalleryImage Form Service', () => {
  let service: GalleryImageFormService;

  beforeEach(() => {
    service = TestBed.inject(GalleryImageFormService);
  });

  describe('Service methods', () => {
    describe('createGalleryImageFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createGalleryImageFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            title: expect.any(Object),
            image: expect.any(Object),
            imageUrl: expect.any(Object),
            linkUrl: expect.any(Object),
            altText: expect.any(Object),
            displayOrder: expect.any(Object),
            active: expect.any(Object),
            startAt: expect.any(Object),
            endAt: expect.any(Object),
            openInNewTab: expect.any(Object),
            gallery: expect.any(Object),
          }),
        );
      });

      it('passing IGalleryImage should create a new form with FormGroup', () => {
        const formGroup = service.createGalleryImageFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            title: expect.any(Object),
            image: expect.any(Object),
            imageUrl: expect.any(Object),
            linkUrl: expect.any(Object),
            altText: expect.any(Object),
            displayOrder: expect.any(Object),
            active: expect.any(Object),
            startAt: expect.any(Object),
            endAt: expect.any(Object),
            openInNewTab: expect.any(Object),
            gallery: expect.any(Object),
          }),
        );
      });
    });

    describe('getGalleryImage', () => {
      it('should return NewGalleryImage for default GalleryImage initial value', () => {
        const formGroup = service.createGalleryImageFormGroup(sampleWithNewData);

        const galleryImage = service.getGalleryImage(formGroup);

        expect(galleryImage).toMatchObject(sampleWithNewData);
      });

      it('should return NewGalleryImage for empty GalleryImage initial value', () => {
        const formGroup = service.createGalleryImageFormGroup();

        const galleryImage = service.getGalleryImage(formGroup);

        expect(galleryImage).toMatchObject({});
      });

      it('should return IGalleryImage', () => {
        const formGroup = service.createGalleryImageFormGroup(sampleWithRequiredData);

        const galleryImage = service.getGalleryImage(formGroup);

        expect(galleryImage).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IGalleryImage should not enable id FormControl', () => {
        const formGroup = service.createGalleryImageFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewGalleryImage should disable id FormControl', () => {
        const formGroup = service.createGalleryImageFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
