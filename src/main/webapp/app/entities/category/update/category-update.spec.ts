import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpResponse } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { Subject, from, of } from 'rxjs';

import { IListing } from 'app/entities/listing/listing.model';
import { ListingService } from 'app/entities/listing/service/listing.service';
import { ICategory } from '../category.model';
import { CategoryService } from '../service/category.service';

import { CategoryFormService } from './category-form.service';
import { CategoryUpdate } from './category-update';

describe('Category Management Update Component', () => {
  let comp: CategoryUpdate;
  let fixture: ComponentFixture<CategoryUpdate>;
  let activatedRoute: ActivatedRoute;
  let categoryFormService: CategoryFormService;
  let categoryService: CategoryService;
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

    fixture = TestBed.createComponent(CategoryUpdate);
    activatedRoute = TestBed.inject(ActivatedRoute);
    categoryFormService = TestBed.inject(CategoryFormService);
    categoryService = TestBed.inject(CategoryService);
    listingService = TestBed.inject(ListingService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call Category query and add missing value', () => {
      const category: ICategory = { id: 4374 };
      const parent: ICategory = { id: 6752 };
      category.parent = parent;

      const categoryCollection: ICategory[] = [{ id: 6752 }];
      vi.spyOn(categoryService, 'query').mockReturnValue(of(new HttpResponse({ body: categoryCollection })));
      const additionalCategories = [parent];
      const expectedCollection: ICategory[] = [...additionalCategories, ...categoryCollection];
      vi.spyOn(categoryService, 'addCategoryToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ category });
      comp.ngOnInit();

      expect(categoryService.query).toHaveBeenCalled();
      expect(categoryService.addCategoryToCollectionIfMissing).toHaveBeenCalledWith(
        categoryCollection,
        ...additionalCategories.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.categoriesSharedCollection()).toEqual(expectedCollection);
    });

    it('should call Listing query and add missing value', () => {
      const category: ICategory = { id: 4374 };
      const listings: IListing[] = [{ id: 14276 }];
      category.listings = listings;

      const listingCollection: IListing[] = [{ id: 14276 }];
      vi.spyOn(listingService, 'query').mockReturnValue(of(new HttpResponse({ body: listingCollection })));
      const additionalListings = [...listings];
      const expectedCollection: IListing[] = [...additionalListings, ...listingCollection];
      vi.spyOn(listingService, 'addListingToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ category });
      comp.ngOnInit();

      expect(listingService.query).toHaveBeenCalled();
      expect(listingService.addListingToCollectionIfMissing).toHaveBeenCalledWith(
        listingCollection,
        ...additionalListings.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.listingsSharedCollection()).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const category: ICategory = { id: 4374 };
      const parent: ICategory = { id: 6752 };
      category.parent = parent;
      const listing: IListing = { id: 14276 };
      category.listings = [listing];

      activatedRoute.data = of({ category });
      comp.ngOnInit();

      expect(comp.categoriesSharedCollection()).toContainEqual(parent);
      expect(comp.listingsSharedCollection()).toContainEqual(listing);
      expect(comp.category).toEqual(category);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<ICategory>();
      const category = { id: 6752 };
      vi.spyOn(categoryFormService, 'getCategory').mockReturnValue(category);
      vi.spyOn(categoryService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ category });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(category);
      saveSubject.complete();

      // THEN
      expect(categoryFormService.getCategory).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(categoryService.update).toHaveBeenCalledWith(expect.objectContaining(category));
      expect(comp.isSaving()).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<ICategory>();
      const category = { id: 6752 };
      vi.spyOn(categoryFormService, 'getCategory').mockReturnValue({ id: null });
      vi.spyOn(categoryService, 'create').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ category: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(category);
      saveSubject.complete();

      // THEN
      expect(categoryFormService.getCategory).toHaveBeenCalled();
      expect(categoryService.create).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<ICategory>();
      const category = { id: 6752 };
      vi.spyOn(categoryService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ category });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(categoryService.update).toHaveBeenCalled();
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
