import { beforeEach, describe, expect, it, vi } from 'vitest';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { Subject, from, of } from 'rxjs';

import { ICachedContent } from '../cached-content.model';
import { CachedContentService } from '../service/cached-content.service';

import { CachedContentFormService } from './cached-content-form.service';
import { CachedContentUpdate } from './cached-content-update';

describe('CachedContent Management Update Component', () => {
  let comp: CachedContentUpdate;
  let fixture: ComponentFixture<CachedContentUpdate>;
  let activatedRoute: ActivatedRoute;
  let cachedContentFormService: CachedContentFormService;
  let cachedContentService: CachedContentService;

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

    fixture = TestBed.createComponent(CachedContentUpdate);
    activatedRoute = TestBed.inject(ActivatedRoute);
    cachedContentFormService = TestBed.inject(CachedContentFormService);
    cachedContentService = TestBed.inject(CachedContentService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should update editForm', () => {
      const cachedContent: ICachedContent = { id: 31525 };

      activatedRoute.data = of({ cachedContent });
      comp.ngOnInit();

      expect(comp.cachedContent).toEqual(cachedContent);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<ICachedContent>();
      const cachedContent = { id: 5503 };
      vi.spyOn(cachedContentFormService, 'getCachedContent').mockReturnValue(cachedContent);
      vi.spyOn(cachedContentService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ cachedContent });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(cachedContent);
      saveSubject.complete();

      // THEN
      expect(cachedContentFormService.getCachedContent).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(cachedContentService.update).toHaveBeenCalledWith(expect.objectContaining(cachedContent));
      expect(comp.isSaving()).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<ICachedContent>();
      const cachedContent = { id: 5503 };
      vi.spyOn(cachedContentFormService, 'getCachedContent').mockReturnValue({ id: null });
      vi.spyOn(cachedContentService, 'create').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ cachedContent: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(cachedContent);
      saveSubject.complete();

      // THEN
      expect(cachedContentFormService.getCachedContent).toHaveBeenCalled();
      expect(cachedContentService.create).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<ICachedContent>();
      const cachedContent = { id: 5503 };
      vi.spyOn(cachedContentService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ cachedContent });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(cachedContentService.update).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });
});
