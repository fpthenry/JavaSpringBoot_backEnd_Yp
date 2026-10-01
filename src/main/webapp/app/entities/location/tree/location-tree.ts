import { NgTemplateOutlet } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable, finalize, map } from 'rxjs';

import { AlertError } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { ILocation } from '../location.model';
import { LocationService } from '../service/location.service';

/** Cấp cuối (phường/xã) không có con. */
const LEAF_TYPE = 'ward';
/** Một cấp có tối đa vài trăm đơn vị con, tải một lần là đủ. */
const CHILDREN_PAGE_SIZE = 1000;

export interface LocationNode {
  location: ILocation;
  expanded: ReturnType<typeof signal<boolean>>;
  loading: ReturnType<typeof signal<boolean>>;
  children: ReturnType<typeof signal<LocationNode[] | undefined>>;
}

/**
 * Cây đơn vị hành chính: tỉnh/thành -> quận/huyện -> phường/xã.
 * Tải dần từng cấp khi mở nút, không tải cả 15K địa phương một lần.
 */
@Component({
  selector: 'jhi-location-tree',
  templateUrl: './location-tree.html',
  imports: [NgTemplateOutlet, RouterLink, FontAwesomeModule, AlertError, TranslateDirective, TranslatePipe],
})
export class LocationTree {
  readonly roots = signal<LocationNode[]>([]);
  readonly isLoading = signal(false);

  protected readonly locationService = inject(LocationService);

  constructor() {
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.query({ 'parentId.specified': false, sort: ['id,asc'] })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe(nodes => this.roots.set(nodes));
  }

  isLeaf(node: LocationNode): boolean {
    return node.location.type === LEAF_TYPE;
  }

  toggle(node: LocationNode): void {
    if (node.expanded()) {
      node.expanded.set(false);
      return;
    }
    if (node.children() !== undefined) {
      node.expanded.set(true);
      return;
    }
    node.loading.set(true);
    this.query({ 'parentId.equals': node.location.id, sort: ['name,asc'] })
      .pipe(finalize(() => node.loading.set(false)))
      .subscribe(children => {
        node.children.set(children);
        node.expanded.set(true);
      });
  }

  protected query(criteria: Record<string, unknown>): Observable<LocationNode[]> {
    return this.locationService
      .query({ ...criteria, page: 0, size: CHILDREN_PAGE_SIZE })
      .pipe(map(res => (res.body ?? []).map(location => this.toNode(location))));
  }

  protected toNode(location: ILocation): LocationNode {
    return { location, expanded: signal(false), loading: signal(false), children: signal<LocationNode[] | undefined>(undefined) };
  }
}
