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
    categoryService = TestBed.inject(CategoryService);
    locationService = TestBed.inject(LocationService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call Category query and add missing value', () => {
      const listing: IListing = { id: 86 };
      const categories: ICategory[] = [{ id: 6752 }];
      listing.categories = categories;

      const categoryCollection: ICategory[] = [{ id: 6752 }];
      vi.spyOn(categoryService, 'query').mockReturnValue(of(new HttpResponse({ body: categoryCollection })));
      const additionalCategories = [...categories];
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
      const locations: ILocation[] = [{ id: 8454 }];
      listing.locations = locations;

      const locationCollection: ILocation[] = [{ id: 8454 }];
      vi.spyOn(locationService, 'query').mockReturnValue(of(new HttpResponse({ body: locationCollection })));
      const additionalLocations = [...locations];
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
      const category: ICategory = { id: 6752 };
      listing.categories = [category];
      const location: ILocation = { id: 8454 };
      listing.locations = [location];

      activatedRoute.data = of({ listing });
      comp.ngOnInit();

      expect(comp.categoriesSharedCollection()).toContainEqual(category);
      expect(comp.locationsSharedCollection()).toContainEqual(location);
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
