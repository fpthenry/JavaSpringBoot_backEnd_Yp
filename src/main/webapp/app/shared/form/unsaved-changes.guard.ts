import { inject } from '@angular/core';
import { CanDeactivateFn } from '@angular/router';

import { TranslateService } from '@ngx-translate/core';

/** Trang sửa có dữ liệu chưa lưu (thường là {@code editForm.dirty} và chưa bấm Lưu). */
export interface HasUnsavedChanges {
  hasUnsavedChanges(): boolean;
}

/**
 * Hỏi lại trước khi rời trang sửa khi còn thay đổi chưa lưu (bấm menu, Hủy, nút Back của trình duyệt).
 * Gắn vào route: {@code canDeactivate: [unsavedChangesGuard]}. Đóng tab/tải lại trang do component tự chặn bằng
 * {@code beforeunload}.
 */
export const unsavedChangesGuard: CanDeactivateFn<HasUnsavedChanges> = component => {
  if (!component.hasUnsavedChanges()) {
    return true;
  }
  return globalThis.confirm(inject(TranslateService).instant('global.form.unsavedConfirm'));
};
