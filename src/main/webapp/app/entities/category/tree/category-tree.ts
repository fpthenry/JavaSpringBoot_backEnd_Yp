import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

import { AlertError } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { TreeView, createTreeSource } from 'app/shared/tree';
import { CategoryService } from '../service/category.service';

/** Cây ngành nghề (tối đa 4 cấp): nhóm ngành Yellow Pages và hệ thống ngành VSIC. */
@Component({
  selector: 'jhi-category-tree',
  templateUrl: './category-tree.html',
  imports: [RouterLink, FontAwesomeModule, AlertError, TranslateDirective, TreeView],
})
export class CategoryTree {
  readonly source = createTreeSource(inject(CategoryService));
}
