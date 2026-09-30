import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

import { Alert, AlertError } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { IRedirectRule } from '../redirect-rule.model';

@Component({
  selector: 'jhi-redirect-rule-detail',
  templateUrl: './redirect-rule-detail.html',
  imports: [FontAwesomeModule, Alert, AlertError, TranslateDirective, RouterLink],
})
export class RedirectRuleDetail {
  readonly redirectRule = input<IRedirectRule | null>(null);

  previousState(): void {
    globalThis.history.back();
  }
}
