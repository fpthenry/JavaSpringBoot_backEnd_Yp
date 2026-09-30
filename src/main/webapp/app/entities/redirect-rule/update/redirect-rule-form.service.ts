import { Service } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { IRedirectRule, NewRedirectRule } from '../redirect-rule.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IRedirectRule for edit and NewRedirectRuleFormGroupInput for create.
 */
type RedirectRuleFormGroupInput = IRedirectRule | PartialWithRequiredKeyOf<NewRedirectRule>;

type RedirectRuleFormDefaults = Pick<NewRedirectRule, 'id'>;

type RedirectRuleFormGroupContent = {
  id: FormControl<IRedirectRule['id'] | NewRedirectRule['id']>;
  sourceId: FormControl<IRedirectRule['sourceId']>;
  sourceSlug: FormControl<IRedirectRule['sourceSlug']>;
  destinationId: FormControl<IRedirectRule['destinationId']>;
  destinationSlug: FormControl<IRedirectRule['destinationSlug']>;
  objectType: FormControl<IRedirectRule['objectType']>;
};

export type RedirectRuleFormGroup = FormGroup<RedirectRuleFormGroupContent>;

@Service()
export class RedirectRuleFormService {
  createRedirectRuleFormGroup(redirectRule?: RedirectRuleFormGroupInput): RedirectRuleFormGroup {
    const redirectRuleRawValue = {
      ...this.getFormDefaults(),
      ...(redirectRule ?? { id: null }),
    };

    return new FormGroup<RedirectRuleFormGroupContent>({
      id: new FormControl(
        { value: redirectRuleRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      sourceId: new FormControl(redirectRuleRawValue.sourceId),
      sourceSlug: new FormControl(redirectRuleRawValue.sourceSlug, {
        validators: [Validators.required, Validators.maxLength(500)],
      }),
      destinationId: new FormControl(redirectRuleRawValue.destinationId),
      destinationSlug: new FormControl(redirectRuleRawValue.destinationSlug, {
        validators: [Validators.required, Validators.maxLength(500)],
      }),
      objectType: new FormControl(redirectRuleRawValue.objectType, {
        validators: [Validators.required, Validators.maxLength(50)],
      }),
    });
  }

  getRedirectRule(form: RedirectRuleFormGroup): IRedirectRule | NewRedirectRule {
    return form.getRawValue();
  }

  resetForm(form: RedirectRuleFormGroup, redirectRule: RedirectRuleFormGroupInput): void {
    const redirectRuleRawValue = { ...this.getFormDefaults(), ...redirectRule };
    form.reset({
      ...redirectRuleRawValue,
      id: { value: redirectRuleRawValue.id, disabled: true },
    });
  }

  private getFormDefaults(): RedirectRuleFormDefaults {
    return {
      id: null,
    };
  }
}
