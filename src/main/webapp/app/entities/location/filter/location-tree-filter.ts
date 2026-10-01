import { Component, effect, inject, input, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { Observable, map } from 'rxjs';

import { IFilterOptions } from 'app/shared/filter';
import { TranslateDirective } from 'app/shared/language';
import { ILocation } from '../location.model';
import { LocationService } from '../service/location.service';

/** Tên filter gửi lên API: listing thuộc địa phương này hoặc bất kỳ cấp con nào. */
export const LOCATION_TREE_FILTER = 'locationTreeId.equals';
const LOCATION_TREE_QUERY_PARAM = `filter[${LOCATION_TREE_FILTER}]`;
/** Một cấp có tối đa vài trăm đơn vị con, tải một lần là đủ. */
const OPTIONS_PAGE_SIZE = 1000;

/**
 * Bộ lọc địa phương 3 cấp: Tỉnh/thành -> Quận/huyện -> Phường/xã.
 * Đặt filter `locationTreeId.equals` bằng cấp sâu nhất được chọn; đọc lại lựa chọn từ URL khi tải trang.
 */
@Component({
  selector: 'jhi-location-tree-filter',
  templateUrl: './location-tree-filter.html',
  imports: [FormsModule, TranslateDirective],
})
export class LocationTreeFilter {
  readonly filters = input.required<IFilterOptions>();

  readonly provinces = signal<ILocation[]>([]);
  readonly districts = signal<ILocation[]>([]);
  readonly wards = signal<ILocation[]>([]);

  readonly provinceId = signal<number | null>(null);
  readonly districtId = signal<number | null>(null);
  readonly wardId = signal<number | null>(null);

  protected readonly locationService = inject(LocationService);
  protected readonly selectedInUrl = toSignal(
    inject(ActivatedRoute).queryParamMap.pipe(map(params => Number(params.get(LOCATION_TREE_QUERY_PARAM)) || null)),
  );

  constructor() {
    this.children(null).subscribe(provinces => this.provinces.set(provinces));
    effect(() => {
      const selected = this.selectedInUrl() ?? null;
      untracked(() => {
        // Chỉ đồng bộ khi URL đổi từ bên ngoài (tải lại trang, xóa filter, mở link từ cây đơn vị hành chính)
        if (selected !== this.deepestSelected()) {
          this.restore(selected);
        }
      });
    });
  }

  onProvinceChange(id: number | null): void {
    this.provinceId.set(id);
    this.districtId.set(null);
    this.wardId.set(null);
    this.districts.set([]);
    this.wards.set([]);
    if (id) {
      this.children(id).subscribe(districts => this.districts.set(districts));
    }
    this.applyFilter();
  }

  onDistrictChange(id: number | null): void {
    this.districtId.set(id);
    this.wardId.set(null);
    this.wards.set([]);
    if (id) {
      this.children(id).subscribe(wards => this.wards.set(wards));
    }
    this.applyFilter();
  }

  onWardChange(id: number | null): void {
    this.wardId.set(id);
    this.applyFilter();
  }

  protected deepestSelected(): number | null {
    return this.wardId() ?? this.districtId() ?? this.provinceId();
  }

  /**
   * Thay giá trị filter cũ bằng cấp sâu nhất đang chọn.
   * Trang danh sách đọc filter qua signal nên các thay đổi liên tiếp chỉ gây một lần tải lại.
   */
  protected applyFilter(): void {
    const filters = this.filters();
    const selected = this.deepestSelected();
    const previous = filters.filterOptions.find(filterOption => filterOption.name === LOCATION_TREE_FILTER)?.values ?? [];
    previous.filter(value => value !== String(selected)).forEach(value => filters.removeFilter(LOCATION_TREE_FILTER, value));
    if (selected !== null) {
      filters.addFilter(LOCATION_TREE_FILTER, String(selected));
    }
  }

  /** Dựng lại 3 ô chọn từ id trong URL: đi ngược lên cha để biết tỉnh, quận/huyện, phường/xã. */
  protected restore(id: number | null): void {
    this.provinceId.set(null);
    this.districtId.set(null);
    this.wardId.set(null);
    this.districts.set([]);
    this.wards.set([]);
    if (id === null) {
      return;
    }
    this.locationService.find(id).subscribe(location => {
      const chain = [location.id];
      const walkUp = (parentId: number | undefined): void => {
        if (parentId === undefined) {
          this.select(chain.reverse());
          return;
        }
        chain.push(parentId);
        this.locationService.find(parentId).subscribe(parent => walkUp(parent.parent?.id));
      };
      walkUp(location.parent?.id);
    });
  }

  /** chain: [tỉnh, quận/huyện?, phường/xã?] */
  protected select(chain: number[]): void {
    const province = chain.at(0) ?? null;
    const district = chain.at(1) ?? null;
    this.provinceId.set(province);
    this.districtId.set(district);
    this.wardId.set(chain.at(2) ?? null);
    if (province) {
      this.children(province).subscribe(districts => this.districts.set(districts));
    }
    if (district) {
      this.children(district).subscribe(wards => this.wards.set(wards));
    }
  }

  protected children(parentId: number | null): Observable<ILocation[]> {
    const criteria =
      parentId === null ? { 'parentId.specified': false, sort: ['id,asc'] } : { 'parentId.equals': parentId, sort: ['name,asc'] };
    return this.locationService.query({ ...criteria, page: 0, size: OPTIONS_PAGE_SIZE }).pipe(map(res => res.body ?? []));
  }
}
