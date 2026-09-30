import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpResponse } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { Subject, from, of } from 'rxjs';

import { IGallery } from 'app/entities/gallery/gallery.model';
import { GalleryService } from 'app/entities/gallery/service/gallery.service';
import { IListing } from 'app/entities/listing/listing.model';
import { ListingService } from 'app/entities/listing/service/listing.service';
import { IListingImage } from '../listing-image.model';
import { ListingImageService } from '../service/listing-image.service';

import { ListingImageFormService } from './listing-image-form.service';
import { ListingImageUpdate } from './listing-image-update';

describe('ListingImage Management Update Component', () => {
  let comp: ListingImageUpdate;
  let fixture: ComponentFixture<ListingImageUpdate>;
  let activatedRoute: ActivatedRoute;
  let listingImageFormService: ListingImageFormService;
  let listingImageService: ListingImageService;
  let listingService: ListingService;
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

    fixture = TestBed.createComponent(ListingImageUpdate);
    activatedRoute = TestBed.inject(ActivatedRoute);
    listingImageFormService = TestBed.inject(ListingImageFormService);
    listingImageService = TestBed.inject(ListingImageService);
    listingService = TestBed.inject(ListingService);
    galleryService = TestBed.inject(GalleryService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call Listing query and add missing value', () => {
      const listingImage: IListingImage = { id: 30153 };
      const listing: IListing = { id: 14276 };
      listingImage.listing = listing;

      const listingCollection: IListing[] = [{ id: 14276 }];
      vi.spyOn(listingService, 'query').mockReturnValue(of(new HttpResponse({ body: listingCollection })));
      const additionalListings = [listing];
      const expectedCollection: IListing[] = [...additionalListings, ...listingCollection];
      vi.spyOn(listingService, 'addListingToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ listingImage });
      comp.ngOnInit();

      expect(listingService.query).toHaveBeenCalled();
      expect(listingService.addListingToCollectionIfMissing).toHaveBeenCalledWith(
        listingCollection,
        ...additionalListings.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.listingsSharedCollection()).toEqual(expectedCollection);
    });

    it('should call Gallery query and add missing value', () => {
      const listingImage: IListingImage = { id: 30153 };
      const gallery: IGallery = { id: 16173 };
      listingImage.gallery = gallery;

      const galleryCollection: IGallery[] = [{ id: 16173 }];
      vi.spyOn(galleryService, 'query').mockReturnValue(of(new HttpResponse({ body: galleryCollection })));
      const additionalGalleries = [gallery];
      const expectedCollection: IGallery[] = [...additionalGalleries, ...galleryCollection];
      vi.spyOn(galleryService, 'addGalleryToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ listingImage });
      comp.ngOnInit();

      expect(galleryService.query).toHaveBeenCalled();
      expect(galleryService.addGalleryToCollectionIfMissing).toHaveBeenCalledWith(
        galleryCollection,
        ...additionalGalleries.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.galleriesSharedCollection()).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const listingImage: IListingImage = { id: 30153 };
      const listing: IListing = { id: 14276 };
      listingImage.listing = listing;
      const gallery: IGallery = { id: 16173 };
      listingImage.gallery = gallery;

      activatedRoute.data = of({ listingImage });
      comp.ngOnInit();

      expect(comp.listingsSharedCollection()).toContainEqual(listing);
      expect(comp.galleriesSharedCollection()).toContainEqual(gallery);
      expect(comp.listingImage).toEqual(listingImage);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<IListingImage>();
      const listingImage = { id: 8129 };
      vi.spyOn(listingImageFormService, 'getListingImage').mockReturnValue(listingImage);
      vi.spyOn(listingImageService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ listingImage });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(listingImage);
      saveSubject.complete();

      // THEN
      expect(listingImageFormService.getListingImage).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(listingImageService.update).toHaveBeenCalledWith(expect.objectContaining(listingImage));
      expect(comp.isSaving()).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<IListingImage>();
      const listingImage = { id: 8129 };
      vi.spyOn(listingImageFormService, 'getListingImage').mockReturnValue({ id: null });
      vi.spyOn(listingImageService, 'create').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ listingImage: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(listingImage);
      saveSubject.complete();

      // THEN
      expect(listingImageFormService.getListingImage).toHaveBeenCalled();
      expect(listingImageService.create).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<IListingImage>();
      const listingImage = { id: 8129 };
      vi.spyOn(listingImageService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ listingImage });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(listingImageService.update).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareListing', () => {
      it('should forward to listingService', () => {
        const entity = { id: 14276 };
        const entity2 = { id: 86 };
        vi.spyOn(listingService, 'compareListing');
        comp.compareListing(entity, entity2);
        expect(listingService.compareListing).toHaveBeenCalledWith(entity, entity2);
      });
    });

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
