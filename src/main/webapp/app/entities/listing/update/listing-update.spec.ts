import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpResponse } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { Subject, from, of } from 'rxjs';

import { ICategory } from 'app/entities/category/category.model';
import { CategoryService } from 'app/entities/category/service/category.service';
import { ILocation } from 'app/entities/location/location.model';
import { LocationService } from 'app/entities/location/service/location.service';
import { UserService } from 'app/entities/user/service/user.service';
import { IUser } from 'app/entities/user/user.model';
import { IListing } from '../listing.model';
import { ListingService } from '../service/listing.service';

import { ListingFormService } from './listing-form.service';
import { ListingUpdate } from './listing-update';

describe('Listing Management Update Component', () => {
  let comp: ListingUpdate;
  let fixture: ComponentFixture<ListingUpdate>;
  let activatedRoute: ActivatedRoute;
  let listingFormService: ListingFormService;
  let listingService: ListingService;
  let userService: UserService;
  let categoryService: CategoryService;
  let locationService: LocationService;

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

    fixture = TestBed.createComponent(ListingUpdate);
    activatedRoute = TestBed.inject(ActivatedRoute);
    listingFormService = TestBed.inject(ListingFormService);
    listingService = TestBed.inject(ListingService);
    userService = TestBed.inject(UserService);
    categoryService = TestBed.inject(CategoryService);
    locationService = TestBed.inject(LocationService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call User query and add missing value', () => {
      const listing: IListing = { id: 86 };
      const author: IUser = { id: 3944 };
      listing.author = author;

      const userCollection: IUser[] = [{ id: 3944 }];
      vi.spyOn(userService, 'query').mockReturnValue(of(new HttpResponse({ body: userCollection })));
      const additionalUsers = [author];
      const expectedCollection: IUser[] = [...additionalUsers, ...userCollection];
      vi.spyOn(userService, 'addUserToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ listing });
      comp.ngOnInit();

      expect(userService.query).toHaveBeenCalled();
      expect(userService.addUserToCollectionIfMissing).toHaveBeenCalledWith(
        userCollection,
        ...additionalUsers.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.usersSharedCollection()).toEqual(expectedCollection);
    });

    it('should call Category query and add missing value', () => {
      const listing: IListing = { id: 86 };
      const categorieses: ICategory[] = [{ id: 6752 }];
      listing.categorieses = categorieses;

      const categoryCollection: ICategory[] = [{ id: 6752 }];
      vi.spyOn(categoryService, 'query').mockReturnValue(of(new HttpResponse({ body: categoryCollection })));
      const additionalCategories = [...categorieses];
      const expectedCollection: ICategory[] = [...additionalCategories, ...categoryCollection];
      vi.spyOn(categoryService, 'addCategoryToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ listing });
      comp.ngOnInit();

      expect(categoryService.query).toHaveBeenCalled();
      expect(categoryService.addCategoryToCollectionIfMissing).toHaveBeenCalledWith(
        categoryCollection,
        ...additionalCategories.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.categoriesSharedCollection()).toEqual(expectedCollection);
    });

    it('should call Location query and add missing value', () => {
      const listing: IListing = { id: 86 };
      const locationses: ILocation[] = [{ id: 8454 }];
      listing.locationses = locationses;

      const locationCollection: ILocation[] = [{ id: 8454 }];
      vi.spyOn(locationService, 'query').mockReturnValue(of(new HttpResponse({ body: locationCollection })));
      const additionalLocations = [...locationses];
      const expectedCollection: ILocation[] = [...additionalLocations, ...locationCollection];
      vi.spyOn(locationService, 'addLocationToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ listing });
      comp.ngOnInit();

      expect(locationService.query).toHaveBeenCalled();
      expect(locationService.addLocationToCollectionIfMissing).toHaveBeenCalledWith(
        locationCollection,
        ...additionalLocations.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.locationsSharedCollection()).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const listing: IListing = { id: 86 };
      const author: IUser = { id: 3944 };
      listing.author = author;
      const categories: ICategory = { id: 6752 };
      listing.categorieses = [categories];
      const locations: ILocation = { id: 8454 };
      listing.locationses = [locations];

      activatedRoute.data = of({ listing });
      comp.ngOnInit();

      expect(comp.usersSharedCollection()).toContainEqual(author);
      expect(comp.categoriesSharedCollection()).toContainEqual(categories);
      expect(comp.locationsSharedCollection()).toContainEqual(locations);
      expect(comp.listing).toEqual(listing);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<IListing>();
      const listing = { id: 14276 };
      vi.spyOn(listingFormService, 'getListing').mockReturnValue(listing);
      vi.spyOn(listingService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ listing });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(listing);
      saveSubject.complete();

      // THEN
      expect(listingFormService.getListing).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(listingService.update).toHaveBeenCalledWith(expect.objectContaining(listing));
      expect(comp.isSaving()).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<IListing>();
      const listing = { id: 14276 };
      vi.spyOn(listingFormService, 'getListing').mockReturnValue({ id: null });
      vi.spyOn(listingService, 'create').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ listing: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(listing);
      saveSubject.complete();

      // THEN
      expect(listingFormService.getListing).toHaveBeenCalled();
      expect(listingService.create).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<IListing>();
      const listing = { id: 14276 };
      vi.spyOn(listingService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ listing });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(listingService.update).toHaveBeenCalled();
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

    describe('compareCategory', () => {
      it('should forward to categoryService', () => {
        const entity = { id: 6752 };
        const entity2 = { id: 4374 };
        vi.spyOn(categoryService, 'compareCategory');
        comp.compareCategory(entity, entity2);
        expect(categoryService.compareCategory).toHaveBeenCalledWith(entity, entity2);
      });
    });

    describe('compareLocation', () => {
      it('should forward to locationService', () => {
        const entity = { id: 8454 };
        const entity2 = { id: 13013 };
        vi.spyOn(locationService, 'compareLocation');
        comp.compareLocation(entity, entity2);
        expect(locationService.compareLocation).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
