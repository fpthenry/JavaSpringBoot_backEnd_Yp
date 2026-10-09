import { MockInstance, afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';

import { FaIconLibrary } from '@fortawesome/angular-fontawesome';
import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

import { fontAwesomeIcons } from 'app/config/font-awesome-icons';
import { LocationIndexItem, LocationIndexPage, LocationIndexService } from '../service/location-index.service';

import { LocationIndex, placeOf } from './location-index';

const item = (patch: Partial<LocationIndexItem>): LocationIndexItem => ({
  id: 1,
  name: 'Thành phố Hà Nội',
  slug: null,
  type: 'province',
  isNew: true,
  letter: 'H',
  parentId: null,
  parentName: null,
  provinceName: 'Thành phố Hà Nội',
  listingCount: 0,
  ...patch,
});

const PAGE: LocationIndexPage = {
  items: [
    item({
      id: 13001,
      name: 'Xã Châu Thới',
      type: 'ward',
      letter: 'C',
      parentId: 13000,
      parentName: 'Tỉnh Bến Tre',
      provinceName: 'Tỉnh Bến Tre',
      listingCount: 5,
    }),
    item({
      id: 12000,
      name: 'Phường Vĩnh Tuy',
      type: 'ward',
      letter: 'V',
      parentId: 11960,
      parentName: 'Thành phố Hà Nội',
      listingCount: 2375,
    }),
  ],
  page: 0,
  size: 60,
  totalItems: 2,
  totalPages: 1,
};

describe('placeOf', () => {
  it('xã mới: tỉnh; phường cũ: quận, tỉnh; tỉnh: không có', () => {
    expect(placeOf(PAGE.items[1])).toBe('Thành phố Hà Nội');
    expect(
      placeOf(item({ id: 800, name: 'Phường Thanh Lương', type: 'ward', isNew: false, parentId: 30, parentName: 'Quận Hai Bà Trưng' })),
    ).toBe('Quận Hai Bà Trưng, Thành phố Hà Nội');
    expect(placeOf(item({}))).toBeNull();
  });
});

describe('LocationIndex', () => {
  let fixture: ComponentFixture<LocationIndex>;
  let comp: LocationIndex;
  let service: LocationIndexService;
  let navigate: MockInstance<Router['navigate']>;
  let letters: MockInstance<LocationIndexService['letters']>;
  let search: MockInstance<LocationIndexService['search']>;

  const setup = (queryParams: Record<string, string>): void => {
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(queryParams) } } },
      ],
    });
    service = TestBed.inject(LocationIndexService);
    letters = vi.spyOn(service, 'letters').mockReturnValue(
      of([
        { letter: 'D', count: 41 },
        { letter: 'Đ', count: 244 },
      ]),
    );
    search = vi.spyOn(service, 'search').mockReturnValue(of(PAGE));
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    TestBed.inject(FaIconLibrary).addIcons(...fontAwesomeIcons);
    fixture = TestBed.createComponent(LocationIndex);
    comp = fixture.componentInstance;
  };

  beforeEach(() => {
    TestBed.resetTestingModule();
    vi.useFakeTimers();
  });

  afterEach(() => vi.useRealTimers());

  it('mở trang mặc định: địa giới mới, mọi cấp, ẩn nơi trống', () => {
    setup({});
    expect(letters).toHaveBeenCalledWith({ generation: 'new', level: null, hideEmpty: true });
    expect(search).toHaveBeenLastCalledWith({
      generation: 'new',
      level: null,
      hideEmpty: true,
      letter: null,
      q: '',
      page: 0,
      size: 60,
      sort: 'name,asc',
    });
    expect(comp.letters().map(l => l.letter)).toEqual(['D', 'Đ']);
    expect(comp.levels()).toEqual(['province', 'ward']);
  });

  it('đọc trạng thái từ URL; tham số lạ thì bỏ qua', () => {
    setup({ letter: 'Đ', q: 'dong', page: '2', sort: 'count', gen: 'old', level: 'district', all: 'true' });
    expect(search).toHaveBeenLastCalledWith(
      expect.objectContaining({
        generation: 'old',
        level: 'district',
        hideEmpty: false,
        letter: 'Đ',
        q: 'dong',
        page: 1,
        sort: 'listingCount,desc',
      }),
    );
    TestBed.resetTestingModule();
    setup({ gen: '2025', level: 'district' });
    // bộ mới không có cấp quận/huyện
    expect(search).toHaveBeenLastCalledWith(expect.objectContaining({ generation: 'new', level: null }));
  });

  it('đổi địa giới: tải lại chữ cái, bỏ cấp quận/huyện khi về bộ mới, ghi lên URL', () => {
    setup({ gen: 'all', level: 'district', page: '4' });
    comp.changeGeneration('new');
    expect(comp.level()).toBeNull();
    expect(letters).toHaveBeenLastCalledWith({ generation: 'new', level: null, hideEmpty: true });
    expect(search).toHaveBeenLastCalledWith(expect.objectContaining({ generation: 'new', level: null, page: 0 }));
    comp.changeGeneration('old');
    comp.changeLevel('ward');
    expect(letters).toHaveBeenLastCalledWith({ generation: 'old', level: 'ward', hideEmpty: true });
    expect(navigate).toHaveBeenLastCalledWith(
      [],
      expect.objectContaining({
        queryParams: { letter: null, q: null, page: null, sort: null, all: null, gen: 'old', level: 'ward' },
        replaceUrl: true,
      }),
    );
  });

  it('bấm chữ Đ, gõ tìm kiếm chờ 300 ms', () => {
    setup({});
    comp.selectLetter('Đ');
    expect(search).toHaveBeenLastCalledWith(expect.objectContaining({ letter: 'Đ', page: 0 }));
    const calls = search.mock.calls.length;
    comp.onQueryChange('dong');
    comp.onQueryChange('dong anh ');
    expect(search.mock.calls.length).toBe(calls);
    vi.advanceTimersByTime(300);
    expect(search).toHaveBeenLastCalledWith(expect.objectContaining({ q: 'dong anh', letter: 'Đ' }));
  });

  it('thẻ mở danh sách doanh nghiệp lọc theo cả cây địa phương', () => {
    setup({});
    expect(comp.listingQueryParams(PAGE.items[1])).toEqual({ 'filter[locationTreeId.equals]': 12000 });
    expect(comp.formatCount(271_041)).toBe('271.041');
  });
});
