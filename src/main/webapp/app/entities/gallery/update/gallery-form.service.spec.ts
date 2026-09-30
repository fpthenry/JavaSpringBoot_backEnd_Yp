import { beforeEach, describe, expect, it } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../gallery.test-samples';

import { GalleryFormService } from './gallery-form.service';

describe('Gallery Form Service', () => {
  let service: GalleryFormService;

  beforeEach(() => {
    service = TestBed.inject(GalleryFormService);
  });

  describe('Service methods', () => {
    describe('createGalleryFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createGalleryFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            title: expect.any(Object),
            slug: expect.any(Object),
            content: expect.any(Object),
            status: expect.any(Object),
            createdAt: expect.any(Object),
            updatedAt: expect.any(Object),
            author: expect.any(Object),
            listing: expect.any(Object),
          }),
        );
      });

      it('passing IGallery should create a new form with FormGroup', () => {
        const formGroup = service.createGalleryFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            title: expect.any(Object),
            slug: expect.any(Object),
            content: expect.any(Object),
            status: expect.any(Object),
            createdAt: expect.any(Object),
            updatedAt: expect.any(Object),
            author: expect.any(Object),
            listing: expect.any(Object),
          }),
        );
      });
    });

    describe('getGallery', () => {
      it('should return NewGallery for default Gallery initial value', () => {
        const formGroup = service.createGalleryFormGroup(sampleWithNewData);

        const gallery = service.getGallery(formGroup);

        expect(gallery).toMatchObject(sampleWithNewData);
      });

      it('should return NewGallery for empty Gallery initial value', () => {
        const formGroup = service.createGalleryFormGroup();

        const gallery = service.getGallery(formGroup);

        expect(gallery).toMatchObject({});
      });

      it('should return IGallery', () => {
        const formGroup = service.createGalleryFormGroup(sampleWithRequiredData);

        const gallery = service.getGallery(formGroup);

        expect(gallery).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IGallery should not enable id FormControl', () => {
        const formGroup = service.createGalleryFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewGallery should disable id FormControl', () => {
        const formGroup = service.createGalleryFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
