import { Service } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import dayjs from 'dayjs/esm';

import { DATE_TIME_FORMAT } from 'app/config';
import { IGalleryImage, NewGalleryImage } from '../gallery-image.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IGalleryImage for edit and NewGalleryImageFormGroupInput for create.
 */
type GalleryImageFormGroupInput = IGalleryImage | PartialWithRequiredKeyOf<NewGalleryImage>;

/**
 * Type that converts some properties for forms.
 */
type FormValueOf<T extends IGalleryImage | NewGalleryImage> = Omit<T, 'startAt' | 'endAt'> & {
  startAt?: string | null;
  endAt?: string | null;
};

type GalleryImageFormRawValue = FormValueOf<IGalleryImage>;

type NewGalleryImageFormRawValue = FormValueOf<NewGalleryImage>;

type GalleryImageFormDefaults = Pick<NewGalleryImage, 'id' | 'active' | 'startAt' | 'endAt' | 'openInNewTab'>;

type GalleryImageFormGroupContent = {
  id: FormControl<GalleryImageFormRawValue['id'] | NewGalleryImage['id']>;
  title: FormControl<GalleryImageFormRawValue['title']>;
  image: FormControl<GalleryImageFormRawValue['image']>;
  imageContentType: FormControl<GalleryImageFormRawValue['imageContentType']>;
  imageUrl: FormControl<GalleryImageFormRawValue['imageUrl']>;
  linkUrl: FormControl<GalleryImageFormRawValue['linkUrl']>;
  altText: FormControl<GalleryImageFormRawValue['altText']>;
  displayOrder: FormControl<GalleryImageFormRawValue['displayOrder']>;
  active: FormControl<GalleryImageFormRawValue['active']>;
  startAt: FormControl<GalleryImageFormRawValue['startAt']>;
  endAt: FormControl<GalleryImageFormRawValue['endAt']>;
  openInNewTab: FormControl<GalleryImageFormRawValue['openInNewTab']>;
  gallery: FormControl<GalleryImageFormRawValue['gallery']>;
};

export type GalleryImageFormGroup = FormGroup<GalleryImageFormGroupContent>;

@Service()
export class GalleryImageFormService {
  createGalleryImageFormGroup(galleryImage?: GalleryImageFormGroupInput): GalleryImageFormGroup {
    const galleryImageRawValue = this.convertGalleryImageToGalleryImageRawValue({
      ...this.getFormDefaults(),
      ...(galleryImage ?? { id: null }),
    });

    return new FormGroup<GalleryImageFormGroupContent>({
      id: new FormControl(
        { value: galleryImageRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      title: new FormControl(galleryImageRawValue.title, {
        validators: [Validators.maxLength(255)],
      }),
      image: new FormControl(galleryImageRawValue.image),
      imageContentType: new FormControl(galleryImageRawValue.imageContentType),
      imageUrl: new FormControl(galleryImageRawValue.imageUrl, {
        validators: [Validators.maxLength(1000)],
      }),
      linkUrl: new FormControl(galleryImageRawValue.linkUrl, {
        validators: [Validators.maxLength(1000)],
      }),
      altText: new FormControl(galleryImageRawValue.altText, {
        validators: [Validators.maxLength(255)],
      }),
      displayOrder: new FormControl(galleryImageRawValue.displayOrder, {
        validators: [Validators.required, Validators.min(0)],
      }),
      active: new FormControl(galleryImageRawValue.active, {
        validators: [Validators.required],
      }),
      startAt: new FormControl(galleryImageRawValue.startAt),
      endAt: new FormControl(galleryImageRawValue.endAt),
      openInNewTab: new FormControl(galleryImageRawValue.openInNewTab),
      gallery: new FormControl(galleryImageRawValue.gallery, {
        validators: [Validators.required],
      }),
    });
  }

  getGalleryImage(form: GalleryImageFormGroup): IGalleryImage | NewGalleryImage {
    return this.convertGalleryImageRawValueToGalleryImage(form.getRawValue());
  }

  resetForm(form: GalleryImageFormGroup, galleryImage: GalleryImageFormGroupInput): void {
    const galleryImageRawValue = this.convertGalleryImageToGalleryImageRawValue({ ...this.getFormDefaults(), ...galleryImage });
    form.reset({
      ...galleryImageRawValue,
      id: { value: galleryImageRawValue.id, disabled: true },
    });
  }

  private getFormDefaults(): GalleryImageFormDefaults {
    // Sửa tay: JHipster mặc định startAt = endAt = giờ hiện tại, làm ảnh mới hết hạn ngay khi tạo.
    // Để trống = hiển thị ngay, không hết hạn.
    return {
      id: null,
      active: false,
      startAt: null,
      endAt: null,
      openInNewTab: false,
    };
  }

  private convertGalleryImageRawValueToGalleryImage(
    rawGalleryImage: GalleryImageFormRawValue | NewGalleryImageFormRawValue,
  ): IGalleryImage | NewGalleryImage {
    return {
      ...rawGalleryImage,
      // Sửa tay: ô trống phải lưu null; dayjs(undefined) trả giờ hiện tại
      startAt: rawGalleryImage.startAt ? dayjs(rawGalleryImage.startAt, DATE_TIME_FORMAT) : null,
      endAt: rawGalleryImage.endAt ? dayjs(rawGalleryImage.endAt, DATE_TIME_FORMAT) : null,
    };
  }

  private convertGalleryImageToGalleryImageRawValue(
    galleryImage: IGalleryImage | (Partial<NewGalleryImage> & GalleryImageFormDefaults),
  ): GalleryImageFormRawValue | PartialWithRequiredKeyOf<NewGalleryImageFormRawValue> {
    return {
      ...galleryImage,
      startAt: galleryImage.startAt ? galleryImage.startAt.format(DATE_TIME_FORMAT) : undefined,
      endAt: galleryImage.endAt ? galleryImage.endAt.format(DATE_TIME_FORMAT) : undefined,
    };
  }
}
