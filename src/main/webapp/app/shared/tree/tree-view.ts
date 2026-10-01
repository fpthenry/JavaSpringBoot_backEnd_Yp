import { NgTemplateOutlet } from '@angular/common';
import { Component, effect, input, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { IconProp } from '@fortawesome/fontawesome-svg-core';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';

import { TranslateDirective } from 'app/shared/language';
import { TreeItem, TreeSource } from './tree-source';

export interface TreeNode {
  item: TreeItem;
  expanded: ReturnType<typeof signal<boolean>>;
  loading: ReturnType<typeof signal<boolean>>;
  children: ReturnType<typeof signal<TreeNode[] | undefined>>;
}

/**
 * Cây tải dần: chỉ tải các nút gốc, mở nút nào mới tải con của nút đó.
 * Mỗi nút có link xem chi tiết và link xem danh sách doanh nghiệp đã lọc theo cả cây con.
 */
@Component({
  selector: 'jhi-tree-view',
  templateUrl: './tree-view.html',
  imports: [NgTemplateOutlet, RouterLink, FontAwesomeModule, TranslateDirective, TranslatePipe],
})
export class TreeView {
  readonly source = input.required<TreeSource>();
  /** Đường dẫn trang entity, vd `/location`; link chi tiết là `<entityRoute>/<id>/view`. */
  readonly entityRoute = input.required<string>();
  /** Tên filter của trang Listing, vd `locationTreeId.equals`. */
  readonly listingFilterName = input.required<string>();
  /** Biết trước nút lá (vd phường/xã) thì không hiện nút mở; mặc định coi là lá khi đã tải và không có con. */
  readonly isLeaf = input<(item: TreeItem) => boolean>(() => false);
  /** Khóa i18n mô tả nút (vd cấp hành chính), không có thì bỏ trống. */
  readonly itemLabel = input<(item: TreeItem) => string | null>(() => null);
  /** Icon cho nút lá. */
  readonly leafIcon = input<IconProp>('circle');

  readonly roots = signal<TreeNode[]>([]);
  readonly isLoading = signal(false);

  constructor() {
    effect(() => {
      this.source();
      untracked(() => this.load());
    });
  }

  load(): void {
    this.isLoading.set(true);
    this.source()
      .roots()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe(items => this.roots.set(items.map(item => this.toNode(item))));
  }

  leaf(node: TreeNode): boolean {
    return this.isLeaf()(node.item) || node.children()?.length === 0;
  }

  toggle(node: TreeNode): void {
    if (node.expanded()) {
      node.expanded.set(false);
      return;
    }
    if (node.children() !== undefined) {
      node.expanded.set(true);
      return;
    }
    node.loading.set(true);
    this.source()
      .children(node.item.id)
      .pipe(finalize(() => node.loading.set(false)))
      .subscribe(children => {
        node.children.set(children.map(child => this.toNode(child)));
        node.expanded.set(true);
      });
  }

  listingQueryParams(node: TreeNode): Record<string, number> {
    return { [`filter[${this.listingFilterName()}]`]: node.item.id };
  }

  protected toNode(item: TreeItem): TreeNode {
    return { item, expanded: signal(false), loading: signal(false), children: signal<TreeNode[] | undefined>(undefined) };
  }
}
