import { beforeEach, describe, expect, it, vi } from 'vitest';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { Subject, firstValueFrom, from, of } from 'rxjs';

import { ListingService } from 'app/entities/listing/service/listing.service';
import { ICategory } from '../category.model';
import { CategoryIndexService } from '../service/category-index.service';
import { CategoryService } from '../service/category.service';

import { CategoryFormService } from './category-form.service';
import { CategoryUpdate } from './category-update';

/** Cây: 1 SẢN XUẤT > 4 BAO BÌ > 5 BAO BÌ GIẤY */
const TREE: Record<number, ICategory> = {
  1: { id: 1, name: 'SẢN XUẤT', parent: null },
  4: { id: 4, name: 'BAO BÌ', parent: { id: 1, name: 'SẢN XUẤT' } },
  5: { id: 5, name: 'BAO BÌ GIẤY', parent: { id: 4, name: 'BAO BÌ' } },
};

describe('Category Management Update Component', () => {
  let comp: CategoryUpdate;
  let fixture: ComponentFixture<CategoryUpdate>;
  let activatedRoute: ActivatedRoute;
  let categoryFormService: CategoryFormService;
  let categoryService: CategoryService;

  const routeWith = (query: Record<string, string>): void => {
    Object.assign(activatedRoute, { snapshot: { queryParamMap: convertToParamMap(query) } });
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { params: from([{}]), snapshot: { queryParamMap: convertToParamMap({}) } } },
      ],
    });

    fixture = TestBed.createComponent(CategoryUpdate);
    activatedRoute = TestBed.inject(ActivatedRoute);
    categoryFormService = TestBed.inject(CategoryFormService);
    categoryService = TestBed.inject(CategoryService);
    vi.spyOn(categoryService, 'find').mockImplementation((id: number) => of(TREE[id]));
    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('loads the parent path and level without preloading 20-row selects', () => {
      const categoryQuery = vi.spyOn(categoryService, 'query');
      const listingQuery = vi.spyOn(TestBed.inject(ListingService), 'query');
      activatedRoute.data = of({ category: TREE[5] });
      comp.ngOnInit();

      expect(comp.category).toEqual(TREE[5]);
      expect(comp.parentPath()).toEqual([
        { id: 1, name: 'SẢN XUẤT' },
        { id: 4, name: 'BAO BÌ' },
      ]);
      expect(comp.level()).toBe(3);
      expect(comp.parentCycle()).toBe(false);
      expect(categoryQuery).not.toHaveBeenCalled();
      expect(listingQuery).not.toHaveBeenCalled();
      expect(comp.hasUnsavedChanges()).toBe(false);
    });

    it('blocks choosing one of its own sub-categories as parent (cycle)', () => {
      activatedRoute.data = of({ category: TREE[4] });
      comp.ngOnInit();
      expect(comp.excludedParentIds()).toEqual([4]);

      comp.editForm.controls.parent.setValue({ id: 5, name: 'BAO BÌ GIẤY' });
      expect(comp.parentCycle()).toBe(true);
      expect(comp.editForm.controls.parent.hasError('cycle')).toBe(true);
      expect(comp.editForm.invalid).toBe(true);

      const create = vi.spyOn(categoryService, 'update');
      vi.spyOn(comp, 'focusField').mockImplementation(() => undefined);
      comp.save();
      expect(create).not.toHaveBeenCalled();
      expect(comp.focusField).toHaveBeenCalledWith('parent');

      comp.editForm.controls.parent.setValue({ id: 1, name: 'SẢN XUẤT' });
      expect(comp.parentCycle()).toBe(false);
      expect(comp.editForm.controls.parent.errors).toBeNull();
    });

    it('"Thêm ngành con": ?parentId= prefills the parent', () => {
      routeWith({ parentId: '4' });
      activatedRoute.data = of({ category: null });
      comp.ngOnInit();
      expect(comp.editForm.controls.parent.value).toEqual({ id: 4, name: 'BAO BÌ' });
      expect(comp.level()).toBe(3);
    });

    it('?focus=description jumps to that field', () => {
      const focusField = vi.spyOn(comp, 'focusField').mockImplementation(() => undefined);
      routeWith({ focus: 'description' });
      activatedRoute.data = of({ category: TREE[4] });
      comp.ngOnInit();
      expect(focusField).toHaveBeenCalledWith('description');
    });
  });

  it('generateSlug builds the slug from the Vietnamese name', () => {
    activatedRoute.data = of({ category: { id: 9, name: 'Lập trình máy vi tính &amp; Đồ họa' } });
    comp.ngOnInit();
    comp.generateSlug();
    expect(comp.editForm.controls.slug.value).toBe('lap-trinh-may-vi-tinh-do-hoa');
    expect(comp.editForm.dirty).toBe(true);
  });

  it('searchParents maps the A–Z index with the parent as hint', async () => {
    vi.spyOn(TestBed.inject(CategoryIndexService), 'search').mockReturnValue(
      of({
        items: [{ id: 4, name: 'BAO BÌ', slug: null, letter: 'B', parentId: 1, parentName: 'SẢN XUẤT', listingCount: 9 }],
        page: 0,
        size: 20,
        totalItems: 1,
        totalPages: 1,
      }),
    );
    expect(await firstValueFrom(comp.searchParents('bao'))).toEqual([{ id: 4, name: 'BAO BÌ', hint: 'SẢN XUẤT' }]);
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      const saveSubject = new Subject<ICategory>();
      const category = { id: 5841 };
      vi.spyOn(categoryFormService, 'getCategory').mockReturnValue(category);
      vi.spyOn(categoryService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState').mockImplementation(() => undefined);
      activatedRoute.data = of({ category });
      comp.ngOnInit();

      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(category);
      saveSubject.complete();

      expect(categoryService.update).toHaveBeenCalledWith(expect.objectContaining(category));
      expect(comp.previousState).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      const saveSubject = new Subject<ICategory>();
      vi.spyOn(categoryFormService, 'getCategory').mockReturnValue({ id: null });
      vi.spyOn(categoryService, 'create').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState').mockImplementation(() => undefined);
      activatedRoute.data = of({ category: null });
      comp.ngOnInit();

      comp.save();
      saveSubject.next({ id: 5841 });
      saveSubject.complete();

      expect(categoryService.create).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
    });

    it('should set isSaving to false on error', () => {
      const saveSubject = new Subject<ICategory>();
      vi.spyOn(categoryService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState').mockImplementation(() => undefined);
      activatedRoute.data = of({ category: { id: 5841 } });
      comp.ngOnInit();

      comp.save();
      saveSubject.error('This is an error!');

      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });
});
