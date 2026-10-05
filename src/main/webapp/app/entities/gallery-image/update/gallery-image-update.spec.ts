import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpResponse } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { Subject, from, of } from 'rxjs';

import { IGallery } from 'app/entities/gallery/gallery.model';
import { GalleryService } from 'app/entities/gallery/service/gallery.service';
import { IGalleryImage } from '../gallery-image.model';
import { GalleryImageService } from '../service/gallery-image.service';

import { GalleryImageFormService } from './gallery-image-form.service';
import { GalleryImageUpdate } from './gallery-image-update';

describe('GalleryImage Management Update Component', () => {
  let comp: GalleryImageUpdate;
  let fixture: ComponentFixture<GalleryImageUpdate>;
  let activatedRoute: ActivatedRoute;
  let galleryImageFormService: GalleryImageFormService;
  let galleryImageService: GalleryImageService;
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

    fixture = TestBed.createComponent(GalleryImageUpdate);
    activatedRoute = TestBed.inject(ActivatedRoute);
    galleryImageFormService = TestBed.inject(GalleryImageFormService);
    galleryImageService = TestBed.inject(GalleryImageService);
    galleryService = TestBed.inject(GalleryService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call Gallery query and add missing value', () => {
      const galleryImage: IGalleryImage = { id: 4556 };
      const gallery: IGallery = { id: 16173 };
      galleryImage.gallery = gallery;

      const galleryCollection: IGallery[] = [{ id: 16173 }];
      vi.spyOn(galleryService, 'query').mockReturnValue(of(new HttpResponse({ body: galleryCollection })));
      const additionalGalleries = [gallery];
      const expectedCollection: IGallery[] = [...additionalGalleries, ...galleryCollection];
      vi.spyOn(galleryService, 'addGalleryToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ galleryImage });
      comp.ngOnInit();

      expect(galleryService.query).toHaveBeenCalled();
      expect(galleryService.addGalleryToCollectionIfMissing).toHaveBeenCalledWith(
        galleryCollection,
        ...additionalGalleries.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.galleriesSharedCollection()).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const galleryImage: IGalleryImage = { id: 4556 };
      const gallery: IGallery = { id: 16173 };
      galleryImage.gallery = gallery;

      activatedRoute.data = of({ galleryImage });
      comp.ngOnInit();

      expect(comp.galleriesSharedCollection()).toContainEqual(gallery);
      expect(comp.galleryImage).toEqual(galleryImage);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<IGalleryImage>();
      const galleryImage = { id: 21370 };
      vi.spyOn(galleryImageFormService, 'getGalleryImage').mockReturnValue(galleryImage);
      vi.spyOn(galleryImageService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ galleryImage });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(galleryImage);
      saveSubject.complete();

      // THEN
      expect(galleryImageFormService.getGalleryImage).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(galleryImageService.update).toHaveBeenCalledWith(expect.objectContaining(galleryImage));
      expect(comp.isSaving()).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<IGalleryImage>();
      const galleryImage = { id: 21370 };
      vi.spyOn(galleryImageFormService, 'getGalleryImage').mockReturnValue({ id: null });
      vi.spyOn(galleryImageService, 'create').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ galleryImage: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(galleryImage);
      saveSubject.complete();

      // THEN
      expect(galleryImageFormService.getGalleryImage).toHaveBeenCalled();
      expect(galleryImageService.create).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<IGalleryImage>();
      const galleryImage = { id: 21370 };
      vi.spyOn(galleryImageService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ galleryImage });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(galleryImageService.update).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareGallery', () => {
      it('should forward to galleryService', () => {
        const entity = { id: 16173 };
        const entity2 = { id: 7807 };
        vi.spyOn(galleryService, 'compareGallery');
        comp.compareGallery(entity, entity2);
        expect(galleryService.compareGallery).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
