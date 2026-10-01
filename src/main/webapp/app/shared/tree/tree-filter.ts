import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { TranslatePipe } from '@ngx-translate/core';
import { Observable, forkJoin, map, of, switchMap } from 'rxjs';

import { IFilterOptions } from 'app/shared/filter';
import { TreeItem, TreeSource } from './tree-source';

export interface TreeFilterLevel {
  options: TreeItem[];
  selected: number | null;
}

/**
 * Bộ lọc theo cây: một dãy ô chọn nối tiếp (cấp 1 -> cấp 2 -> ...), cấp sau chỉ hiện khi nút đã chọn có con.
 * Đặt filter `filterName` (vd `locationTreeId.equals`) bằng nút sâu nhất được chọn; đọc lại lựa chọn từ URL khi tải trang.
 */
@Component({
  selector: 'jhi-tree-filter',
  templateUrl: './tree-filter.html',
  imports: [FormsModule, TranslatePipe],
})
export class TreeFilter {
  readonly filters = input.required<IFilterOptions>();
  readonly source = input.required<TreeSource>();
  readonly filterName = input.required<string>();
  /** Khóa i18n cho lựa chọn "tất cả" theo từng cấp; cấp sâu hơn dùng khóa cuối. */
  readonly placeholders = input.required<string[]>();

  readonly levels = signal<TreeFilterLevel[]>([]);

  protected readonly queryParamMap = toSignal(inject(ActivatedRoute).queryParamMap);
  protected readonly selectedInUrl = computed(() => Number(this.queryParamMap()?.get(`filter[${this.filterName()}]`)) || null);

  constructor() {
    effect(() => {
      const selected = this.selectedInUrl();
      this.source();
      untracked(() => {
        // Chỉ dựng lại khi URL đổi từ bên ngoài (tải trang, xóa filter, mở link) hoặc lần đầu
        if (this.levels().length === 0 || selected !== this.deepestSelected()) {
          this.restore(selected);
        }
      });
    });
  }

  placeholder(levelIndex: number): string {
    const placeholders = this.placeholders();
    return placeholders[Math.min(levelIndex, placeholders.length - 1)];
  }

  onSelect(levelIndex: number, id: number | null): void {
    this.levels.update(levels => [...levels.slice(0, levelIndex), { ...levels[levelIndex], selected: id }]);
    this.applyFilter();
    if (id === null) {
      return;
    }
    this.source()
      .children(id)
      .subscribe(children => {
        // Bỏ qua kết quả cũ nếu người dùng đã chọn nút khác trong lúc chờ
        if (children.length > 0 && this.levels()[levelIndex]?.selected === id) {
          this.levels.update(levels => [...levels.slice(0, levelIndex + 1), { options: children, selected: null }]);
        }
      });
  }

  protected deepestSelected(): number | null {
    return (
      this.levels()
        .map(level => level.selected)
        .filter(selected => selected !== null)
        .at(-1) ?? null
    );
  }

  /**
   * Thay giá trị filter cũ bằng nút sâu nhất đang chọn.
   * Trang danh sách đọc filter qua signal nên các thay đổi liên tiếp chỉ gây một lần tải lại.
   */
  protected applyFilter(): void {
    const filters = this.filters();
    const name = this.filterName();
    const selected = this.deepestSelected();
    const previous = filters.filterOptions.find(filterOption => filterOption.name === name)?.values ?? [];
    previous.filter(value => value !== String(selected)).forEach(value => filters.removeFilter(name, value));
    if (selected !== null) {
      filters.addFilter(name, String(selected));
    }
  }

  /** Dựng lại các ô chọn: đi ngược lên cha để có chuỗi [gốc, ..., id], rồi tải danh sách lựa chọn của từng cấp. */
  protected restore(id: number | null): void {
    const source = this.source();
    const chain$: Observable<number[]> = id === null ? of([]) : this.ancestors(id);
    chain$
      .pipe(
        switchMap(chain =>
          forkJoin([source.roots(), ...chain.map(nodeId => source.children(nodeId))]).pipe(
            map(optionsPerLevel =>
              optionsPerLevel
                .map((options, index) => ({ options, selected: chain.at(index) ?? null }))
                // Cấp cuối (con của nút sâu nhất) chỉ hiện nếu có con
                .filter((level, index) => index < chain.length || level.options.length > 0),
            ),
          ),
        ),
      )
      .subscribe(levels => this.levels.set(levels));
  }

  protected ancestors(id: number): Observable<number[]> {
    return this.source()
      .find(id)
      .pipe(switchMap(node => (node.parent ? this.ancestors(node.parent.id).pipe(map(chain => [...chain, id])) : of([id]))));
  }
}
