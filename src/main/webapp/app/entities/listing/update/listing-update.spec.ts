import { beforeEach, describe, expect, it, vi } from 'vitest';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { Subject, firstValueFrom, from, of } from 'rxjs';

import { ICategory } from 'app/entities/category/category.model';
import { CategoryIndexService } from 'app/entities/category/service/category-index.service';
import { CategoryService } from 'app/entities/category/service/category.service';
import { ILocation } from 'app/entities/location/location.model';
import { LocationIndexService } from 'app/entities/location/service/location-index.service';
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
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            params: from([{}]),
            snapshot: { queryParamMap: convertToParamMap({}) },
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
    it('should update editForm with categories and locations without preloading a 20-row select', () => {
      const category: ICategory = { id: 6752, name: 'BAO BÌ' };
      const location: ILocation = { id: 8454, name: 'Phường Vĩnh Tuy' };
      const listing: IListing = { id: 86, status: 'publish', categories: [category], locations: [location] };
      const categoryQuery = vi.spyOn(categoryService, 'query');
      const locationQuery = vi.spyOn(locationService, 'query');

      activatedRoute.data = of({ listing });
      comp.ngOnInit();

      expect(comp.listing).toEqual(listing);
      expect(comp.editForm.controls.categories.value).toEqual([category]);
      expect(comp.editForm.controls.locations.value).toEqual([location]);
      expect(categoryQuery).not.toHaveBeenCalled();
      expect(locationQuery).not.toHaveBeenCalled();
      expect(comp.hasUnsavedChanges()).toBe(false);
    });

    it('new listing: draft by default, technical block open, required fields listed', () => {
      activatedRoute.data = of({ listing: null });
      comp.ngOnInit();

      expect(comp.editForm.controls.status.value).toBe('draft');
      expect(comp.technicalOpen()).toBe(true);
      expect(comp.invalidFields().map(field => field.key)).toEqual(['wpId', 'name']);
    });

    it('keeps an unknown status from the data as an extra option', () => {
      activatedRoute.data = of({ listing: { id: 1, status: 'pending' } });
      comp.ngOnInit();

      expect(comp.statusOptions()).toEqual(['publish', 'draft', 'pending']);
      expect(comp.statusLabel('publish')).toBe('javaSpringBootBackEndApp.listing.statusValue.publish');
      expect(comp.statusLabel('pending')).toBe('pending');
    });
  });

  describe('search pickers', () => {
    it('searchCategories maps the A–Z index to options with the parent as hint', async () => {
      vi.spyOn(TestBed.inject(CategoryIndexService), 'search').mockReturnValue(
        of({
          items: [{ id: 4, name: 'BAO BÌ', slug: null, letter: 'B', parentId: 1, parentName: 'SẢN XUẤT', listingCount: 9 }],
          page: 0,
          size: 20,
          totalItems: 1,
          totalPages: 1,
        }),
      );
      expect(await firstValueFrom(comp.searchCategories('bao bi'))).toEqual([{ id: 4, name: 'BAO BÌ', hint: 'SẢN XUẤT' }]);
    });

    it('searchLocations searches new and old units; old ones get a badge', async () => {
      const search = vi.spyOn(TestBed.inject(LocationIndexService), 'search').mockReturnValue(
        of({
          items: [
            {
              id: 12000,
              name: 'Phường Vĩnh Tuy',
              slug: null,
              type: 'ward',
              isNew: true,
              letter: 'V',
              parentId: 11960,
              parentName: 'Thành phố Hà Nội',
              provinceName: 'Thành phố Hà Nội',
              listingCount: 1,
            },
            {
              id: 800,
              name: 'Phường Thanh Lương',
              slug: null,
              type: 'ward',
              isNew: false,
              letter: 'T',
              parentId: 30,
              parentName: 'Quận Hai Bà Trưng',
              provinceName: 'Thành phố Hà Nội',
              listingCount: 1,
            },
          ],
          page: 0,
          size: 20,
          totalItems: 2,
          totalPages: 1,
        }),
      );
      const options = await firstValueFrom(comp.searchLocations('vinh tuy'));
      expect(search).toHaveBeenCalledWith({ q: 'vinh tuy', generation: 'all', size: 20 });
      expect(options).toEqual([
        { id: 12000, name: 'Phường Vĩnh Tuy', hint: 'Thành phố Hà Nội', badge: null },
        {
          id: 800,
          name: 'Phường Thanh Lương',
          hint: 'Quận Hai Bà Trưng, Thành phố Hà Nội',
          badge: 'javaSpringBootBackEndApp.location.index.oldBadge',
        },
      ]);
    });
  });

  describe('unsaved changes and shortcut', () => {
    it('reports unsaved changes until saved', () => {
      const saveSubject = new Subject<IListing>();
      vi.spyOn(listingService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState').mockImplementation(() => undefined);
      activatedRoute.data = of({ listing: { id: 7, wpId: 1, name: 'A' } });
      comp.ngOnInit();
      comp.editForm.controls.phone.setValue('0240000000');
      comp.editForm.markAsDirty();
      expect(comp.hasUnsavedChanges()).toBe(true);

      comp.save();
      saveSubject.next({ id: 7 });
      saveSubject.complete();
      expect(comp.hasUnsavedChanges()).toBe(false);
    });

    it('Ctrl+S saves a valid form, otherwise jumps to the first invalid field', () => {
      const save = vi.spyOn(comp, 'save').mockImplementation(() => undefined);
      const focusField = vi.spyOn(comp, 'focusField').mockImplementation(() => undefined);
      activatedRoute.data = of({ listing: { id: 7, wpId: 1, name: 'A' } });
      comp.ngOnInit();
      const event = new KeyboardEvent('keydown', { key: 's', ctrlKey: true, cancelable: true });
      comp.onSaveShortcut(event);
      expect(event.defaultPrevented).toBe(true);
      expect(save).toHaveBeenCalledTimes(1);

      comp.editForm.controls.name.setValue(null);
      comp.onSaveShortcut(new KeyboardEvent('keydown', { key: 's', ctrlKey: true, cancelable: true }));
      expect(save).toHaveBeenCalledTimes(1);
      expect(focusField).toHaveBeenCalledWith('name');
    });

    it('?focus=email from the detail page jumps to that field; unknown fields are ignored', () => {
      const focusField = vi.spyOn(comp, 'focusField').mockImplementation(() => undefined);
      Object.assign(activatedRoute, { snapshot: { queryParamMap: convertToParamMap({ focus: 'email' }) } });
      activatedRoute.data = of({ listing: { id: 7, wpId: 1, name: 'A' } });
      comp.ngOnInit();
      expect(focusField).toHaveBeenCalledWith('email');

      focusField.mockClear();
      Object.assign(activatedRoute, { snapshot: { queryParamMap: convertToParamMap({ focus: 'constructor' }) } });
      comp.ngOnInit();
      expect(focusField).not.toHaveBeenCalled();
    });

    it('focusField opens the technical block for technical fields', () => {
      activatedRoute.data = of({ listing: { id: 7 } });
      comp.ngOnInit();
      comp.focusField('wpId');
      expect(comp.technicalOpen()).toBe(true);
      expect(comp.editForm.controls.wpId.touched).toBe(true);
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
});
