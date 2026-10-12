import { NgTemplateOutlet } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable, finalize, map, switchMap } from 'rxjs';

import { AlertError } from 'app/shared/alert';
import { AutoResizeDirective, ChipPicker, EditPage, PickerOption, decodeEntities, slugify } from 'app/shared/form';
import { TranslateDirective } from 'app/shared/language';
import { CategoryRef, loadCategoryPath } from '../category-path';
import { ICategory } from '../category.model';
import { CategoryIndexService } from '../service/category-index.service';
import { CategoryService } from '../service/category.service';

import { CategoryFormGroup, CategoryFormService } from './category-form.service';

/**
 * Sửa tay: form ngành nghề chia nhóm (thông tin chung, vị trí trong cây), cột phải thống kê/kỹ thuật, thanh lưu dính đáy.
 * Ngành cha chọn bằng gõ tìm (thay select 20 dòng đầu), hiện đường dẫn cây, chặn chọn ngành con của chính nó (vòng lặp).
 * Bỏ ô chọn doanh nghiệp (chiều ngược của quan hệ, select 20 dòng đầu, không dùng được). ?parentId= khi "Thêm ngành con".
 */
@Component({
  selector: 'jhi-category-update',
  templateUrl: './category-update.html',
  imports: [
    TranslateDirective,
    TranslatePipe,
    FontAwesomeModule,
    AlertError,
    ReactiveFormsModule,
    RouterLink,
    NgTemplateOutlet,
    AutoResizeDirective,
    ChipPicker,
  ],
})
export class CategoryUpdate extends EditPage implements OnInit {
  category: ICategory | null = null;
  /** Đường dẫn từ ngành gốc tới ngành cha đang chọn. */
  readonly parentPath = signal<CategoryRef[]>([]);
  /** Ngành cha đang chọn là chính nó hoặc ngành con của nó. */
  readonly parentCycle = signal(false);
  readonly level = computed(() => this.parentPath().length + 1);

  protected categoryService = inject(CategoryService);
  protected categoryFormService = inject(CategoryFormService);
  protected categoryIndexService = inject(CategoryIndexService);
  protected readonly labelPrefix = 'javaSpringBootBackEndApp.category.';
  protected readonly listRoute = '/category';
  protected readonly technicalFields = new Set(['wpTermId', 'icon']);
  protected readonly decode = decodeEntities;

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: CategoryFormGroup = this.categoryFormService.createCategoryFormGroup();

  private readonly destroyRef = inject(DestroyRef);

  /** Gõ tìm ngành cha: không dấu, dòng phụ là ngành cha của nó. */
  searchParents = (term: string): Observable<PickerOption[]> =>
    this.categoryIndexService
      .search({ q: term, size: 20 })
      .pipe(map(page => page.items.map(item => ({ id: item.id, name: item.name, hint: item.parentName }))));

  ngOnInit(): void {
    this.editForm.controls.parent.valueChanges
      .pipe(
        switchMap(parent => loadCategoryPath(this.categoryService, parent)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(path => this.setParentPath(path));

    this.activatedRoute.data.subscribe(({ category }) => {
      this.category = category;
      if (category) {
        this.updateForm(category);
      } else {
        this.prefillParent();
      }
      this.applyFocusParam();
    });
  }

  /** Không gợi ý chính ngành đang sửa làm ngành cha. */
  excludedParentIds(): number[] {
    return this.category ? [this.category.id] : [];
  }

  /** Tạo slug từ tên: "Lập trình máy vi tính" → "lap-trinh-may-vi-tinh". */
  generateSlug(): void {
    const control = this.editForm.controls.slug;
    control.setValue(slugify(this.editForm.controls.name.value));
    control.markAsDirty();
  }

  save(): void {
    if (this.parentCycle()) {
      this.focusField('parent');
      return;
    }
    this.isSaving.set(true);
    const category = this.categoryFormService.getCategory(this.editForm);
    if (category.id === null) {
      this.subscribeToSaveResponse(this.categoryService.create(category));
    } else {
      this.subscribeToSaveResponse(this.categoryService.update(category));
    }
  }

  protected setParentPath(path: CategoryRef[]): void {
    this.parentPath.set(path);
    const selfId = this.category?.id;
    const cycle = selfId !== undefined && path.some(node => node.id === selfId);
    this.parentCycle.set(cycle);
    const control = this.editForm.controls.parent;
    if (cycle) {
      control.setErrors({ ...control.errors, cycle: true });
      control.markAsTouched();
    } else if (control.hasError('cycle')) {
      const { cycle: _removed, ...rest } = control.errors ?? {};
      control.setErrors(Object.keys(rest).length > 0 ? rest : null);
    }
  }

  /** "Thêm ngành con" từ trang xem: /category/new?parentId=4 → điền sẵn ngành cha. */
  protected prefillParent(): void {
    const parentId = Number(this.activatedRoute.snapshot.queryParamMap.get('parentId'));
    if (!parentId) {
      return;
    }
    this.categoryService.find(parentId).subscribe({
      next: parent => {
        this.editForm.controls.parent.setValue({ id: parent.id, name: parent.name });
      },
      error: () => undefined,
    });
  }

  protected subscribeToSaveResponse(result: Observable<ICategory | null>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.markSaved();
    this.previousState();
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }

  protected onSaveFinalize(): void {
    this.isSaving.set(false);
  }

  protected updateForm(category: ICategory): void {
    this.category = category;
    this.categoryFormService.resetForm(this.editForm, category);
  }
}
