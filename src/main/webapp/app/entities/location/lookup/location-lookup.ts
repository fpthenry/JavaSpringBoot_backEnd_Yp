import { NgTemplateOutlet } from '@angular/common';
import { Component, DestroyRef, ElementRef, Injector, afterNextRender, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { AlertError } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { LocationIndexItem, LocationIndexService, LocationNodeItem, LocationOrigin } from '../service/location-index.service';

import {
  LocationFilter,
  LocationNode,
  LocationSort,
  LocationTree,
  ancestorsOf,
  buildLocationTree,
  expandableIds,
  searchLocations,
  summarize,
} from './location-lookup-tree';

/** Bộ địa giới: new = từ 01/7/2025 (tỉnh → phường, xã, đặc khu), old = trước đó (tỉnh → quận, huyện → xã). */
export type Generation = 'new' | 'old';

/** Phần mở rộng của một nút: đơn vị mới → đơn vị cũ hình thành nên nó; đơn vị cũ → đơn vị mới nay chứa nó. */
interface Relation {
  origins?: LocationOrigin[];
  successors?: LocationIndexItem[];
}

const SEARCH_DELAY_MS = 200;

/**
 * Sửa tay: tra cứu đơn vị hành chính, bộ mới và bộ cũ tách riêng (2 tab), dạng cây như bảng tra cứu mã ngành.
 *   - Tìm theo tên không dấu (đầu từ) hoặc mã ĐVHC (01, 001, 00167); lọc nơi có doanh nghiệp / chưa có trong bảng chuyển đổi
 *   - Mở một đơn vị mới: hình thành từ những đơn vị cũ nào; mở một đơn vị cũ: nay thuộc đơn vị mới nào. Bấm để nhảy qua
 *     bộ kia, mở sẵn tới đúng đơn vị đó.
 * Mỗi bộ tải một lần (~3.400 / ~12.000 nút), tìm ngay trên trình duyệt; quan hệ cũ ↔ mới tải riêng khi mở từng nút.
 */
@Component({
  selector: 'jhi-location-lookup',
  templateUrl: './location-lookup.html',
  styleUrl: './location-lookup.scss',
  imports: [NgTemplateOutlet, RouterLink, FontAwesomeModule, TranslatePipe, TranslateDirective, AlertError],
})
export class LocationLookup {
  readonly generation = signal<Generation>('new');
  readonly sort = signal<LocationSort>('code');
  readonly filter = signal<LocationFilter>('all');
  /** Từ khóa đang áp dụng (sau khi ngừng gõ). */
  readonly query = signal('');
  readonly items = signal<Partial<Record<Generation, LocationNodeItem[]>>>({});
  readonly loading = signal<Partial<Record<Generation, boolean>>>({});
  readonly expanded = signal<ReadonlySet<number>>(new Set());
  /** Nút vừa nhảy tới từ bộ kia (tô nổi). */
  readonly focused = signal<number | null>(null);
  readonly relations = signal<ReadonlyMap<number, Relation | undefined>>(new Map());

  readonly tree = computed<LocationTree | null>(() => {
    const items = this.items()[this.generation()];
    return items ? buildLocationTree(items, this.sort()) : null;
  });
  readonly search = computed(() => {
    const tree = this.tree();
    return tree ? searchLocations(tree, this.query(), this.filter()) : null;
  });
  readonly summary = computed(() => {
    const tree = this.tree();
    return tree ? summarize(tree) : null;
  });
  readonly roots = computed(() => this.visible(this.tree()?.roots ?? []));
  readonly isLoading = computed(() => this.loading()[this.generation()] === true);

  protected readonly locationIndexService = inject(LocationIndexService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);
  private searchTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    inject(DestroyRef).onDestroy(() => clearTimeout(this.searchTimer));
    const params = this.route.snapshot.queryParamMap;
    if (params.get('generation') === 'old') {
      this.generation.set('old');
    }
    const q = params.get('q');
    if (q) {
      this.query.set(q.trim());
    }
    this.ensureLoaded(this.generation(), () => this.applySearch());
  }

  /** Tải một bộ (lần đầu), xong thì gọi then. */
  ensureLoaded(generation: Generation, then?: () => void, force = false): void {
    if (this.items()[generation] && !force) {
      then?.();
      return;
    }
    this.loading.update(map => ({ ...map, [generation]: true }));
    this.locationIndexService
      .all(generation)
      .pipe(finalize(() => this.loading.update(map => ({ ...map, [generation]: false }))))
      .subscribe(items => {
        this.items.update(map => ({ ...map, [generation]: items }));
        then?.();
      });
  }

  reload(): void {
    this.relations.set(new Map());
    this.ensureLoaded(this.generation(), () => this.applySearch(), true);
  }

  switchTo(generation: Generation): void {
    if (generation === this.generation()) {
      return;
    }
    this.generation.set(generation);
    this.focused.set(null);
    if (this.filter() === 'unmapped' && generation === 'new') {
      this.filter.set('all');
    }
    this.syncUrl();
    this.ensureLoaded(generation, () => this.applySearch());
  }

  onSearchInput(value: string): void {
    clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => this.setQuery(value), SEARCH_DELAY_MS);
  }

  /** Enter: tìm ngay, không chờ. */
  setQuery(value: string): void {
    clearTimeout(this.searchTimer);
    this.query.set(value.trim());
    this.focused.set(null);
    this.syncUrl();
    this.applySearch();
  }

  setFilter(filter: LocationFilter): void {
    this.filter.set(this.filter() === filter ? 'all' : filter);
    this.applySearch();
  }

  setSort(sort: string): void {
    this.sort.set(sort === 'name' ? 'name' : 'code');
  }

  expandAll(): void {
    const tree = this.tree();
    if (tree) {
      this.expanded.set(expandableIds(tree, this.search()));
    }
  }

  collapseAll(): void {
    this.expanded.set(new Set());
  }

  isExpanded(node: LocationNode): boolean {
    return this.expanded().has(node.item.id);
  }

  isMatch(node: LocationNode): boolean {
    return this.search()?.matches.has(node.item.id) ?? false;
  }

  /** Mở / đóng một nút; mở lần đầu thì tải quan hệ cũ ↔ mới. */
  toggle(node: LocationNode): void {
    const id = node.item.id;
    const next = new Set(this.expanded());
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
      this.loadRelation(node.item.id);
    }
    this.expanded.set(next);
  }

  relation(node: LocationNode): Relation | undefined {
    return this.relations().get(node.item.id);
  }

  hasRelation(node: LocationNode): boolean {
    return this.relations().has(node.item.id);
  }

  loadRelation(id: number): void {
    if (this.relations().has(id)) {
      return;
    }
    this.setRelation(id, undefined);
    const drop = (): void => this.relations.update(map => new Map([...map].filter(([key]) => key !== id)));
    if (this.generation() === 'new') {
      this.locationIndexService.origins(id).subscribe({ next: origins => this.setRelation(id, { origins }), error: drop });
    } else {
      this.locationIndexService.successors(id).subscribe({ next: successors => this.setRelation(id, { successors }), error: drop });
    }
  }

  /** Nhảy sang bộ kia, bỏ tìm/lọc, mở sẵn các cấp cha và chính đơn vị đó, cuộn tới. */
  reveal(generation: Generation, id: number | null): void {
    if (id === null) {
      return;
    }
    this.generation.set(generation);
    this.query.set('');
    this.filter.set('all');
    this.syncUrl();
    this.ensureLoaded(generation, () => {
      const node = this.tree()?.byId.get(id);
      if (!node) {
        return;
      }
      this.expanded.set(new Set([...ancestorsOf(node).map(a => a.item.id), id]));
      this.focused.set(id);
      this.loadRelation(id);
      afterNextRender(() => this.host.nativeElement.querySelector(`#loc-${id}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' }), {
        injector: this.injector,
      });
    });
  }

  /** Các con còn hiện: đang tìm/lọc thì chỉ nút khớp, nút cha và con cháu của nút khớp. */
  visible(nodes: LocationNode[]): LocationNode[] {
    const search = this.search();
    return search ? nodes.filter(node => search.keep.has(node.item.id)) : nodes;
  }

  listingQueryParams(node: LocationNode): Record<string, number> {
    return { 'filter[locationTreeId.equals]': node.item.id };
  }

  /** Lớp màu cho nhãn loại đơn vị. */
  kindClass(kind: string | null): string {
    switch (kind) {
      case 'Tỉnh':
      case 'Thành phố':
        return 'text-bg-primary';
      case 'Quận':
      case 'Huyện':
      case 'Thị xã':
        return 'text-bg-secondary';
      case 'Phường':
        return 'text-bg-info';
      case 'Xã':
        return 'text-bg-success';
      case 'Thị trấn':
        return 'text-bg-light border';
      case 'Đặc khu':
        return 'text-bg-warning';
      default:
        return 'text-bg-light border';
    }
  }

  /** "Quận Cầu Giấy, Thành phố Hà Nội" — vị trí cũ của đơn vị cũ. */
  originPlace(origin: LocationOrigin): string {
    return [origin.parentName, origin.provinceName].filter(Boolean).join(', ');
  }

  private applySearch(): void {
    this.expanded.set(new Set(this.search()?.expand ?? []));
    // Khớp đúng nguyên mã (gõ 00167) → mở luôn đơn vị đó cùng quan hệ cũ ↔ mới
    const tree = this.tree();
    const code = this.query();
    if (tree && /^\d{2,5}$/.test(code)) {
      for (const id of this.search()?.matches ?? []) {
        const node = tree.byId.get(id);
        if (node?.item.code === code) {
          this.expanded.update(set => new Set(set).add(id));
          this.loadRelation(id);
        }
      }
    }
  }

  private setRelation(id: number, relation: Relation | undefined): void {
    this.relations.update(map => new Map(map).set(id, relation));
  }

  private syncUrl(): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { generation: this.generation() === 'old' ? 'old' : null, q: this.query() || null },
      replaceUrl: true,
    });
  }
}
