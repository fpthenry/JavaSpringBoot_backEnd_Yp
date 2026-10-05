import { Component, ElementRef, ViewEncapsulation, effect, forwardRef, signal, viewChild } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { TranslatePipe } from '@ngx-translate/core';

import { sanitizeHtml } from './html-sanitizer';

interface ToolbarButton {
  command: string;
  value?: string;
  label: string;
  icon?: string;
  text?: string;
}

/**
 * Trình soạn thảo HTML dạng WYSIWYG cho nội dung bài viết, dùng contenteditable của trình duyệt (không thư viện ngoài).
 * Dán nội dung từ website/Word giữ chữ, ảnh, bảng, link; tự làm sạch (xem {@link sanitizeHtml}).
 * Dùng với reactive form: {@code <jhi-rich-text-editor formControlName="content" />}.
 */
@Component({
  selector: 'jhi-rich-text-editor',
  templateUrl: './rich-text-editor.html',
  styleUrl: './rich-text-editor.scss',
  // Style phải áp dụng cho nội dung chèn bằng innerHTML (ảnh, bảng trong bài); selector đã có tiền tố .rte
  encapsulation: ViewEncapsulation.None,
  imports: [FormsModule, FontAwesomeModule, TranslatePipe],
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => RichTextEditor), multi: true }],
})
export class RichTextEditor implements ControlValueAccessor {
  readonly htmlMode = signal(false);
  readonly disabled = signal(false);
  readonly html = signal('');

  readonly blockFormats = [
    { value: 'p', label: 'entity.richText.paragraph' },
    { value: 'h2', label: 'entity.richText.heading2' },
    { value: 'h3', label: 'entity.richText.heading3' },
    { value: 'h4', label: 'entity.richText.heading4' },
    { value: 'blockquote', label: 'entity.richText.quote' },
  ];

  readonly buttons: ToolbarButton[][] = [
    [
      { command: 'bold', label: 'entity.richText.bold', text: 'B' },
      { command: 'italic', label: 'entity.richText.italic', text: 'I' },
      { command: 'underline', label: 'entity.richText.underline', text: 'U' },
      { command: 'strikeThrough', label: 'entity.richText.strike', text: 'S' },
    ],
    [
      { command: 'insertUnorderedList', label: 'entity.richText.bulletList', icon: 'list' },
      { command: 'insertOrderedList', label: 'entity.richText.numberList', text: '1.' },
    ],
    [
      { command: 'justifyLeft', label: 'entity.richText.alignLeft', text: '⯇' },
      { command: 'justifyCenter', label: 'entity.richText.alignCenter', text: '≡' },
      { command: 'justifyRight', label: 'entity.richText.alignRight', text: '⯈' },
    ],
    [
      { command: 'insertHorizontalRule', label: 'entity.richText.hr', text: '―' },
      { command: 'removeFormat', label: 'entity.richText.removeFormat', icon: 'times' },
      { command: 'undo', label: 'entity.richText.undo', text: '↶' },
      { command: 'redo', label: 'entity.richText.redo', text: '↷' },
    ],
  ];

  protected readonly editor = viewChild<ElementRef<HTMLDivElement>>('editor');

  constructor() {
    // Đưa HTML vào vùng soạn thảo khi giá trị đổi từ bên ngoài hoặc khi rời chế độ HTML
    effect(() => {
      const element = this.editor()?.nativeElement;
      const html = this.html();
      if (element && !this.htmlMode() && element.innerHTML !== html) {
        element.innerHTML = html;
      }
    });
  }

  writeValue(value: string | null): void {
    // Nội dung có sẵn (vd đồng bộ từ WordPress): chỉ bỏ phần nguy hiểm, giữ class/style để không vỡ trình bày
    this.html.set(value ? sanitizeHtml(value) : '');
  }

  registerOnChange(fn: (value: string | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
  }

  exec(command: string, value?: string): void {
    this.editor()?.nativeElement.focus();
    runCommand(command, value);
    this.emitFromEditor();
  }

  formatBlock(tag: string): void {
    this.exec('formatBlock', `<${tag}>`);
  }

  insertLink(): void {
    const url = prompt('URL (https://...)');
    if (url && /^(https?:|mailto:|tel:|\/)/i.test(url.trim())) {
      this.exec('createLink', url.trim());
    }
  }

  insertImage(): void {
    const url = prompt('URL ảnh (https://...)');
    if (url && /^https?:\/\//i.test(url.trim())) {
      this.exec('insertImage', url.trim());
    }
  }

  toggleHtmlMode(): void {
    if (this.htmlMode()) {
      // Rời chế độ HTML: làm sạch phần đã sửa tay
      this.html.set(sanitizeHtml(this.html()));
      this.onChange(this.html() || null);
    }
    this.htmlMode.update(mode => !mode);
  }

  onHtmlSourceChange(value: string): void {
    this.html.set(value);
    this.onChange(value || null);
  }

  onInput(): void {
    this.emitFromEditor();
  }

  onBlur(): void {
    this.onTouched();
  }

  /** Dán: ưu tiên HTML (giữ định dạng, ảnh, bảng) đã làm sạch; không có thì dán chữ thường. */
  onPaste(event: ClipboardEvent): void {
    const data = event.clipboardData;
    if (!data) {
      return;
    }
    const pastedHtml = data.getData('text/html');
    event.preventDefault();
    if (pastedHtml) {
      runCommand('insertHTML', sanitizeHtml(pastedHtml, { strict: true }));
    } else {
      const text = data.getData('text/plain');
      const escaped = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
      const paragraphs = escaped
        .split(/\r?\n\s*\r?\n/)
        .map(p => `<p>${p.replace(/\r?\n/g, '<br>')}</p>`)
        .join('');
      runCommand('insertHTML', paragraphs);
    }
    this.emitFromEditor();
  }

  private onChange: (value: string | null) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  private emitFromEditor(): void {
    const element = this.editor()?.nativeElement;
    if (!element) {
      return;
    }
    const html = element.innerHTML === '<br>' ? '' : element.innerHTML;
    this.html.set(html);
    this.onChange(html || null);
  }
}

/**
 * document.execCommand bị đánh dấu deprecated nhưng mọi trình duyệt vẫn hỗ trợ đầy đủ và chưa có API thay thế
 * (Input Events / Selection API cần tự viết lại toàn bộ thao tác định dạng). Dự án không thêm thư viện soạn thảo,
 * nên gom mọi lệnh định dạng về đây.
 */
function runCommand(command: string, value?: string): void {
  // eslint-disable-next-line @typescript-eslint/no-deprecated
  document.execCommand(command, false, value);
}
