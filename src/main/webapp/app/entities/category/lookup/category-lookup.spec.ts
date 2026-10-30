import { MockInstance, afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { FaIconLibrary } from '@fortawesome/angular-fontawesome';
import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

import { fontAwesomeIcons } from 'app/config/font-awesome-icons';
import { CategoryIndexItem, CategoryIndexService } from '../service/category-index.service';

import { CategoryLookup } from './category-lookup';

const item = (id: number, code: string, name: string, parentId: number | null, listingCount = 0): CategoryIndexItem => ({
  id,
  code,
  level: /^[A-Z]$/.test(code) ? 1 : code.length,
  name,
  slug: null,
  letter: 'K',
  parentId,
  parentName: null,
  listingCount,
});

const ITEMS = [
  item(1, 'E', 'CUNG CẤP NƯỚC; HOẠT ĐỘNG QUẢN LÝ VÀ XỬ LÝ RÁC THẢI, NƯỚC THẢI', null),
  item(2, '36', 'Khai thác, xử lý và cung cấp nước', 1),
  item(3, '360', 'Khai thác, xử lý và cung cấp nước', 2),
  item(4, '3600', 'Khai thác, xử lý và cung cấp nước', 3),
  item(5, '36000', 'Khai thác, xử lý và cung cấp nước', 4, 12),
  item(6, 'F', 'XÂY DỰNG', null),
];

describe('CategoryLookup', () => {
  let fixture: ComponentFixture<CategoryLookup>;
  let comp: CategoryLookup;
  let detail: MockInstance<CategoryIndexService['detail']>;

  const element = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const rows = (): string[] => [...element().querySelectorAll('.lookup-row .lookup-code')].map(el => el.textContent.trim());

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      providers: [provideTranslateService(), provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    const service = TestBed.inject(CategoryIndexService);
    vi.spyOn(service, 'all').mockReturnValue(of(ITEMS));
    detail = vi.spyOn(service, 'detail').mockImplementation(id =>
      of({
        id,
        name: 'x',
        slug: null,
        listingCount: 0,
        description: id === 5 ? 'Ngành này gồm:\n\n- Khai thác nước;\n \n- Thu nước mưa' : null,
      }),
    );
    TestBed.inject(FaIconLibrary).addIcons(...fontAwesomeIcons);
    fixture = TestBed.createComponent(CategoryLookup);
    comp = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => vi.useRealTimers());

  it('mới vào chỉ hiện ngành cấp 1, bấm thì mở con và tải chi tiết', () => {
    expect(rows()).toEqual(['E', 'F']);
    comp.toggle(comp.tree().byId.get(1)!);
    fixture.detectChanges();
    expect(rows()).toEqual(['E', '36', 'F']);
    expect(detail).toHaveBeenCalledWith(1);
    comp.toggle(comp.tree().byId.get(1)!);
    fixture.detectChanges();
    expect(rows()).toEqual(['E', 'F']);
  });

  it('Mở hết / Thu gọn', () => {
    comp.expandAll();
    fixture.detectChanges();
    expect(rows()).toEqual(['E', '36', '360', '3600', '36000', 'F']);
    expect(detail).not.toHaveBeenCalled();
    comp.collapseAll();
    fixture.detectChanges();
    expect(rows()).toEqual(['E', 'F']);
  });

  it('gõ nguyên mã: mở tới ngành đó, hiện phần Chi tiết', () => {
    comp.onSearchInput('36000');
    vi.advanceTimersByTime(300);
    fixture.detectChanges();
    expect(rows()).toEqual(['E', '36', '360', '3600', '36000']);
    expect(detail).toHaveBeenCalledWith(5);
    expect(element().querySelector('[data-cy="lookupDetail"]')?.textContent).toContain('Khai thác nước');
    expect(comp.detail(comp.tree().byId.get(5)!)).toBe('Ngành này gồm:\n- Khai thác nước;\n- Thu nước mưa');
  });

  it('tìm tên không dấu; không khớp thì báo', () => {
    comp.setQuery('xay dung');
    fixture.detectChanges();
    expect(rows()).toEqual(['F']);
    expect(comp.search()!.matches.size).toBe(1);
    comp.setQuery('không có ngành này');
    fixture.detectChanges();
    expect(element().querySelector('[data-cy="lookupNoMatch"]')).toBeTruthy();
  });
});
