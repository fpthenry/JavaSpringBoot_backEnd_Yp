import { AfterViewInit, DestroyRef, Directive, ElementRef, inject, input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgControl } from '@angular/forms';

/**
 * Ô nhập nhiều dòng tự cao theo nội dung: dữ liệu dài tự xuống dòng, không phải kéo góc hay cuộn trong ô nhỏ.
 * Dùng thay cho {@code <input>} với field chữ có thể dài (tên, địa chỉ, website…):
 * {@code <textarea jhiAutoResize rows="1" formControlName="name"></textarea>}.
 * Cao tối đa {@link maxHeight}, quá thì cuộn trong ô.
 */
@Directive({
  selector: 'textarea[jhiAutoResize]',
  host: {
    class: 'auto-resize',
    '(input)': 'resize()',
    '(keydown.enter)': 'onEnter($event)',
  },
})
export class AutoResizeDirective implements AfterViewInit {
  /** Chiều cao tối đa (px); quá thì hiện thanh cuộn. */
  readonly maxHeight = input(480);
  /** false: Enter không xuống dòng (field một dòng như tên, chữ chỉ tự xuống dòng khi dài). */
  readonly allowNewline = input(true);

  private readonly element: HTMLTextAreaElement = inject(ElementRef<HTMLTextAreaElement>).nativeElement;
  private readonly control = inject(NgControl, { optional: true, self: true });
  private readonly destroyRef = inject(DestroyRef);

  ngAfterViewInit(): void {
    // Giá trị do form gán (patchValue/reset) không phát sự kiện input: nghe valueChanges
    this.control?.valueChanges?.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => queueMicrotask(() => this.resize()));
    // Ô nằm trong tab/khối ẩn có scrollHeight = 0: đo lại khi kích thước thay đổi
    if (typeof ResizeObserver !== 'undefined') {
      let lastWidth = 0;
      const observer = new ResizeObserver(entries => {
        const width = entries[0]?.contentRect.width ?? 0;
        if (width !== lastWidth) {
          lastWidth = width;
          this.resize();
        }
      });
      observer.observe(this.element);
      this.destroyRef.onDestroy(() => observer.disconnect());
    }
    this.resize();
  }

  resize(): void {
    const el = this.element;
    el.style.height = 'auto';
    const border = el.offsetHeight - el.clientHeight;
    const height = el.scrollHeight + border;
    const max = this.maxHeight();
    el.style.height = `${Math.min(height, max)}px`;
    el.style.overflowY = height > max ? 'auto' : 'hidden';
  }

  onEnter(event: Event): void {
    if (!this.allowNewline()) {
      // Field một dòng: Enter không chèn xuống dòng (giá trị lưu không có \n)
      event.preventDefault();
    }
  }
}
