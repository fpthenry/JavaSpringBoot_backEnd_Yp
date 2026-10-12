import { NgTemplateOutlet } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { Observable, finalize, map } from 'rxjs';

import { CategoryIndexService } from 'app/entities/category/service/category-index.service';
import { LocationIndexService, placeOf } from 'app/entities/location/service/location-index.service';
import { AlertError } from 'app/shared/alert';
import { AutoResizeDirective, ChipPicker, HasUnsavedChanges, PickerOption } from 'app/shared/form';
import { TranslateDirective } from 'app/shared/language';
import { RichTextEditor } from 'app/shared/rich-text/rich-text-editor';
import { LISTING_STATUSES, statusLabelKey } from '../listing-display';
import { IListing } from '../listing.model';
import { ListingService } from '../service/listing.service';

import { ListingFormGroup, ListingFormService } from './listing-form.service';

/** Nhãn hiển thị của các field (dùng cho danh sách "Kiểm tra lại"). */
const FIELD_LABELS: Record<string, string> = {
  name: 'javaSpringBootBackEndApp.listing.name',
  wpId: 'javaSpringBootBackEndApp.listing.wpId',
};
/** Field nằm trong khối "Thông tin kỹ thuật" (đang thu gọn): phải mở khối mới focus được. */
const TECHNICAL_FIELDS = new Set(['wpId', 'apiId', 'locationJson', 'esIndexed']);

/**
 * Sửa tay: form chia nhóm, cột phải xuất bản/kỹ thuật, thanh lưu dính đáy, Ctrl+S, hỏi lại khi rời trang chưa lưu,
 * chọn ngành nghề/địa phương bằng gõ tìm (API mục lục A–Z) thay cho select chỉ có 20 dòng đầu.
 */
@Component({
  selector: 'jhi-listing-update',
  templateUrl: './listing-update.html',
  imports: [
    TranslateDirective,
    TranslatePipe,
    FontAwesomeModule,
    AlertError,
    ReactiveFormsModule,
    RouterLink,
    NgTemplateOutlet,
    AutoResizeDirective,
    ChipPicker,
    RichTextEditor,
  ],
  host: {
    '(window:beforeunload)': 'onBeforeUnload($event)',
    '(document:keydown.control.s)': 'onSaveShortcut($event)',
    '(document:keydown.meta.s)': 'onSaveShortcut($event)',
  },
})
export class ListingUpdate implements OnInit, HasUnsavedChanges {
  readonly isSaving = signal(false);
  readonly technicalOpen = signal(false);
  readonly statusOptions = signal<string[]>(LISTING_STATUSES);
  listing: IListing | null = null;

  protected listingService = inject(ListingService);
  protected listingFormService = inject(ListingFormService);
  protected categoryIndexService = inject(CategoryIndexService);
  protected locationIndexService = inject(LocationIndexService);
  protected translateService = inject(TranslateService);
  protected activatedRoute = inject(ActivatedRoute);
  protected router = inject(Router);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: ListingFormGroup = this.listingFormService.createListingFormGroup();

  private saved = false;

  /** Gõ tìm ngành: tìm không dấu trong tên, hiện ngành cha để phân biệt ngành trùng tên. */
  searchCategories = (term: string): Observable<PickerOption[]> =>
    this.categoryIndexService
      .search({ q: term, size: 20 })
      .pipe(map(page => page.items.map(item => ({ id: item.id, name: item.name, hint: item.parentName }))));

  /** Gõ tìm địa phương trong cả bộ mới và cũ; dòng phụ là quận/tỉnh, đơn vị cũ có nhãn "Cũ". */
  searchLocations = (term: string): Observable<PickerOption[]> =>
    this.locationIndexService.search({ q: term, generation: 'all', size: 20 }).pipe(
      map(page =>
        page.items.map(item => ({
          id: item.id,
          name: item.name,
          hint: placeOf(item),
          badge: item.isNew ? null : this.translateService.instant('javaSpringBootBackEndApp.location.index.oldBadge'),
        })),
      ),
    );

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ listing }) => {
      this.listing = listing;
      if (listing) {
        this.updateForm(listing);
      } else {
        // Thêm mới: mặc định bản nháp, mở khối kỹ thuật vì Mã WordPress bắt buộc
        this.editForm.patchValue({ status: 'draft' });
        this.technicalOpen.set(true);
      }
      // Từ trang xem, "Bổ sung" một mục còn thiếu: mở form và nhảy tới đúng field (?focus=email)
      const focus = this.activatedRoute.snapshot.queryParamMap.get('focus');
      if (focus && Object.prototype.hasOwnProperty.call(this.editForm.controls, focus)) {
        this.focusField(focus);
      }
    });
  }

  statusLabel(status: string): string {
    return statusLabelKey(status);
  }

  /** Các field đang sai, để thanh lưu liệt kê và bấm vào thì nhảy tới. */
  invalidFields(): { key: string; label: string }[] {
    return Object.entries(this.editForm.controls)
      .filter(([, control]) => control.invalid)
      .map(([key]) => ({ key, label: FIELD_LABELS[key] ?? `javaSpringBootBackEndApp.listing.${key}` }));
  }

  focusField(key: string): void {
    if (TECHNICAL_FIELDS.has(key)) {
      this.technicalOpen.set(true);
    }
    this.editForm.get(key)?.markAsTouched();
    setTimeout(() => {
      const element = document.getElementById(`field_${key}`);
      element?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      element?.focus({ preventScroll: true });
      // Nháy viền để người dùng thấy ngay ô cần nhập (ô chọn nhiều, trình soạn thảo: nháy cả khung)
      const box = element?.closest('.chip-picker, .rte-compact') ?? element;
      box?.classList.remove('field-flash');
      void (box as HTMLElement | null)?.offsetWidth;
      box?.classList.add('field-flash');
    });
  }

  hasUnsavedChanges(): boolean {
    return this.editForm.dirty && !this.saved;
  }

  onBeforeUnload(event: BeforeUnloadEvent): void {
    if (this.hasUnsavedChanges()) {
      event.preventDefault();
    }
  }

  onSaveShortcut(event: Event): void {
    event.preventDefault();
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      const first = this.invalidFields().at(0);
      if (first) {
        this.focusField(first.key);
      }
    } else if (!this.isSaving()) {
      this.save();
    }
  }

  /** Về trang trước trong ứng dụng; mở thẳng trang sửa (link, tab mới) thì về danh sách thay vì rời ứng dụng. */
  previousState(): void {
    const navigationId = (globalThis.history.state as { navigationId?: number } | null)?.navigationId ?? 1;
    if (navigationId > 1) {
      globalThis.history.back();
    } else {
      void this.router.navigate(['/listing']);
    }
  }

  save(): void {
    this.isSaving.set(true);
    const listing = this.listingFormService.getListing(this.editForm);
    if (listing.id === null) {
      this.subscribeToSaveResponse(this.listingService.create(listing));
    } else {
      this.subscribeToSaveResponse(this.listingService.update(listing));
    }
  }

  protected subscribeToSaveResponse(result: Observable<IListing | null>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.saved = true;
    this.previousState();
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }

  protected onSaveFinalize(): void {
    this.isSaving.set(false);
  }

  protected updateForm(listing: IListing): void {
    this.listing = listing;
    this.listingFormService.resetForm(this.editForm, listing);
    if (listing.status && !LISTING_STATUSES.includes(listing.status)) {
      this.statusOptions.set([...LISTING_STATUSES, listing.status]);
    }
  }
}
