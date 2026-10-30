import { NgTemplateOutlet } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable, finalize, map, merge, switchMap } from 'rxjs';

import { AlertError } from 'app/shared/alert';
import { AutoResizeDirective, ChipPicker, EditPage, PickerOption, decodeEntities } from 'app/shared/form';
import { TranslateDirective } from 'app/shared/language';
import { categorySlug, levelOfCode, parentCodeOf } from '../category-code';
import { CategoryRef, loadCategoryPath } from '../category-path';
import { ICategory } from '../category.model';
import { CategoryIndexService } from '../service/category-index.service';
import { CategoryService } from '../service/category.service';

import { CategoryFormGroup, CategoryFormService } from './category-form.service';

/** Gợi ý về ngành cha theo mã VSIC: khóa i18n javaSpringBootBackEndApp.category.help.<key> và tham số. */
export interface ParentHint {
  key: 'parentByCode' | 'parentNotFound' | 'parentOfLevel1' | 'parentOfLevel2';
  params: { code?: string; parent?: string };
}

/**
 * Sửa tay: form ngành nghề VSIC 2025: mã ngành (bắt buộc, không trùng, tự viết hoa), cấp suy từ mã, slug = tên + mã
 * (thêm mới thì tự điền), gợi ý ngành cha theo mã (4632 → ngành 463) và nút chọn nhanh.
 * Chia nhóm (thông tin chung, vị trí trong cây), cột phải thống kê, thanh lưu dính đáy.
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
  /** Mã ngành đang nhập (viết hoa, bỏ khoảng trắng). */
  readonly code = signal('');
  readonly name = signal('');
  readonly slug = signal('');
  /** Cấp suy từ mã; mã chưa đúng dạng thì theo vị trí trong cây. */
  readonly level = computed(() => levelOfCode(this.code()) ?? (this.code() ? null : this.parentPath().length + 1));
  readonly findingParent = signal(false);
  /** Đã tìm mà không có ngành mang mã cha này. */
  readonly missingParentCode = signal<string | null>(null);

  /** Slug đang khác slug chuẩn (tên + mã) thì trả slug chuẩn để gợi ý. */
  readonly slugSuggestion = computed(() => {
    const standard = levelOfCode(this.code()) ? categorySlug(this.name(), this.code()) : '';
    return standard && this.slug() && this.slug() !== standard ? standard : null;
  });

  /** Ngành cha không khớp mã: 4632 phải nằm dưới 463; cấp 1 không có cha; cấp 2 nằm dưới ngành chữ cái. */
  readonly parentHint = computed<ParentHint | null>(() => {
    const level = levelOfCode(this.code());
    const parentCode = this.parentPath().at(-1)?.code ?? null;
    const hasParent = this.parentPath().length > 0;
    if (level === 1) {
      return hasParent ? { key: 'parentOfLevel1', params: {} } : null;
    }
    if (level === 2) {
      return hasParent && levelOfCode(parentCode) !== 1 ? { key: 'parentOfLevel2', params: { code: this.code() } } : null;
    }
    const expected = parentCodeOf(this.code());
    if (!expected || parentCode === expected) {
      return null;
    }
    return this.missingParentCode() === expected
      ? { key: 'parentNotFound', params: { code: this.code(), parent: expected } }
      : { key: 'parentByCode', params: { code: this.code(), parent: expected } };
  });

  protected categoryService = inject(CategoryService);
  protected categoryFormService = inject(CategoryFormService);
  protected categoryIndexService = inject(CategoryIndexService);
  protected readonly labelPrefix = 'javaSpringBootBackEndApp.category.';
  protected readonly listRoute = '/category';
  protected readonly decode = decodeEntities;

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: CategoryFormGroup = this.categoryFormService.createCategoryFormGroup();

  private readonly destroyRef = inject(DestroyRef);

  /** Gõ tìm ngành cha theo tên (không dấu) hoặc mã; nhãn là mã ngành, dòng phụ là ngành cha của nó. */
  searchParents = (term: string): Observable<PickerOption[]> =>
    this.categoryIndexService
      .search({ q: term, size: 20, sort: levelOfCode(term) ? 'code,asc' : undefined })
      .pipe(map(page => page.items.map(item => ({ id: item.id, name: item.name, hint: item.parentName, badge: item.code }))));

  ngOnInit(): void {
    const { code, name, slug } = this.editForm.controls;
    merge(code.valueChanges, name.valueChanges, slug.valueChanges)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.syncSignals());
    // Thêm mới: slug tự theo tên + mã cho tới khi người dùng tự sửa slug
    merge(code.valueChanges, name.valueChanges)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (!this.category?.id && !slug.dirty && levelOfCode(code.value) && name.value) {
          slug.setValue(categorySlug(name.value, code.value));
        }
      });

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

  /** Mã ngành viết hoa, bỏ khoảng trắng ngay khi gõ (g → G). */
  normalizeCode(): void {
    const control = this.editForm.controls.code;
    const normalized = (control.value ?? '').replace(/\s+/g, '').toUpperCase();
    if (normalized !== control.value) {
      control.setValue(normalized);
    }
  }

  /** Slug = tên + mã: "Bán buôn thực phẩm", 4632 → "ban-buon-thuc-pham-4632". */
  generateSlug(): void {
    const control = this.editForm.controls.slug;
    control.setValue(categorySlug(this.editForm.controls.name.value, this.editForm.controls.code.value));
    control.markAsDirty();
  }

  /** Chọn ngành cha theo mã suy ra (4632 → ngành có mã 463). */
  pickParentByCode(): void {
    const expected = parentCodeOf(this.code());
    if (!expected) {
      return;
    }
    this.findingParent.set(true);
    this.categoryService
      .query({ 'code.equals': expected, size: 1 })
      .pipe(finalize(() => this.findingParent.set(false)))
      .subscribe(response => {
        const parent = response.body?.at(0);
        if (parent) {
          const control = this.editForm.controls.parent;
          control.setValue({ id: parent.id, name: parent.name, code: parent.code });
          control.markAsDirty();
        } else {
          this.missingParentCode.set(expected);
        }
      });
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
        this.editForm.controls.parent.setValue({ id: parent.id, name: parent.name, code: parent.code });
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
    this.syncSignals();
  }

  protected syncSignals(): void {
    const { code, name, slug } = this.editForm.controls;
    this.code.set((code.value ?? '').trim());
    this.name.set(name.value ?? '');
    this.slug.set(slug.value ?? '');
  }
}
