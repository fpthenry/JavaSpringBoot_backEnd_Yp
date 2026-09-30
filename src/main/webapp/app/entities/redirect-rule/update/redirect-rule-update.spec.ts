import { beforeEach, describe, expect, it, vi } from 'vitest';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { Subject, from, of } from 'rxjs';

import { IRedirectRule } from '../redirect-rule.model';
import { RedirectRuleService } from '../service/redirect-rule.service';

import { RedirectRuleFormService } from './redirect-rule-form.service';
import { RedirectRuleUpdate } from './redirect-rule-update';

describe('RedirectRule Management Update Component', () => {
  let comp: RedirectRuleUpdate;
  let fixture: ComponentFixture<RedirectRuleUpdate>;
  let activatedRoute: ActivatedRoute;
  let redirectRuleFormService: RedirectRuleFormService;
  let redirectRuleService: RedirectRuleService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            params: from([{}]),
          },
        },
      ],
    });

    fixture = TestBed.createComponent(RedirectRuleUpdate);
    activatedRoute = TestBed.inject(ActivatedRoute);
    redirectRuleFormService = TestBed.inject(RedirectRuleFormService);
    redirectRuleService = TestBed.inject(RedirectRuleService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should update editForm', () => {
      const redirectRule: IRedirectRule = { id: 31360 };

      activatedRoute.data = of({ redirectRule });
      comp.ngOnInit();

      expect(comp.redirectRule).toEqual(redirectRule);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<IRedirectRule>();
      const redirectRule = { id: 31397 };
      vi.spyOn(redirectRuleFormService, 'getRedirectRule').mockReturnValue(redirectRule);
      vi.spyOn(redirectRuleService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ redirectRule });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(redirectRule);
      saveSubject.complete();

      // THEN
      expect(redirectRuleFormService.getRedirectRule).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(redirectRuleService.update).toHaveBeenCalledWith(expect.objectContaining(redirectRule));
      expect(comp.isSaving()).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<IRedirectRule>();
      const redirectRule = { id: 31397 };
      vi.spyOn(redirectRuleFormService, 'getRedirectRule').mockReturnValue({ id: null });
      vi.spyOn(redirectRuleService, 'create').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ redirectRule: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(redirectRule);
      saveSubject.complete();

      // THEN
      expect(redirectRuleFormService.getRedirectRule).toHaveBeenCalled();
      expect(redirectRuleService.create).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<IRedirectRule>();
      const redirectRule = { id: 31397 };
      vi.spyOn(redirectRuleService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ redirectRule });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(redirectRuleService.update).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });
});
