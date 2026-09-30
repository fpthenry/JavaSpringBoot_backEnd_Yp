import { beforeEach, describe, expect, it } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../static-page.test-samples';

import { StaticPageFormService } from './static-page-form.service';

describe('StaticPage Form Service', () => {
  let service: StaticPageFormService;

  beforeEach(() => {
    service = TestBed.inject(StaticPageFormService);
  });

  describe('Service methods', () => {
    describe('createStaticPageFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createStaticPageFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            title: expect.any(Object),
            slug: expect.any(Object),
            content: expect.any(Object),
            status: expect.any(Object),
            template: expect.any(Object),
            menuOrder: expect.any(Object),
            createdAt: expect.any(Object),
            updatedAt: expect.any(Object),
            author: expect.any(Object),
            parent: expect.any(Object),
          }),
        );
      });

      it('passing IStaticPage should create a new form with FormGroup', () => {
        const formGroup = service.createStaticPageFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            title: expect.any(Object),
            slug: expect.any(Object),
            content: expect.any(Object),
            status: expect.any(Object),
            template: expect.any(Object),
            menuOrder: expect.any(Object),
            createdAt: expect.any(Object),
            updatedAt: expect.any(Object),
            author: expect.any(Object),
            parent: expect.any(Object),
          }),
        );
      });
    });

    describe('getStaticPage', () => {
      it('should return NewStaticPage for default StaticPage initial value', () => {
        const formGroup = service.createStaticPageFormGroup(sampleWithNewData);

        const staticPage = service.getStaticPage(formGroup);

        expect(staticPage).toMatchObject(sampleWithNewData);
      });

      it('should return NewStaticPage for empty StaticPage initial value', () => {
        const formGroup = service.createStaticPageFormGroup();

        const staticPage = service.getStaticPage(formGroup);

        expect(staticPage).toMatchObject({});
      });

      it('should return IStaticPage', () => {
        const formGroup = service.createStaticPageFormGroup(sampleWithRequiredData);

        const staticPage = service.getStaticPage(formGroup);

        expect(staticPage).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IStaticPage should not enable id FormControl', () => {
        const formGroup = service.createStaticPageFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewStaticPage should disable id FormControl', () => {
        const formGroup = service.createStaticPageFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
