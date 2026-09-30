import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpResponse } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { Subject, from, of } from 'rxjs';

import { IListing } from 'app/entities/listing/listing.model';
import { ListingService } from 'app/entities/listing/service/listing.service';
import { ILocation } from '../location.model';
import { LocationService } from '../service/location.service';

import { LocationFormService } from './location-form.service';
import { LocationUpdate } from './location-update';

describe('Location Management Update Component', () => {
  let comp: LocationUpdate;
  let fixture: ComponentFixture<LocationUpdate>;
  let activatedRoute: ActivatedRoute;
  let locationFormService: LocationFormService;
  let locationService: LocationService;
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

    fixture = TestBed.createComponent(LocationUpdate);
    activatedRoute = TestBed.inject(ActivatedRoute);
    locationFormService = TestBed.inject(LocationFormService);
    locationService = TestBed.inject(LocationService);
    listingService = TestBed.inject(ListingService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call Location query and add missing value', () => {
      const location: ILocation = { id: 13013 };
      const parent: ILocation = { id: 8454 };
      location.parent = parent;

      const locationCollection: ILocation[] = [{ id: 8454 }];
      vi.spyOn(locationService, 'query').mockReturnValue(of(new HttpResponse({ body: locationCollection })));
      const additionalLocations = [parent];
      const expectedCollection: ILocation[] = [...additionalLocations, ...locationCollection];
      vi.spyOn(locationService, 'addLocationToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ location });
      comp.ngOnInit();

      expect(locationService.query).toHaveBeenCalled();
      expect(locationService.addLocationToCollectionIfMissing).toHaveBeenCalledWith(
        locationCollection,
        ...additionalLocations.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.locationsSharedCollection()).toEqual(expectedCollection);
    });

    it('should call Listing query and add missing value', () => {
      const location: ILocation = { id: 13013 };
      const listingses: IListing[] = [{ id: 14276 }];
      location.listingses = listingses;

      const listingCollection: IListing[] = [{ id: 14276 }];
      vi.spyOn(listingService, 'query').mockReturnValue(of(new HttpResponse({ body: listingCollection })));
      const additionalListings = [...listingses];
      const expectedCollection: IListing[] = [...additionalListings, ...listingCollection];
      vi.spyOn(listingService, 'addListingToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ location });
      comp.ngOnInit();

      expect(listingService.query).toHaveBeenCalled();
      expect(listingService.addListingToCollectionIfMissing).toHaveBeenCalledWith(
        listingCollection,
        ...additionalListings.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.listingsSharedCollection()).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const location: ILocation = { id: 13013 };
      const parent: ILocation = { id: 8454 };
      location.parent = parent;
      const listings: IListing = { id: 14276 };
      location.listingses = [listings];

      activatedRoute.data = of({ location });
      comp.ngOnInit();

      expect(comp.locationsSharedCollection()).toContainEqual(parent);
      expect(comp.listingsSharedCollection()).toContainEqual(listings);
      expect(comp.location).toEqual(location);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<ILocation>();
      const location = { id: 8454 };
      vi.spyOn(locationFormService, 'getLocation').mockReturnValue(location);
      vi.spyOn(locationService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ location });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(location);
      saveSubject.complete();

      // THEN
      expect(locationFormService.getLocation).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(locationService.update).toHaveBeenCalledWith(expect.objectContaining(location));
      expect(comp.isSaving()).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<ILocation>();
      const location = { id: 8454 };
      vi.spyOn(locationFormService, 'getLocation').mockReturnValue({ id: null });
      vi.spyOn(locationService, 'create').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ location: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(location);
      saveSubject.complete();

      // THEN
      expect(locationFormService.getLocation).toHaveBeenCalled();
      expect(locationService.create).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<ILocation>();
      const location = { id: 8454 };
      vi.spyOn(locationService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ location });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(locationService.update).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareLocation', () => {
      it('should forward to locationService', () => {
        const entity = { id: 8454 };
        const entity2 = { id: 13013 };
        vi.spyOn(locationService, 'compareLocation');
        comp.compareLocation(entity, entity2);
        expect(locationService.compareLocation).toHaveBeenCalledWith(entity, entity2);
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
