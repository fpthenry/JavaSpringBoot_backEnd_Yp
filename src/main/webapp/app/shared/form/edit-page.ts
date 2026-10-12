import { Directive, inject, signal } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { HasUnsavedChanges } from './unsaved-changes.guard';

/**
 * Hành vi chung của trang thêm/sửa (dùng cho mọi entity, kế thừa lớp này):
 * <ul>
 *   <li>Ctrl+S (⌘+S) lưu; form sai thì nhảy tới field sai đầu tiên.</li>
 *   <li>{@link invalidFields} cho thanh lưu liệt kê "Kiểm tra lại: …", {@link focusField} cuộn tới field, đặt con trỏ, nháy viền
 *   (field trong khối kỹ thuật thu gọn thì tự mở khối).</li>
 *   <li>?focus=&lt;field&gt; trên URL (nút "Bổ sung" ở trang xem): mở form tại đúng field ({@link applyFocusParam}).</li>
 *   <li>Hỏi lại khi rời trang còn thay đổi chưa lưu (route có {@code canDeactivate: [unsavedChangesGuard]}; đóng tab thì
 *   {@code beforeunload}).</li>
 *   <li>Hủy: về trang trước trong ứng dụng, mở thẳng trang sửa thì về danh sách {@link listRoute}.</li>
 * </ul>
 * Lớp con: khai báo {@link editForm}, {@link labelPrefix}, {@link listRoute}, {@link save}; gọi {@link markSaved} khi lưu xong.
 * Field có id {@code field_<tên>} trong template.
 */
@Directive({
  host: {
    '(window:beforeunload)': 'onBeforeUnload($event)',
    '(document:keydown.control.s)': 'onSaveShortcut($event)',
    '(document:keydown.meta.s)': 'onSaveShortcut($event)',
  },
})
export abstract class EditPage implements HasUnsavedChanges {
  readonly isSaving = signal(false);
  /** Khối "Thông tin kỹ thuật" đang mở. */
  readonly technicalOpen = signal(false);

  abstract editForm: FormGroup;

  protected readonly activatedRoute = inject(ActivatedRoute);
  protected readonly router = inject(Router);
  /** Tiền tố khóa dịch nhãn field, ví dụ 'javaSpringBootBackEndApp.listing.'. */
  protected abstract readonly labelPrefix: string;
  /** Trang danh sách để về khi bấm Hủy mà không có trang trước trong ứng dụng. */
  protected abstract readonly listRoute: string;
  /** Field nằm trong khối kỹ thuật thu gọn. */
  protected readonly technicalFields: ReadonlySet<string> = new Set<string>();
  /** Nhãn riêng cho field (mặc định labelPrefix + tên field). */
  protected readonly fieldLabels: Readonly<Record<string, string>> = {};

  private saved = false;

  abstract save(): void;

  /** Các field đang sai, để thanh lưu liệt kê và bấm vào thì nhảy tới. */
  invalidFields(): { key: string; label: string }[] {
    return Object.entries(this.editForm.controls)
      .filter(([, control]) => control.invalid)
      .map(([key]) => ({ key, label: this.fieldLabels[key] ?? `${this.labelPrefix}${key}` }));
  }

  focusField(key: string): void {
    if (this.technicalFields.has(key)) {
      this.technicalOpen.set(true);
    }
    this.editForm.get(key)?.markAsTouched();
    setTimeout(() => {
      const element = document.getElementById(`field_${key}`);
      element?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      element?.focus({ preventScroll: true });
      // Nháy viền để người dùng thấy ngay ô cần nhập (ô chọn nhiều, trình soạn thảo: nháy cả khung)
      const box = element?.closest('.chip-picker, .rte-compact') ?? element;
      box?.classList.remove('field-flash');
      void (box as HTMLElement | null)?.offsetWidth;
      box?.classList.add('field-flash');
    });
  }

  hasUnsavedChanges(): boolean {
    return this.editForm.dirty && !this.saved;
  }

  onBeforeUnload(event: BeforeUnloadEvent): void {
    if (this.hasUnsavedChanges()) {
      event.preventDefault();
    }
  }

  onSaveShortcut(event: Event): void {
    event.preventDefault();
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      const first = this.invalidFields().at(0);
      if (first) {
        this.focusField(first.key);
      }
    } else if (!this.isSaving()) {
      this.save();
    }
  }

  /** Về trang trước trong ứng dụng; mở thẳng trang sửa (link, tab mới) thì về danh sách thay vì rời ứng dụng. */
  previousState(): void {
    const navigationId = (globalThis.history.state as { navigationId?: number } | null)?.navigationId ?? 1;
    if (navigationId > 1) {
      globalThis.history.back();
    } else {
      void this.router.navigate([this.listRoute]);
    }
  }

  /** Gọi khi lưu xong: không hỏi "rời trang?" nữa. */
  protected markSaved(): void {
    this.saved = true;
  }

  /** Từ trang xem, "Bổ sung" một mục còn thiếu: mở form và nhảy tới đúng field (?focus=email). Tên lạ bị bỏ qua. */
  protected applyFocusParam(): void {
    const focus = this.activatedRoute.snapshot.queryParamMap.get('focus');
    if (focus && Object.prototype.hasOwnProperty.call(this.editForm.controls, focus)) {
      this.focusField(focus);
    }
  }
}
