import { Service } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

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

type GalleryFormDefaults = Pick<NewGallery, 'id' | 'active'>;

type GalleryFormGroupContent = {
  id: FormControl<IGallery['id'] | NewGallery['id']>;
  name: FormControl<IGallery['name']>;
  code: FormControl<IGallery['code']>;
  description: FormControl<IGallery['description']>;
  active: FormControl<IGallery['active']>;
  wpId: FormControl<IGallery['wpId']>;
};

export type GalleryFormGroup = FormGroup<GalleryFormGroupContent>;

@Service()
export class GalleryFormService {
  createGalleryFormGroup(gallery?: GalleryFormGroupInput): GalleryFormGroup {
    const galleryRawValue = {
      ...this.getFormDefaults(),
      ...(gallery ?? { id: null }),
    };

    return new FormGroup<GalleryFormGroupContent>({
      id: new FormControl(
        { value: galleryRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      name: new FormControl(galleryRawValue.name, {
        validators: [Validators.required, Validators.maxLength(255)],
      }),
      code: new FormControl(galleryRawValue.code, {
        validators: [
          Validators.required,
          Validators.maxLength(100),
          Validators.pattern('^[a-z0-9-]+$'), // NOSONAR
        ],
      }),
      description: new FormControl(galleryRawValue.description),
      active: new FormControl(galleryRawValue.active, {
        validators: [Validators.required],
      }),
      wpId: new FormControl(galleryRawValue.wpId),
    });
  }

  getGallery(form: GalleryFormGroup): IGallery | NewGallery {
    return form.getRawValue();
  }

  resetForm(form: GalleryFormGroup, gallery: GalleryFormGroupInput): void {
    const galleryRawValue = { ...this.getFormDefaults(), ...gallery };
    form.reset({
      ...galleryRawValue,
      id: { value: galleryRawValue.id, disabled: true },
    });
  }

  private getFormDefaults(): GalleryFormDefaults {
    return {
      id: null,
      active: false,
    };
  }
}
