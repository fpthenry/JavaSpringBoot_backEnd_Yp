import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Observable, finalize } from 'rxjs';

import { AlertError } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { IRedirectRule } from '../redirect-rule.model';
import { RedirectRuleService } from '../service/redirect-rule.service';

import { RedirectRuleFormGroup, RedirectRuleFormService } from './redirect-rule-form.service';

@Component({
  selector: 'jhi-redirect-rule-update',
  templateUrl: './redirect-rule-update.html',
  imports: [TranslateDirective, FontAwesomeModule, AlertError, ReactiveFormsModule],
})
export class RedirectRuleUpdate implements OnInit {
  readonly isSaving = signal(false);
  redirectRule: IRedirectRule | null = null;

  protected redirectRuleService = inject(RedirectRuleService);
  protected redirectRuleFormService = inject(RedirectRuleFormService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: RedirectRuleFormGroup = this.redirectRuleFormService.createRedirectRuleFormGroup();

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ redirectRule }) => {
      this.redirectRule = redirectRule;
      if (redirectRule) {
        this.updateForm(redirectRule);
      }
    });
  }

  previousState(): void {
    globalThis.history.back();
  }

  save(): void {
    this.isSaving.set(true);
    const redirectRule = this.redirectRuleFormService.getRedirectRule(this.editForm);
    if (redirectRule.id === null) {
      this.subscribeToSaveResponse(this.redirectRuleService.create(redirectRule));
    } else {
      this.subscribeToSaveResponse(this.redirectRuleService.update(redirectRule));
    }
  }

  protected subscribeToSaveResponse(result: Observable<IRedirectRule | null>): void {
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

  protected updateForm(redirectRule: IRedirectRule): void {
    this.redirectRule = redirectRule;
    this.redirectRuleFormService.resetForm(this.editForm, redirectRule);
  }
}
