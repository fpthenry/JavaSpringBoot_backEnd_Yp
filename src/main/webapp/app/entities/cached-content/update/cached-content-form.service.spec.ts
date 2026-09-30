import { beforeEach, describe, expect, it } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../cached-content.test-samples';

import { CachedContentFormService } from './cached-content-form.service';

describe('CachedContent Form Service', () => {
  let service: CachedContentFormService;

  beforeEach(() => {
    service = TestBed.inject(CachedContentFormService);
  });

  describe('Service methods', () => {
    describe('createCachedContentFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createCachedContentFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            termSlug: expect.any(Object),
            expired: expect.any(Object),
            content: expect.any(Object),
          }),
        );
      });

      it('passing ICachedContent should create a new form with FormGroup', () => {
        const formGroup = service.createCachedContentFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            termSlug: expect.any(Object),
            expired: expect.any(Object),
            content: expect.any(Object),
          }),
        );
      });
    });

    describe('getCachedContent', () => {
      it('should return NewCachedContent for default CachedContent initial value', () => {
        const formGroup = service.createCachedContentFormGroup(sampleWithNewData);

        const cachedContent = service.getCachedContent(formGroup);

        expect(cachedContent).toMatchObject(sampleWithNewData);
      });

      it('should return NewCachedContent for empty CachedContent initial value', () => {
        const formGroup = service.createCachedContentFormGroup();

        const cachedContent = service.getCachedContent(formGroup);

        expect(cachedContent).toMatchObject({});
      });

      it('should return ICachedContent', () => {
        const formGroup = service.createCachedContentFormGroup(sampleWithRequiredData);

        const cachedContent = service.getCachedContent(formGroup);

        expect(cachedContent).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing ICachedContent should not enable id FormControl', () => {
        const formGroup = service.createCachedContentFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewCachedContent should disable id FormControl', () => {
        const formGroup = service.createCachedContentFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
