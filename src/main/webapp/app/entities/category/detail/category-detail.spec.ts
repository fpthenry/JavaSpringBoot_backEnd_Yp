import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpHeaders, HttpResponse, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { FaIconLibrary } from '@fortawesome/angular-fontawesome';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap/modal';
import { provideTranslateService } from '@ngx-translate/core';
import { Subject, of } from 'rxjs';

import { ITEM_DELETED_EVENT } from 'app/config';
import { fontAwesomeIcons } from 'app/config/font-awesome-icons';
import { ListingService } from 'app/entities/listing/service/listing.service';
import { ICategory } from '../category.model';
import { CategoryService } from '../service/category.service';

import { CategoryDetail } from './category-detail';

const TREE: Record<number, ICategory> = {
  1: { id: 1, code: 'C', level: 1, name: 'SẢN XUẤT', parent: null },
  4: {
    id: 4,
    code: '17',
    level: 2,
    name: 'BAO BÌ &amp; GIẤY',
    slug: 'bao-bi-va-giay-17',
    description: '',
    listingCount: 6625,
    parent: { id: 1, name: 'SẢN XUẤT', code: 'C' },
  },
};

describe('Category Management Detail Component', () => {
  let comp: CategoryDetail;
  let fixture: ComponentFixture<CategoryDetail>;
  let categoryService: CategoryService;
  let listingService: ListingService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter(
          [
            {
              path: '**',
              loadComponent: () => import('./category-detail').then(m => m.CategoryDetail),
              resolve: { category: () => of({ id: 123 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    });
    TestBed.inject(FaIconLibrary).addIcons(...fontAwesomeIcons);
    categoryService = TestBed.inject(CategoryService);
    listingService = TestBed.inject(ListingService);
    vi.spyOn(categoryService, 'find').mockImplementation((id: number) => of(TREE[id]));
    vi.spyOn(categoryService, 'query').mockReturnValue(
      of(new HttpResponse({ body: [{ id: 5, name: 'BAO BÌ GIẤY', listingCount: 120 }] as ICategory[] })),
    );
    vi.spyOn(listingService, 'query').mockReturnValue(
      of(
        new HttpResponse({
          body: [{ id: 9, name: 'CÔNG TY BAO BÌ A', addressNew: '1 Lãng Yên' }],
          headers: new HttpHeaders({ 'X-Total-Count': '6625' }),
        }),
      ),
    );
    fixture = TestBed.createComponent(CategoryDetail);
    comp = fixture.componentInstance;
  });

  const show = (category: ICategory): void => {
    fixture.componentRef.setInput('category', category);
    fixture.detectChanges();
  };

  it('should load category on init', async () => {
    vi.mocked(categoryService.find).mockReturnValue(of({ id: 123 }));
    const harness = await RouterTestingHarness.create();
    const instance = await harness.navigateByUrl('/', CategoryDetail);
    expect(instance.category()).toEqual(expect.objectContaining({ id: 123 }));
  });

  it('shows the tree path, level, children and recent businesses', () => {
    show(TREE[4]);
    expect(comp.name()).toBe('BAO BÌ & GIẤY');
    expect(comp.ancestors()).toEqual([{ id: 1, name: 'SẢN XUẤT', code: 'C' }]);
    expect(comp.level()).toBe(2);
    expect(categoryService.query).toHaveBeenCalledWith({ 'parentId.equals': 4, size: 500, sort: ['code,asc', 'name,asc'] });
    expect(comp.childCount()).toBe(1);
    expect(listingService.query).toHaveBeenCalledWith({ 'categoryTreeId.equals': 4, page: 0, size: 5, sort: ['updatedAt,desc'] });
    expect(comp.recentListings()?.map(listing => listing.name)).toEqual(['CÔNG TY BAO BÌ A']);
    expect(comp.recentTotal()).toBe(6625);
    const text = (fixture.nativeElement as HTMLElement).textContent;
    expect(text).toContain('BAO BÌ GIẤY');
  });

  it('empty description shows a hint with Bổ sung; A–Z link uses letter and name', () => {
    show(TREE[4]);
    expect((fixture.nativeElement as HTMLElement).querySelector('.empty-hint a')?.getAttribute('href')).toContain('focus=description');
    expect(comp.indexQueryParams()).toEqual({ letter: 'B', q: 'BAO BÌ & GIẤY' });
    expect(comp.newChildQueryParams(4)).toEqual({ parentId: 4 });
    show({ ...TREE[4], name: 'Đá quý' });
    expect(comp.letter()).toBe('D');
  });

  it('shows 10 sub-categories first, then all with "Xem thêm"', () => {
    const many = Array.from({ length: 21 }, (_, i) => ({ id: 100 + i, name: `NGÀNH ${i}`, listingCount: i }));
    vi.mocked(categoryService.query).mockReturnValue(of(new HttpResponse({ body: many })));
    show(TREE[4]);
    expect(comp.visibleChildren().length).toBe(10);
    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelectorAll('[data-cy=children] li').length).toBe(10);
    root.querySelector<HTMLButtonElement>('[data-cy=toggleChildren]')?.click();
    fixture.detectChanges();
    expect(root.querySelectorAll('[data-cy=children] li').length).toBe(21);
  });

  it('delete is disabled while the category has children', () => {
    show(TREE[4]);
    const button = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('[data-cy=entityDeleteButton]');
    expect(button?.disabled).toBe(true);
    vi.mocked(categoryService.query).mockReturnValue(of(new HttpResponse({ body: [] })));
    show({ ...TREE[4], id: 40 });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('[data-cy=entityDeleteButton]')?.disabled).toBe(false);
  });

  it('after delete goes back to the list; E opens the edit form', () => {
    const closed = new Subject<string>();
    vi.spyOn(TestBed.inject(NgbModal), 'open').mockReturnValue({ componentInstance: {}, closed } as unknown as NgbModalRef);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    show(TREE[4]);
    comp.delete(TREE[4]);
    closed.next(ITEM_DELETED_EVENT);
    expect(navigate).toHaveBeenCalledWith(['/category']);
    comp.onKeydown(new KeyboardEvent('keydown', { key: 'e' }));
    expect(navigate).toHaveBeenCalledWith(['/category', 4, 'edit']);
  });
});
