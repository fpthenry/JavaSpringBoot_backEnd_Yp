import { beforeEach, describe, expect, it, vi } from 'vitest';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { Subject, from, of } from 'rxjs';

import { IGallery } from '../gallery.model';
import { GalleryService } from '../service/gallery.service';

import { GalleryFormService } from './gallery-form.service';
import { GalleryUpdate } from './gallery-update';

describe('Gallery Management Update Component', () => {
  let comp: GalleryUpdate;
  let fixture: ComponentFixture<GalleryUpdate>;
  let activatedRoute: ActivatedRoute;
  let galleryFormService: GalleryFormService;
  let galleryService: GalleryService;

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

    fixture = TestBed.createComponent(GalleryUpdate);
    activatedRoute = TestBed.inject(ActivatedRoute);
    galleryFormService = TestBed.inject(GalleryFormService);
    galleryService = TestBed.inject(GalleryService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should update editForm', () => {
      const gallery: IGallery = { id: 7807 };

      activatedRoute.data = of({ gallery });
      comp.ngOnInit();

      expect(comp.gallery).toEqual(gallery);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<IGallery>();
      const gallery = { id: 16173 };
      vi.spyOn(galleryFormService, 'getGallery').mockReturnValue(gallery);
      vi.spyOn(galleryService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ gallery });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(gallery);
      saveSubject.complete();

      // THEN
      expect(galleryFormService.getGallery).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(galleryService.update).toHaveBeenCalledWith(expect.objectContaining(gallery));
      expect(comp.isSaving()).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<IGallery>();
      const gallery = { id: 16173 };
      vi.spyOn(galleryFormService, 'getGallery').mockReturnValue({ id: null });
      vi.spyOn(galleryService, 'create').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ gallery: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(gallery);
      saveSubject.complete();

      // THEN
      expect(galleryFormService.getGallery).toHaveBeenCalled();
      expect(galleryService.create).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<IGallery>();
      const gallery = { id: 16173 };
      vi.spyOn(galleryService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ gallery });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(galleryService.update).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });
});
