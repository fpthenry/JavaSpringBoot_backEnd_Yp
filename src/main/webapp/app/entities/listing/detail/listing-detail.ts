import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap/modal';
import { TranslatePipe } from '@ngx-translate/core';
import { filter } from 'rxjs';

import { ITEM_DELETED_EVENT } from 'app/config';
import { ILocation } from 'app/entities/location/location.model';
import { LocationService } from 'app/entities/location/service/location.service';
import { Alert, AlertError } from 'app/shared/alert';
import { FormatMediumDatetimePipe } from 'app/shared/date';
import { CopyButton, DetailSection, SectionNav, avatarHue, initials } from 'app/shared/detail';
import { decodeEntities } from 'app/shared/form';
import { TranslateDirective } from 'app/shared/language';
import { ListingDeleteDialog } from '../delete/listing-delete-dialog';
import {
  NEW_UNIT_SUFFIX,
  completenessChecks,
  completenessPercent,
  mapsHref,
  mapsSearchHref,
  parseLocationJson,
  splitEmails,
  splitPhones,
  statusLabelKey,
  telHref,
  websiteHref,
} from '../listing-display';
import { IListing } from '../listing.model';

/** Địa phương đã gắn, kèm cấp cha và thuộc bộ mới hay cũ (đọc thêm bằng /api/locations?id.in=…). */
export interface LinkedLocation {
  id: number;
  name: string;
  parentName: string | null;
  isNew: boolean | null;
}

/** Field chữ của nhóm "Thông tin doanh nghiệp" (mã số thuế hiện riêng vì có nút chép). */
export const BUSINESS_FIELDS = [
  'representative',
  'capital',
  'foundedYear',
  'businessType',
  'businessStatus',
  'industryCode',
  'managedBy',
] as const;
export type BusinessField = (typeof BUSINESS_FIELDS)[number];

export const LISTING_SECTIONS: DetailSection[] = [
  { id: 'section-overview', label: 'javaSpringBootBackEndApp.listing.detail.nav.overview', icon: 'building' },
  { id: 'section-contact', label: 'javaSpringBootBackEndApp.listing.section.contact', icon: 'phone' },
  { id: 'section-address', label: 'javaSpringBootBackEndApp.listing.detail.nav.address', icon: 'location-dot' },
  { id: 'section-categories', label: 'javaSpringBootBackEndApp.listing.section.categories', icon: 'briefcase' },
  { id: 'section-business', label: 'javaSpringBootBackEndApp.listing.detail.nav.business', icon: 'id-card' },
];

/**
 * Sửa tay: trang xem doanh nghiệp. Khối đầu trang (ô viết tắt, tên, nhãn, số liệu chính, nút gọi/email/website/bản đồ),
 * thanh mục lục dính, các nhóm thông tin chỉ hiện field có dữ liệu (field trống gom thành một dòng "Chưa có … · Bổ sung"),
 * cột phải mức độ đầy đủ/xuất bản/thống kê/kỹ thuật, thanh thao tác dính đáy trên điện thoại, phím E để sửa.
 */
@Component({
  selector: 'jhi-listing-detail',
  templateUrl: './listing-detail.html',
  imports: [
    FontAwesomeModule,
    Alert,
    AlertError,
    TranslateDirective,
    TranslatePipe,
    RouterLink,
    FormatMediumDatetimePipe,
    CopyButton,
    SectionNav,
  ],
  host: { '(document:keydown)': 'onKeydown($event)' },
})
export class ListingDetail {
  readonly listing = input<IListing | null>(null);

  readonly sections = LISTING_SECTIONS;
  /** Địa phương kèm cấp cha, bộ mới/cũ; trước khi đọc xong chỉ có tên. */
  readonly linkedLocations = signal<LinkedLocation[]>([]);

  readonly initials = computed(() => initials(this.listing()?.name));
  readonly avatarHue = computed(() => avatarHue(this.listing()?.name));
  readonly phones = computed(() => [...splitPhones(this.listing()?.phone), ...splitPhones(this.listing()?.mobile)]);
  readonly emails = computed(() => splitEmails(this.listing()?.email));
  readonly website = computed(() => websiteHref(this.listing()?.website));
  readonly place = computed(() => parseLocationJson(this.listing()?.locationJson));
  readonly mainAddress = computed(() => this.listing()?.addressNew ?? this.listing()?.address ?? null);
  readonly mapUrl = computed(() => mapsHref(this.place()?.coordinate) ?? mapsSearchHref(this.mainAddress()));
  readonly description = computed(() => {
    const html = this.listing()?.description ?? '';
    return html.replace(/<[^>]*>/g, '').trim() ? html : null;
  });
  /** Field doanh nghiệp có dữ liệu / còn trống. */
  readonly businessFilled = computed(() => BUSINESS_FIELDS.filter(field => !!this.listing()?.[field]?.trim()));
  readonly businessEmpty = computed(() => BUSINESS_FIELDS.filter(field => !this.listing()?.[field]?.trim()));
  readonly hasNewUnit = computed(() => this.linkedLocations().some(location => location.isNew === true));
  readonly checks = computed(() => {
    const listing = this.listing();
    return listing ? completenessChecks(listing, this.hasNewUnit()) : [];
  });
  readonly percent = computed(() => completenessPercent(this.checks()));
  readonly missing = computed(() => this.checks().filter(check => !check.ok));

  protected readonly statusLabelKey = statusLabelKey;
  protected readonly decode = decodeEntities;
  protected readonly locationService = inject(LocationService);
  protected readonly modalService = inject(NgbModal);
  protected readonly router = inject(Router);

  constructor() {
    effect(() => {
      const locations = this.listing()?.locations ?? [];
      this.linkedLocations.set(locations.map(location => ({ id: location.id, name: location.name ?? '', parentName: null, isNew: null })));
      if (locations.length > 0) {
        this.loadLocationDetails(locations.map(location => location.id));
      }
    });
  }

  telHref(phone: string): string {
    return telHref(phone);
  }

  businessValue(listing: IListing, field: BusinessField): string {
    return listing[field] ?? '';
  }

  editQueryParams(field: string): Record<string, string> {
    return { focus: field };
  }

  categoryQueryParams(id: number): Record<string, number> {
    return { 'filter[categoryTreeId.equals]': id };
  }

  locationQueryParams(id: number): Record<string, number> {
    return { 'filter[locationTreeId.equals]': id };
  }

  /** Phím E (không gõ trong ô nhập, không giữ Ctrl/Alt): mở form sửa. */
  onKeydown(event: KeyboardEvent): void {
    const target = event.target as HTMLElement | null;
    const typing = !!target?.closest('input, textarea, select, [contenteditable="true"]');
    const listing = this.listing();
    if (listing && !typing && !event.ctrlKey && !event.metaKey && !event.altKey && event.key.toLowerCase() === 'e') {
      event.preventDefault();
      void this.router.navigate(['/listing', listing.id, 'edit']);
    }
  }

  delete(listing: IListing): void {
    const modalRef = this.modalService.open(ListingDeleteDialog, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.listing = listing;
    modalRef.closed.pipe(filter(reason => reason === ITEM_DELETED_EVENT)).subscribe(() => void this.router.navigate(['/listing']));
  }

  /** Về trang trước trong ứng dụng; mở thẳng trang xem (link, tab mới) thì về danh sách. */
  previousState(): void {
    const navigationId = (globalThis.history.state as { navigationId?: number } | null)?.navigationId ?? 1;
    if (navigationId > 1) {
      globalThis.history.back();
    } else {
      void this.router.navigate(['/listing']);
    }
  }

  protected loadLocationDetails(ids: number[]): void {
    this.locationService.query({ 'id.in': ids.join(','), size: ids.length }).subscribe({
      next: response => {
        const byId = new Map((response.body ?? []).map((location: ILocation) => [location.id, location]));
        this.linkedLocations.update(current =>
          current.map(location => {
            const detail = byId.get(location.id);
            return detail
              ? {
                  ...location,
                  name: detail.name ?? location.name,
                  parentName: detail.parent?.name ?? null,
                  isNew: detail.slug ? detail.slug.endsWith(NEW_UNIT_SUFFIX) : null,
                }
              : location;
          }),
        );
      },
      // Không đọc được chi tiết: vẫn hiện tên như cũ
      error: () => undefined,
    });
  }
}
