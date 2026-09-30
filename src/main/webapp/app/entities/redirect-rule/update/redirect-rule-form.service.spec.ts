import { beforeEach, describe, expect, it } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../redirect-rule.test-samples';

import { RedirectRuleFormService } from './redirect-rule-form.service';

describe('RedirectRule Form Service', () => {
  let service: RedirectRuleFormService;

  beforeEach(() => {
    service = TestBed.inject(RedirectRuleFormService);
  });

  describe('Service methods', () => {
    describe('createRedirectRuleFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createRedirectRuleFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            sourceId: expect.any(Object),
            sourceSlug: expect.any(Object),
            destinationId: expect.any(Object),
            destinationSlug: expect.any(Object),
            objectType: expect.any(Object),
          }),
        );
      });

      it('passing IRedirectRule should create a new form with FormGroup', () => {
        const formGroup = service.createRedirectRuleFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            sourceId: expect.any(Object),
            sourceSlug: expect.any(Object),
            destinationId: expect.any(Object),
            destinationSlug: expect.any(Object),
            objectType: expect.any(Object),
          }),
        );
      });
    });

    describe('getRedirectRule', () => {
      it('should return NewRedirectRule for default RedirectRule initial value', () => {
        const formGroup = service.createRedirectRuleFormGroup(sampleWithNewData);

        const redirectRule = service.getRedirectRule(formGroup);

        expect(redirectRule).toMatchObject(sampleWithNewData);
      });

      it('should return NewRedirectRule for empty RedirectRule initial value', () => {
        const formGroup = service.createRedirectRuleFormGroup();

        const redirectRule = service.getRedirectRule(formGroup);

        expect(redirectRule).toMatchObject({});
      });

      it('should return IRedirectRule', () => {
        const formGroup = service.createRedirectRuleFormGroup(sampleWithRequiredData);

        const redirectRule = service.getRedirectRule(formGroup);

        expect(redirectRule).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IRedirectRule should not enable id FormControl', () => {
        const formGroup = service.createRedirectRuleFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewRedirectRule should disable id FormControl', () => {
        const formGroup = service.createRedirectRuleFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
