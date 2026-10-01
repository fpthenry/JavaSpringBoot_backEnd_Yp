import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, ParamMap, convertToParamMap } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { BehaviorSubject } from 'rxjs';

import { FilterOptions } from 'app/shared/filter';

import { LOCATION_TREE_FILTER, LocationTreeFilter } from './location-tree-filter';

describe('LocationTreeFilter', () => {
  let fixture: ComponentFixture<LocationTreeFilter>;
  let comp: LocationTreeFilter;
  let httpMock: HttpTestingController;
  let filters: FilterOptions;
  let queryParamMap: BehaviorSubject<ParamMap>;

  const filterValues = (): string[] => filters.filterOptions.find(option => option.name === LOCATION_TREE_FILTER)?.values ?? [];

  beforeEach(() => {
    queryParamMap = new BehaviorSubject(convertToParamMap({}));
    TestBed.configureTestingModule({
      providers: [provideTranslateService(), provideHttpClientTesting(), { provide: ActivatedRoute, useValue: { queryParamMap } }],
    });
    fixture = TestBed.createComponent(LocationTreeFilter);
    filters = new FilterOptions();
    fixture.componentRef.setInput('filters', filters);
    comp = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    httpMock.expectOne(req => req.params.get('parentId.specified') === 'false').flush([{ id: 2, name: 'Thành phố Hà Nội' }]);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load provinces', () => {
    expect(comp.provinces()).toEqual([{ id: 2, name: 'Thành phố Hà Nội' }]);
  });

  it('should filter by the deepest selected level and replace the previous value', () => {
    comp.onProvinceChange(2);
    httpMock.expectOne(req => req.params.get('parentId.equals') === '2').flush([{ id: 256, name: 'Huyện Ba Vì' }]);
    expect(filterValues()).toEqual(['2']);

    comp.onDistrictChange(256);
    httpMock.expectOne(req => req.params.get('parentId.equals') === '256').flush([]);
    expect(filterValues()).toEqual(['256']);
    expect(comp.districts()).toEqual([{ id: 256, name: 'Huyện Ba Vì' }]);
  });

  it('should remove the filter when selection is cleared', () => {
    comp.onProvinceChange(2);
    httpMock.expectOne(req => req.params.get('parentId.equals') === '2').flush([]);

    comp.onProvinceChange(null);

    expect(filterValues()).toEqual([]);
    expect(comp.districts()).toEqual([]);
  });

  it('should restore the selection from the url by walking up the parents', () => {
    queryParamMap.next(convertToParamMap({ [`filter[${LOCATION_TREE_FILTER}]`]: '256' }));
    TestBed.tick();

    httpMock.expectOne(req => req.url.endsWith('api/locations/256')).flush({ id: 256, name: 'Huyện Ba Vì', parent: { id: 2 } });
    httpMock.expectOne(req => req.url.endsWith('api/locations/2')).flush({ id: 2, name: 'Thành phố Hà Nội', parent: null });
    httpMock.expectOne(req => req.params.get('parentId.equals') === '2').flush([]);
    httpMock.expectOne(req => req.params.get('parentId.equals') === '256').flush([]);

    expect(comp.provinceId()).toBe(2);
    expect(comp.districtId()).toBe(256);
    expect(comp.wardId()).toBeNull();
  });
});
