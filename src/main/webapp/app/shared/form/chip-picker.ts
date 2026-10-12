import { Component, DestroyRef, ElementRef, forwardRef, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable, Subject, catchError, debounceTime, of, switchMap, tap } from 'rxjs';

let nextId = 0;

const ENTITIES: Record<string, string> = { '&amp;': '&', '&quot;': '"', '&#039;': "'", '&#39;': "'", '&lt;': '<', '&gt;': '>' };

/** Tên lấy từ WordPress còn mã HTML (`&amp;`): giải mã để hiển thị, giá trị lưu giữ nguyên. */
export function decodeEntities(text: string | null | undefined): string {
  return (text ?? '').replace(/&(amp|quot|#0?39|lt|gt);/g, match => ENTITIES[match] ?? match);
}

/** Một lựa chọn trong ô tìm: hint là dòng phụ (ngành cha, tỉnh…), badge là nhãn nhỏ (ví dụ "Cũ"). */
export interface PickerOption {
  id: number;
  name: string;
  hint?: string | null;
  badge?: string | null;
}

/** Giá trị của control: mảng đối tượng có id, name (đúng dạng ICategory, ILocation gửi lên API). */
export interface PickerValue {
  id: number;
  name?: string | null;
}

/**
 * Chọn nhiều bản ghi bằng cách gõ tìm, thay cho {@code <select multiple>} (chỉ chứa 20 dòng đầu, không tìm được).
 * Đã chọn hiện thành thẻ có nút ×; gõ ≥ 1 ký tự → gọi {@link search} (chờ 250 ms), ↑ ↓ chọn, Enter thêm,
 * Backspace ở ô trống bỏ thẻ cuối, Esc đóng danh sách.
 */
@Component({
  selector: 'jhi-chip-picker',
  templateUrl: './chip-picker.html',
  styleUrl: './chip-picker.scss',
  imports: [FontAwesomeModule, TranslatePipe],
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => ChipPicker), multi: true }],
  host: { '(document:click)': 'onDocumentClick($event)' },
})
export class ChipPicker implements ControlValueAccessor {
  readonly search = input.required<(term: string) => Observable<PickerOption[]>>();
  readonly placeholder = input('');
  readonly inputId = input<string | null>(null);

  readonly selected = signal<PickerValue[]>([]);
  readonly options = signal<PickerOption[]>([]);
  readonly open = signal(false);
  readonly loading = signal(false);
  readonly active = signal(0);
  readonly term = signal('');
  readonly disabled = signal(false);
  /** Dòng phụ của thẻ đã chọn (biết được khi chọn từ danh sách). */
  readonly hints = new Map<number, string>();
  readonly listboxId = `chip-picker-${++nextId}`;

  private readonly terms$ = new Subject<string>();
  private readonly host = inject(ElementRef<HTMLElement>);

  constructor() {
    this.terms$
      .pipe(
        debounceTime(250),
        tap(() => this.loading.set(true)),
        switchMap(term =>
          term.trim() ? this.search()(term.trim()).pipe(catchError(() => of<PickerOption[]>([]))) : of<PickerOption[]>([]),
        ),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe(options => {
        this.loading.set(false);
        this.options.set(options);
        this.active.set(0);
        this.open.set(this.term().trim().length > 0);
      });
  }

  writeValue(value: PickerValue[] | null): void {
    this.selected.set(Array.isArray(value) ? [...value] : []);
  }

  registerOnChange(fn: (value: PickerValue[]) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(disabled: boolean): void {
    this.disabled.set(disabled);
  }

  label(text: string | null | undefined): string {
    return decodeEntities(text);
  }

  isSelected(id: number): boolean {
    return this.selected().some(item => item.id === id);
  }

  onInput(value: string): void {
    this.term.set(value);
    this.terms$.next(value);
    if (!value.trim()) {
      this.open.set(false);
    }
  }

  add(option: PickerOption): void {
    if (!this.isSelected(option.id)) {
      if (option.hint) {
        this.hints.set(option.id, option.hint);
      }
      this.emit([...this.selected(), { id: option.id, name: option.name }]);
    }
    this.term.set('');
    this.options.set([]);
    this.open.set(false);
    this.focusInput();
  }

  remove(item: PickerValue): void {
    this.emit(this.selected().filter(x => x.id !== item.id));
  }

  onKeydown(event: KeyboardEvent): void {
    const options = this.options();
    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        this.open.set(options.length > 0);
        this.active.update(i => Math.min(i + 1, options.length - 1));
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.active.update(i => Math.max(i - 1, 0));
        break;
      case 'Enter':
        // Không gửi form khi đang chọn
        event.preventDefault();
        if (this.open() && options[this.active()]) {
          this.add(options[this.active()]);
        }
        break;
      case 'Escape':
        if (this.open()) {
          event.stopPropagation();
          this.open.set(false);
        }
        break;
      case 'Backspace':
        if (!this.term() && this.selected().length) {
          this.remove(this.selected()[this.selected().length - 1]);
        }
        break;
    }
  }

  onBlur(): void {
    this.onTouched();
  }

  onDocumentClick(event: MouseEvent): void {
    if (!this.host.nativeElement.contains(event.target)) {
      this.open.set(false);
    }
  }

  focusInput(): void {
    this.host.nativeElement.querySelector('input')?.focus();
  }

  private onChange: (value: PickerValue[]) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  private emit(value: PickerValue[]): void {
    this.selected.set(value);
    this.onChange(value);
    this.onTouched();
  }
}
