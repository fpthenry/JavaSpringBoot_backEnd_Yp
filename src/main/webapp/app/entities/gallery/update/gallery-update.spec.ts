import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpResponse } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { Subject, from, of } from 'rxjs';

import { IListing } from 'app/entities/listing/listing.model';
import { ListingService } from 'app/entities/listing/service/listing.service';
import { UserService } from 'app/entities/user/service/user.service';
import { IUser } from 'app/entities/user/user.model';
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
  let userService: UserService;
  let listingService: ListingService;

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
    userService = TestBed.inject(UserService);
    listingService = TestBed.inject(ListingService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call User query and add missing value', () => {
      const gallery: IGallery = { id: 7807 };
      const author: IUser = { id: 3944 };
      gallery.author = author;

      const userCollection: IUser[] = [{ id: 3944 }];
      vi.spyOn(userService, 'query').mockReturnValue(of(new HttpResponse({ body: userCollection })));
      const additionalUsers = [author];
      const expectedCollection: IUser[] = [...additionalUsers, ...userCollection];
      vi.spyOn(userService, 'addUserToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ gallery });
      comp.ngOnInit();

      expect(userService.query).toHaveBeenCalled();
      expect(userService.addUserToCollectionIfMissing).toHaveBeenCalledWith(
        userCollection,
        ...additionalUsers.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.usersSharedCollection()).toEqual(expectedCollection);
    });

    it('should call Listing query and add missing value', () => {
      const gallery: IGallery = { id: 7807 };
      const listing: IListing = { id: 14276 };
      gallery.listing = listing;

      const listingCollection: IListing[] = [{ id: 14276 }];
      vi.spyOn(listingService, 'query').mockReturnValue(of(new HttpResponse({ body: listingCollection })));
      const additionalListings = [listing];
      const expectedCollection: IListing[] = [...additionalListings, ...listingCollection];
      vi.spyOn(listingService, 'addListingToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ gallery });
      comp.ngOnInit();

      expect(listingService.query).toHaveBeenCalled();
      expect(listingService.addListingToCollectionIfMissing).toHaveBeenCalledWith(
        listingCollection,
        ...additionalListings.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.listingsSharedCollection()).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const gallery: IGallery = { id: 7807 };
      const author: IUser = { id: 3944 };
      gallery.author = author;
      const listing: IListing = { id: 14276 };
      gallery.listing = listing;

      activatedRoute.data = of({ gallery });
      comp.ngOnInit();

      expect(comp.usersSharedCollection()).toContainEqual(author);
      expect(comp.listingsSharedCollection()).toContainEqual(listing);
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

  describe('Compare relationships', () => {
    describe('compareUser', () => {
      it('should forward to userService', () => {
        const entity = { id: 3944 };
        const entity2 = { id: 6275 };
        vi.spyOn(userService, 'compareUser');
        comp.compareUser(entity, entity2);
        expect(userService.compareUser).toHaveBeenCalledWith(entity, entity2);
      });
    });

    describe('compareListing', () => {
      it('should forward to listingService', () => {
        const entity = { id: 14276 };
        const entity2 = { id: 86 };
        vi.spyOn(listingService, 'compareListing');
        comp.compareListing(entity, entity2);
        expect(listingService.compareListing).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
