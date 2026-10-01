import { beforeEach, describe, expect, it } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, ParamMap, convertToParamMap } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';

import { FilterOptions } from 'app/shared/filter';

import { TreeFilter } from './tree-filter';
import { TreeItem, TreeSource } from './tree-source';

const FILTER = 'categoryTreeId.equals';

/** Cây 4 cấp: 1 -> 10 -> 100 -> 1000 (lá); 2 là gốc không có con. */
const NODES: TreeItem[] = [
  { id: 1, name: 'Gốc 1', parent: null },
  { id: 2, name: 'Gốc 2', parent: null },
  { id: 10, name: 'Cấp 2', parent: { id: 1 } },
  { id: 100, name: 'Cấp 3', parent: { id: 10 } },
  { id: 1000, name: 'Cấp 4', parent: { id: 100 } },
];

const fakeSource: TreeSource = {
  roots: () => of(NODES.filter(node => !node.parent)),
  children: (parentId: number) => of(NODES.filter(node => node.parent?.id === parentId)),
  find(id: number): Observable<TreeItem> {
    const node = NODES.find(n => n.id === id);
    return node ? of(node) : throwError(() => new Error('not found'));
  },
};

describe('TreeFilter', () => {
  let fixture: ComponentFixture<TreeFilter>;
  let comp: TreeFilter;
  let filters: FilterOptions;
  let queryParamMap: BehaviorSubject<ParamMap>;

  const filterValues = (): string[] => filters.filterOptions.find(option => option.name === FILTER)?.values ?? [];
  const levelIds = (): (number | null)[] => comp.levels().map(level => level.selected);

  beforeEach(() => {
    queryParamMap = new BehaviorSubject(convertToParamMap({}));
    TestBed.configureTestingModule({
      providers: [provideTranslateService(), { provide: ActivatedRoute, useValue: { queryParamMap } }],
    });
    fixture = TestBed.createComponent(TreeFilter);
    filters = new FilterOptions();
    fixture.componentRef.setInput('filters', filters);
    fixture.componentRef.setInput('source', fakeSource);
    fixture.componentRef.setInput('filterName', FILTER);
    fixture.componentRef.setInput('placeholders', ['all.roots', 'all.children']);
    comp = fixture.componentInstance;
    TestBed.tick();
  });

  it('should show the roots as first level', () => {
    expect(comp.levels()).toEqual([{ options: [NODES[0], NODES[1]], selected: null }]);
  });

  it('should add a level only when the selected node has children and filter by the deepest node', () => {
    comp.onSelect(0, 1);
    expect(levelIds()).toEqual([1, null]);
    expect(filterValues()).toEqual(['1']);

    comp.onSelect(1, 10);
    comp.onSelect(2, 100);
    comp.onSelect(3, 1000);
    expect(levelIds()).toEqual([1, 10, 100, 1000]);
    expect(filterValues()).toEqual(['1000']);

    comp.onSelect(0, 2);
    expect(levelIds()).toEqual([2]);
    expect(filterValues()).toEqual(['2']);
  });

  it('should fall back to the parent when a level is cleared and remove the filter at the root', () => {
    comp.onSelect(0, 1);
    comp.onSelect(1, 10);

    comp.onSelect(1, null);
    expect(levelIds()).toEqual([1, null]);
    expect(filterValues()).toEqual(['1']);

    comp.onSelect(0, null);
    expect(levelIds()).toEqual([null]);
    expect(filterValues()).toEqual([]);
  });

  it('should restore all levels from the url', () => {
    queryParamMap.next(convertToParamMap({ [`filter[${FILTER}]`]: '100' }));
    TestBed.tick();

    expect(levelIds()).toEqual([1, 10, 100, null]);
    expect(comp.levels().at(-1)?.options).toEqual([NODES[4]]);
  });

  it('should use the last placeholder for deeper levels', () => {
    expect(comp.placeholder(0)).toBe('all.roots');
    expect(comp.placeholder(3)).toBe('all.children');
  });
});
