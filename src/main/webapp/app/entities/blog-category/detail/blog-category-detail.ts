import { Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap/tooltip';
import { TranslatePipe } from '@ngx-translate/core';

import { DataUtils } from 'app/core/util';
import { Alert, AlertError } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { IBlogCategory } from '../blog-category.model';

@Component({
  selector: 'jhi-blog-category-detail',
  templateUrl: './blog-category-detail.html',
  imports: [FontAwesomeModule, NgbTooltip, Alert, AlertError, TranslateDirective, TranslatePipe, RouterLink],
})
export class BlogCategoryDetail {
  readonly blogCategory = input<IBlogCategory | null>(null);

  protected dataUtils = inject(DataUtils);

  byteSize(base64String: string): string {
    return this.dataUtils.byteSize(base64String);
  }

  openFile(base64String: string, contentType: string | null | undefined): void {
    this.dataUtils.openFile(base64String, contentType);
  }

  previousState(): void {
    globalThis.history.back();
  }
}
