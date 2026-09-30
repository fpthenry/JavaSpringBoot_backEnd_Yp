import { Service } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import dayjs from 'dayjs/esm';

import { DATE_TIME_FORMAT } from 'app/config';
import { IGallery, NewGallery } from '../gallery.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IGallery for edit and NewGalleryFormGroupInput for create.
 */
type GalleryFormGroupInput = IGallery | PartialWithRequiredKeyOf<NewGallery>;

/**
 * Type that converts some properties for forms.
 */
type FormValueOf<T extends IGallery | NewGallery> = Omit<T, 'createdAt' | 'updatedAt'> & {
  createdAt?: string | null;
  updatedAt?: string | null;
};

type GalleryFormRawValue = FormValueOf<IGallery>;

type NewGalleryFormRawValue = FormValueOf<NewGallery>;

type GalleryFormDefaults = Pick<NewGallery, 'id' | 'createdAt' | 'updatedAt'>;

type GalleryFormGroupContent = {
  id: FormControl<GalleryFormRawValue['id'] | NewGallery['id']>;
  title: FormControl<GalleryFormRawValue['title']>;
  slug: FormControl<GalleryFormRawValue['slug']>;
  content: FormControl<GalleryFormRawValue['content']>;
  status: FormControl<GalleryFormRawValue['status']>;
  createdAt: FormControl<GalleryFormRawValue['createdAt']>;
  updatedAt: FormControl<GalleryFormRawValue['updatedAt']>;
  author: FormControl<GalleryFormRawValue['author']>;
  listing: FormControl<GalleryFormRawValue['listing']>;
};

export type GalleryFormGroup = FormGroup<GalleryFormGroupContent>;

@Service()
export class GalleryFormService {
  createGalleryFormGroup(gallery?: GalleryFormGroupInput): GalleryFormGroup {
    const galleryRawValue = this.convertGalleryToGalleryRawValue({
      ...this.getFormDefaults(),
      ...(gallery ?? { id: null }),
    });

    return new FormGroup<GalleryFormGroupContent>({
      id: new FormControl(
        { value: galleryRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      title: new FormControl(galleryRawValue.title, {
        validators: [Validators.required, Validators.maxLength(500)],
      }),
      slug: new FormControl(galleryRawValue.slug, {
        validators: [Validators.maxLength(500)],
      }),
      content: new FormControl(galleryRawValue.content),
      status: new FormControl(galleryRawValue.status, {
        validators: [Validators.maxLength(20)],
      }),
      createdAt: new FormControl(galleryRawValue.createdAt),
      updatedAt: new FormControl(galleryRawValue.updatedAt),
      author: new FormControl(galleryRawValue.author),
      listing: new FormControl(galleryRawValue.listing),
    });
  }

  getGallery(form: GalleryFormGroup): IGallery | NewGallery {
    return this.convertGalleryRawValueToGallery(form.getRawValue());
  }

  resetForm(form: GalleryFormGroup, gallery: GalleryFormGroupInput): void {
    const galleryRawValue = this.convertGalleryToGalleryRawValue({ ...this.getFormDefaults(), ...gallery });
    form.reset({
      ...galleryRawValue,
      id: { value: galleryRawValue.id, disabled: true },
    });
  }

  private getFormDefaults(): GalleryFormDefaults {
    const currentTime = dayjs();

    return {
      id: null,
      createdAt: currentTime,
      updatedAt: currentTime,
    };
  }

  private convertGalleryRawValueToGallery(rawGallery: GalleryFormRawValue | NewGalleryFormRawValue): IGallery | NewGallery {
    return {
      ...rawGallery,
      createdAt: dayjs(rawGallery.createdAt, DATE_TIME_FORMAT),
      updatedAt: dayjs(rawGallery.updatedAt, DATE_TIME_FORMAT),
    };
  }

  private convertGalleryToGalleryRawValue(
    gallery: IGallery | (Partial<NewGallery> & GalleryFormDefaults),
  ): GalleryFormRawValue | PartialWithRequiredKeyOf<NewGalleryFormRawValue> {
    return {
      ...gallery,
      createdAt: gallery.createdAt ? gallery.createdAt.format(DATE_TIME_FORMAT) : undefined,
      updatedAt: gallery.updatedAt ? gallery.updatedAt.format(DATE_TIME_FORMAT) : undefined,
    };
  }
}
