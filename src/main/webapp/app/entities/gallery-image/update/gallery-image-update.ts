import { Component, ElementRef, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormControl, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { TranslatePipe } from '@ngx-translate/core';
import dayjs from 'dayjs/esm';
import { Observable, finalize, startWith } from 'rxjs';

import { DATE_FORMAT } from 'app/config';
import { DataUtils, EventManager, EventWithContent, FileLoadError } from 'app/core/util';
import { IGallery, IGalleryWithImages } from 'app/entities/gallery/gallery.model';
import { GalleryService } from 'app/entities/gallery/service/gallery.service';
import { AlertError, AlertErrorModel } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { IGalleryImage, NewGalleryImage } from '../gallery-image.model';
import { GalleryImageService } from '../service/gallery-image.service';

import { GalleryImageFormGroup, GalleryImageFormService } from './gallery-image-form.service';

/**
 * Sửa tay: form thêm/sửa banner tinh gọn. Chỉ 5 ô chính: Tên, Ảnh (tải lên hoặc dán link), Vị trí hiển thị, Ngày hiển thị,
 * Ngày kết thúc (bỏ trống = không giới hạn). Link khi bấm, mở tab mới, bật/tắt, thứ tự nằm trong "Tùy chọn thêm" (đóng sẵn).
 * Thứ tự tự xếp cuối vị trí; mô tả ảnh (alt) lấy theo tên nếu bỏ trống.
 */
@Component({
  selector: 'jhi-gallery-image-update',
  templateUrl: './gallery-image-update.html',
  styleUrl: './gallery-image-update.scss',
  imports: [TranslateDirective, TranslatePipe, FontAwesomeModule, AlertError, ReactiveFormsModule],
})
export class GalleryImageUpdate implements OnInit {
  readonly isSaving = signal(false);
  readonly showMore = signal(false);
  readonly useLink = signal(false);
  readonly dragOver = signal(false);
  galleryImage: IGalleryImage | null = null;

  /** Vị trí hiển thị kèm ảnh (để biết thứ tự cuối cùng của mỗi vị trí). */
  readonly positions = signal<IGalleryWithImages[]>([]);

  protected dataUtils = inject(DataUtils);
  protected eventManager = inject(EventManager);
  protected galleryImageService = inject(GalleryImageService);
  protected galleryImageFormService = inject(GalleryImageFormService);
  protected galleryService = inject(GalleryService);
  protected elementRef = inject(ElementRef);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: GalleryImageFormGroup = this.galleryImageFormService.createGalleryImageFormGroup();

  /** Ngày dạng YYYY-MM-DD cho ô chọn ngày; khi lưu đổi thành đầu ngày / cuối ngày. Bỏ trống ngày hiển thị = hiển thị ngay. */
  // eslint-disable-next-line @typescript-eslint/member-ordering
  readonly startDate = new FormControl<string>(dayjs().format(DATE_FORMAT), { nonNullable: true });
  // eslint-disable-next-line @typescript-eslint/member-ordering
  readonly endDate = new FormControl<string>('', { nonNullable: true, validators: [control => this.endAfterStart(control)] });

  /** Ảnh xem trước: ảnh vừa chọn/đã upload (base64) hoặc link ảnh. */
  private readonly formValue = toSignal(this.editForm.valueChanges.pipe(startWith(this.editForm.getRawValue())), {
    initialValue: this.editForm.getRawValue(),
  });

  // eslint-disable-next-line @typescript-eslint/member-ordering
  readonly preview = computed(() => {
    const { image, imageContentType, imageUrl } = this.formValue();
    if (image) {
      return `data:${imageContentType ?? 'image/*'};base64,${image}`;
    }
    return imageUrl?.trim() ? imageUrl.trim() : null;
  });

  ngOnInit(): void {
    const { title, gallery, displayOrder } = this.editForm.controls;
    title.addValidators([Validators.required]);
    // Thứ tự tự tính khi lưu (xếp cuối vị trí) nếu người dùng không nhập.
    displayOrder.clearValidators();
    displayOrder.addValidators([Validators.min(0)]);
    gallery.addValidators([Validators.required]);
    // Đổi luật kiểm tra xong phải tính lại, không thì trạng thái hợp lệ vẫn theo luật cũ.
    for (const control of [title, displayOrder, gallery]) {
      control.updateValueAndValidity();
    }
    this.startDate.valueChanges.subscribe(() => this.endDate.updateValueAndValidity());

    this.activatedRoute.data.subscribe(({ galleryImage }) => {
      this.galleryImage = galleryImage;
      if (galleryImage) {
        this.updateForm(galleryImage);
      } else {
        this.editForm.controls.active.setValue(true);
      }
      this.loadPositions();
    });
  }

  get isNew(): boolean {
    return !this.galleryImage?.id;
  }

  /** Banner phải có ảnh: tải lên hoặc link. */
  get missingImage(): boolean {
    return !this.preview();
  }

  isInvalid(control: AbstractControl): boolean {
    return control.invalid && (control.dirty || control.touched);
  }

  compareGallery = (o1: IGallery | null, o2: IGallery | null): boolean => this.galleryService.compareGallery(o1, o2);

  setFileData(event: Event): void {
    this.dataUtils.loadFileToForm(event, this.editForm, 'image', true).subscribe({
      next: () => {
        // Đã tải ảnh lên thì bỏ link ảnh cũ cho khỏi lẫn.
        this.editForm.patchValue({ imageUrl: null });
        this.useLink.set(false);
        this.editForm.markAsDirty();
      },
      error: (err: FileLoadError) =>
        this.eventManager.broadcast(
          new EventWithContent<AlertErrorModel>('javaSpringBootBackEndApp.error', { ...err, key: `error.file.${err.key}` }),
        ),
    });
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    const files = event.dataTransfer?.files;
    if (files?.length) {
      this.setFileData({ target: { files } } as unknown as Event);
    }
  }

  onDragOver(event: DragEvent, over: boolean): void {
    event.preventDefault();
    this.dragOver.set(over);
  }

  clearImage(): void {
    this.editForm.patchValue({ image: null, imageContentType: null, imageUrl: null });
    const input = this.elementRef.nativeElement.querySelector('#field_image');
    if (input) {
      input.value = null;
    }
  }

  toggleLink(): void {
    this.useLink.update(use => !use);
  }

  previousState(): void {
    globalThis.history.back();
  }

  save(): void {
    if (this.editForm.invalid || this.startDate.invalid || this.endDate.invalid || this.missingImage) {
      this.editForm.markAllAsTouched();
      this.startDate.markAsTouched();
      this.endDate.markAsTouched();
      return;
    }
    this.isSaving.set(true);
    const galleryImage = this.galleryImageFormService.getGalleryImage(this.editForm);
    const title = galleryImage.title?.trim() ?? null;
    const payload = {
      ...galleryImage,
      title,
      altText: galleryImage.altText?.trim() ? galleryImage.altText.trim() : title,
      imageUrl: galleryImage.image || !galleryImage.imageUrl?.trim() ? null : galleryImage.imageUrl.trim(),
      linkUrl: galleryImage.linkUrl?.trim() ? galleryImage.linkUrl.trim() : null,
      displayOrder: galleryImage.displayOrder ?? this.nextOrder(galleryImage.gallery?.id),
      startAt: this.startDate.value ? dayjs(this.startDate.value).startOf('day') : null,
      endAt: this.endDate.value ? dayjs(this.endDate.value).endOf('day') : null,
    };
    if (payload.id === null) {
      this.subscribeToSaveResponse(this.galleryImageService.create(payload));
    } else {
      this.subscribeToSaveResponse(this.galleryImageService.update(payload));
    }
  }

  protected subscribeToSaveResponse(result: Observable<IGalleryImage | null>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.previousState();
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }

  protected onSaveFinalize(): void {
    this.isSaving.set(false);
  }

  protected updateForm(galleryImage: IGalleryImage): void {
    this.galleryImage = galleryImage;
    this.galleryImageFormService.resetForm(this.editForm, galleryImage);
    this.startDate.setValue(galleryImage.startAt ? galleryImage.startAt.format(DATE_FORMAT) : '');
    this.endDate.setValue(galleryImage.endAt ? galleryImage.endAt.format(DATE_FORMAT) : '');
    this.useLink.set(!galleryImage.image && !!galleryImage.imageUrl);
    // Đang có link khi bấm / tắt / tab mới thì mở sẵn phần tùy chọn để thấy.
    this.showMore.set(!!galleryImage.linkUrl || galleryImage.active === false || !!galleryImage.openInNewTab);
  }

  protected loadPositions(): void {
    this.galleryService.withImages().subscribe(positions => {
      this.positions.set(positions);
      // Thêm mới từ trang danh sách đang lọc một vị trí (?position=) thì chọn sẵn vị trí đó.
      const preset = Number(this.activatedRoute.snapshot.queryParamMap.get('position'));
      if (this.isNew && !this.editForm.controls.gallery.value && preset) {
        const position = positions.find(item => item.id === preset);
        if (position) {
          this.editForm.controls.gallery.setValue({ id: position.id, name: position.name });
        }
      }
    });
  }

  /** Thứ tự kế tiếp trong vị trí: lớn nhất hiện có + 1 (vị trí trống thì 0). */
  private nextOrder(positionId: number | null | undefined): number {
    const images = this.positions().find(position => position.id === positionId)?.images ?? [];
    return images.reduce((max, image) => Math.max(max, (image.displayOrder ?? -1) + 1), 0);
  }

  private endAfterStart(control: AbstractControl): ValidationErrors | null {
    const end = control.value as string;
    const start = this.startDate.value;
    return end && start && dayjs(end).isBefore(dayjs(start)) ? { endBeforeStart: true } : null;
  }
}
