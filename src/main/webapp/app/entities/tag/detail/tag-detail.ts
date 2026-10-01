import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap/tooltip';
import { TranslatePipe } from '@ngx-translate/core';

import { Alert, AlertError } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { ITag } from '../tag.model';

@Component({
  selector: 'jhi-tag-detail',
  templateUrl: './tag-detail.html',
  imports: [FontAwesomeModule, NgbTooltip, Alert, AlertError, TranslateDirective, TranslatePipe, RouterLink],
})
export class TagDetail {
  readonly tag = input<ITag | null>(null);

  previousState(): void {
    globalThis.history.back();
  }
}
