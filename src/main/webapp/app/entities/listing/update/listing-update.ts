import { NgTemplateOutlet } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { Observable, finalize, map } from 'rxjs';

import { CategoryIndexService } from 'app/entities/category/service/category-index.service';
import { LocationIndexService, placeOf } from 'app/entities/location/service/location-index.service';
import { AlertError } from 'app/shared/alert';
import { AutoResizeDirective, ChipPicker, EditPage, PickerOption } from 'app/shared/form';
import { TranslateDirective } from 'app/shared/language';
import { RichTextEditor } from 'app/shared/rich-text/rich-text-editor';
import { LISTING_STATUSES, statusLabelKey } from '../listing-display';
import { IListing } from '../listing.model';
import { ListingService } from '../service/listing.service';

import { ListingFormGroup, ListingFormService } from './listing-form.service';

/**
 * Sửa tay: form chia nhóm, cột phải xuất bản/kỹ thuật, thanh lưu dính đáy, chọn ngành nghề/địa phương bằng gõ tìm
 * (API mục lục A–Z) thay cho select chỉ có 20 dòng đầu. Ctrl+S, hỏi khi rời trang, ?focus=: lớp {@link EditPage}.
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
})
export class ListingUpdate extends EditPage implements OnInit {
  readonly statusOptions = signal<string[]>(LISTING_STATUSES);
  listing: IListing | null = null;

  protected listingService = inject(ListingService);
  protected listingFormService = inject(ListingFormService);
  protected categoryIndexService = inject(CategoryIndexService);
  protected locationIndexService = inject(LocationIndexService);
  protected translateService = inject(TranslateService);
  protected readonly labelPrefix = 'javaSpringBootBackEndApp.listing.';
  protected readonly listRoute = '/listing';
  protected readonly technicalFields = new Set(['wpId', 'apiId', 'locationJson', 'esIndexed']);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: ListingFormGroup = this.listingFormService.createListingFormGroup();

  /** Gõ tìm ngành theo tên (không dấu) hoặc mã VSIC; nhãn là mã ngành, dòng phụ là ngành cha để phân biệt ngành trùng tên. */
  searchCategories = (term: string): Observable<PickerOption[]> =>
    this.categoryIndexService
      .search({ q: term, size: 20 })
      .pipe(map(page => page.items.map(item => ({ id: item.id, name: item.name, hint: item.parentName, badge: item.code }))));

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
      this.applyFocusParam();
    });
  }

  statusLabel(status: string): string {
    return statusLabelKey(status);
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
    this.markSaved();
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
