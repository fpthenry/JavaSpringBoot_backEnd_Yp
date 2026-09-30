import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

import { Alert, AlertError } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { IArticleTag } from '../article-tag.model';

@Component({
  selector: 'jhi-article-tag-detail',
  templateUrl: './article-tag-detail.html',
  imports: [FontAwesomeModule, Alert, AlertError, TranslateDirective, RouterLink],
})
export class ArticleTagDetail {
  readonly articleTag = input<IArticleTag | null>(null);

  previousState(): void {
    globalThis.history.back();
  }
}
