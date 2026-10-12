import { Component, input, signal } from '@angular/core';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { TranslatePipe } from '@ngx-translate/core';

/** Chép bằng ô nhập ẩn (trình duyệt không có Clipboard API, trang chạy http). */
export function copyWithTextarea(text: string): boolean {
  const area = document.createElement('textarea');
  area.value = text;
  area.setAttribute('readonly', '');
  area.style.position = 'fixed';
  area.style.opacity = '0';
  document.body.appendChild(area);
  area.select();
  // eslint-disable-next-line @typescript-eslint/no-deprecated
  const ok = document.execCommand('copy');
  area.remove();
  return ok;
}

/** Chép chữ vào bộ nhớ tạm; trả true khi xong. Clipboard API chỉ có trên https/localhost. */
export async function copyText(text: string): Promise<boolean> {
  if (globalThis.isSecureContext && 'clipboard' in navigator) {
    try {
      await navigator.clipboard.writeText(text);
      return true;
    } catch {
      return copyWithTextarea(text);
    }
  }
  return copyWithTextarea(text);
}

/**
 * Nút chép nhỏ cạnh một giá trị (số điện thoại, email, mã số thuế…): bấm → ✓ trong 1,5 s.
 * {@code <jhi-copy-button [text]="listing.taxCode" />}; {@code [label]} hiện chữ bên cạnh icon.
 */
@Component({
  selector: 'jhi-copy-button',
  template: `<button
    type="button"
    class="btn btn-sm copy-btn"
    [class.btn-link]="!label()"
    [class.btn-outline-secondary]="!!label()"
    [class.copied]="copied()"
    [title]="(copied() ? 'global.detail.copied' : 'global.detail.copy') | translate"
    [attr.aria-label]="(copied() ? 'global.detail.copied' : 'global.detail.copy') | translate"
    (click)="copy(); $event.stopPropagation()"
  >
    <fa-icon [icon]="copied() ? 'check' : 'copy'" />
    @if (label(); as text) {
      <span class="ms-1">{{ (copied() ? 'global.detail.copied' : text) | translate }}</span>
    }
  </button>`,
  imports: [FontAwesomeModule, TranslatePipe],
})
export class CopyButton {
  readonly text = input<string | null | undefined>(null);
  /** Khóa dịch của chữ hiện cạnh icon; bỏ trống = chỉ icon. */
  readonly label = input<string | null>(null);
  readonly copied = signal(false);

  private timer?: ReturnType<typeof setTimeout>;

  async copy(): Promise<void> {
    const text = this.text();
    if (text && (await copyText(text))) {
      this.copied.set(true);
      clearTimeout(this.timer);
      this.timer = setTimeout(() => this.copied.set(false), 1500);
    }
  }
}
