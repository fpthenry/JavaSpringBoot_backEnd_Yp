import { Service } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { ITag, NewTag } from '../tag.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts ITag for edit and NewTagFormGroupInput for create.
 */
type TagFormGroupInput = ITag | PartialWithRequiredKeyOf<NewTag>;

type TagFormDefaults = Pick<NewTag, 'id' | 'blogPosts'>;

type TagFormGroupContent = {
  id: FormControl<ITag['id'] | NewTag['id']>;
  wpTermId: FormControl<ITag['wpTermId']>;
  name: FormControl<ITag['name']>;
  slug: FormControl<ITag['slug']>;
  blogPosts: FormControl<ITag['blogPosts']>;
};

export type TagFormGroup = FormGroup<TagFormGroupContent>;

@Service()
export class TagFormService {
  createTagFormGroup(tag?: TagFormGroupInput): TagFormGroup {
    const tagRawValue = {
      ...this.getFormDefaults(),
      ...(tag ?? { id: null }),
    };

    return new FormGroup<TagFormGroupContent>({
      id: new FormControl(
        { value: tagRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      wpTermId: new FormControl(tagRawValue.wpTermId),
      name: new FormControl(tagRawValue.name, {
        validators: [Validators.required, Validators.maxLength(255)],
      }),
      slug: new FormControl(tagRawValue.slug, {
        validators: [Validators.maxLength(255)],
      }),
      blogPosts: new FormControl(tagRawValue.blogPosts ?? []),
    });
  }

  getTag(form: TagFormGroup): ITag | NewTag {
    return form.getRawValue();
  }

  resetForm(form: TagFormGroup, tag: TagFormGroupInput): void {
    const tagRawValue = { ...this.getFormDefaults(), ...tag };
    form.reset({
      ...tagRawValue,
      id: { value: tagRawValue.id, disabled: true },
    });
  }

  private getFormDefaults(): TagFormDefaults {
    return {
      id: null,
      blogPosts: [],
    };
  }
}
