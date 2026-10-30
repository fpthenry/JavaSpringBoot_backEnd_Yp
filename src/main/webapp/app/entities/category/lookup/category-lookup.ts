import { NgTemplateOutlet } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { AlertError } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { CategoryIndexService } from '../service/category-index.service';

import { VsicNode, VsicTree, buildVsicTree, expandableIds, searchVsic } from './vsic-tree';

/** Mô tả của một ngành: undefined = đang tải, null = không có mô tả. */
type Detail = string | null | undefined;

const SEARCH_DELAY_MS = 200;

/**
 * Sửa tay: tra cứu mã ngành VSIC 2025 dạng cây (A → 01 → 011 → 0111 → 01110), giống bảng tra cứu của
 * dangkykinhdoanh.gov.vn: một ô tìm theo mã hoặc tên (không dấu), nút Mở hết / Thu gọn, mở một ngành thì hiện phần
 * "Chi tiết" (mô tả ngành) và các ngành con. Tải cả cây một lần (~1.600 ngành), tìm ngay trên trình duyệt; mô tả tải
 * riêng khi mở từng ngành.
 */
@Component({
  selector: 'jhi-category-lookup',
  templateUrl: './category-lookup.html',
  styleUrl: './category-lookup.scss',
  imports: [NgTemplateOutlet, RouterLink, FontAwesomeModule, TranslatePipe, TranslateDirective, AlertError],
})
export class CategoryLookup {
  readonly tree = signal<VsicTree>(buildVsicTree([]));
  readonly isLoading = signal(false);
  /** Từ khóa đang áp dụng (sau khi ngừng gõ). */
  readonly query = signal('');
  readonly search = computed(() => searchVsic(this.tree(), this.query()));
  readonly expanded = signal<ReadonlySet<number>>(new Set());
  readonly details = signal<ReadonlyMap<number, Detail>>(new Map());
  readonly roots = computed(() => this.visible(this.tree().roots));

  protected readonly categoryIndexService = inject(CategoryIndexService);
  private searchTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    inject(DestroyRef).onDestroy(() => clearTimeout(this.searchTimer));
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.categoryIndexService
      .all()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe(items => {
        this.tree.set(buildVsicTree(items));
        this.details.set(new Map());
        this.applySearch();
      });
  }

  onSearchInput(value: string): void {
    clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => this.setQuery(value), SEARCH_DELAY_MS);
  }

  /** Enter: tìm ngay, không chờ. */
  setQuery(value: string): void {
    clearTimeout(this.searchTimer);
    this.query.set(value.trim());
    this.applySearch();
  }

  expandAll(): void {
    this.expanded.set(expandableIds(this.tree(), this.search()));
  }

  collapseAll(): void {
    this.expanded.set(new Set());
  }

  isExpanded(node: VsicNode): boolean {
    return this.expanded().has(node.item.id);
  }

  isMatch(node: VsicNode): boolean {
    return this.search()?.matches.has(node.item.id) ?? false;
  }

  /** Mở / đóng một ngành; mở lần đầu thì tải mô tả. */
  toggle(node: VsicNode): void {
    const id = node.item.id;
    const next = new Set(this.expanded());
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
      this.loadDetail(node);
    }
    this.expanded.set(next);
  }

  hasDetail(node: VsicNode): boolean {
    return this.details().has(node.item.id);
  }

  detail(node: VsicNode): Detail {
    return this.details().get(node.item.id);
  }

  loadDetail(node: VsicNode): void {
    const id = node.item.id;
    if (this.details().has(id)) {
      return;
    }
    this.setDetail(id, undefined);
    this.categoryIndexService.detail(id).subscribe({
      // Mô tả VSIC có dòng trống giữa các ý ("- Thu nước mưa;"): bỏ cho gọn như bảng gốc
      next: detail => this.setDetail(id, detail.description?.replace(/\n\s*\n/g, '\n').trim() ?? null),
      error: () => this.details.update(map => new Map([...map].filter(([key]) => key !== id))),
    });
  }

  /** Các con còn hiện: đang tìm thì chỉ các ngành khớp, ngành cha và con cháu của ngành khớp. */
  visible(nodes: VsicNode[]): VsicNode[] {
    const search = this.search();
    return search ? nodes.filter(node => search.keep.has(node.item.id)) : nodes;
  }

  listingQueryParams(node: VsicNode): Record<string, number> {
    return { 'filter[categoryTreeId.equals]': node.item.id };
  }

  private applySearch(): void {
    this.expanded.set(new Set(this.search()?.expand ?? []));
    // Khớp đúng nguyên mã (gõ 4632) thì hiện luôn phần chi tiết của ngành đó
    const code = this.query().toUpperCase();
    for (const id of this.search()?.expand ?? []) {
      const node = this.tree().byId.get(id);
      if (node?.item.code === code) {
        this.loadDetail(node);
      }
    }
  }

  private setDetail(id: number, detail: Detail): void {
    this.details.update(map => new Map(map).set(id, detail));
  }
}
