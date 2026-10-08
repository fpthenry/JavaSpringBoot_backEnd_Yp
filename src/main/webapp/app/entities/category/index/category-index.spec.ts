import { MockInstance, afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';

import { FaIconLibrary } from '@fortawesome/angular-fontawesome';
import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

import { fontAwesomeIcons } from 'app/config/font-awesome-icons';
import { CategoryIndexPage, CategoryIndexService } from '../service/category-index.service';

import { CategoryIndex, shortCount } from './category-index';

const PAGE: CategoryIndexPage = {
  items: [
    { id: 1, name: 'LỊCH', slug: 'lich', letter: 'L', parentId: 14, parentName: 'SẢN XUẤT', listingCount: 51 },
    { id: 2, name: 'Lắp đặt hệ thống điện', slug: 'lap-dat', letter: 'L', parentId: 10, parentName: 'Lắp đặt', listingCount: 353_317 },
  ],
  page: 0,
  size: 60,
  totalItems: 2,
  totalPages: 1,
};

describe('shortCount', () => {
  it('rút gọn số doanh nghiệp như FE: 74, 1k, 321k, 1tr, 1,2tr', () => {
    expect([0, 74, 999, 1337, 321_741, 1_091_865, 1_250_000].map(shortCount)).toEqual(['0', '74', '999', '1k', '321k', '1tr', '1,2tr']);
  });
});

describe('CategoryIndex', () => {
  let fixture: ComponentFixture<CategoryIndex>;
  let comp: CategoryIndex;
  let service: CategoryIndexService;
  let navigate: MockInstance<Router['navigate']>;
  let letters: MockInstance<CategoryIndexService['letters']>;
  let search: MockInstance<CategoryIndexService['search']>;

  const setup = (queryParams: Record<string, string>): void => {
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(queryParams) } } },
      ],
    });
    service = TestBed.inject(CategoryIndexService);
    letters = vi.spyOn(service, 'letters').mockReturnValue(
      of([
        { letter: 'B', count: 313 },
        { letter: 'L', count: 30 },
      ]),
    );
    search = vi.spyOn(service, 'search').mockReturnValue(of(PAGE));
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    TestBed.inject(FaIconLibrary).addIcons(...fontAwesomeIcons);
    fixture = TestBed.createComponent(CategoryIndex);
    comp = fixture.componentInstance;
  };

  beforeEach(() => {
    TestBed.resetTestingModule();
    vi.useFakeTimers();
  });

  afterEach(() => vi.useRealTimers());

  it('mở trang: đọc trạng thái từ URL, tải chữ cái và ngành', () => {
    setup({ letter: 'L', q: 'lap', page: '2', sort: 'count' });
    expect(letters).toHaveBeenCalledWith(true);
    expect(search).toHaveBeenLastCalledWith({ letter: 'L', q: 'lap', hideEmpty: true, page: 1, size: 60, sort: 'listingCount,desc' });
    expect(comp.result()?.items.length).toBe(2);
    expect(comp.letters().map(l => l.letter)).toEqual(['B', 'L']);
  });

  it('bấm chữ cái: về trang 1, ghi lên URL, tải lại', () => {
    setup({ page: '3' });
    comp.selectLetter('B');
    expect(search).toHaveBeenLastCalledWith(expect.objectContaining({ letter: 'B', page: 0, sort: 'name,asc' }));
    expect(navigate).toHaveBeenLastCalledWith(
      [],
      expect.objectContaining({ queryParams: { letter: 'B', q: null, page: null, sort: null, all: null }, replaceUrl: true }),
    );
    comp.selectLetter(null);
    expect(search).toHaveBeenLastCalledWith(expect.objectContaining({ letter: null }));
  });

  it('gõ tìm kiếm: chờ 300 ms mới gọi API, bỏ khoảng trắng', () => {
    setup({});
    const calls = search.mock.calls.length;
    comp.onQueryChange('lap');
    comp.onQueryChange('lap dat ');
    expect(search.mock.calls.length).toBe(calls);
    vi.advanceTimersByTime(300);
    expect(search.mock.calls.length).toBe(calls + 1);
    expect(search).toHaveBeenLastCalledWith(expect.objectContaining({ q: 'lap dat', page: 0 }));
  });

  it('hiện cả ngành trống và sắp theo số doanh nghiệp', () => {
    setup({});
    comp.toggleHideEmpty();
    expect(letters).toHaveBeenLastCalledWith(false);
    expect(search).toHaveBeenLastCalledWith(expect.objectContaining({ hideEmpty: false }));
    comp.changeSort('count');
    expect(search).toHaveBeenLastCalledWith(expect.objectContaining({ sort: 'listingCount,desc' }));
    expect(navigate).toHaveBeenLastCalledWith(
      [],
      expect.objectContaining({ queryParams: expect.objectContaining({ all: 'true', sort: 'count' }) }),
    );
  });

  it('thẻ ngành mở danh sách doanh nghiệp lọc theo cả cây ngành', () => {
    setup({});
    expect(comp.listingQueryParams(PAGE.items[1])).toEqual({ 'filter[categoryTreeId.equals]': 2 });
    expect(comp.formatCount(353_317)).toBe('353.317');
  });
});
