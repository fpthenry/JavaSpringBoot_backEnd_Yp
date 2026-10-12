import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { FaIconLibrary } from '@fortawesome/angular-fontawesome';
import { provideTranslateService } from '@ngx-translate/core';
import { Observable, of } from 'rxjs';

import { fontAwesomeIcons } from 'app/config/font-awesome-icons';

import { AutoResizeDirective } from './auto-resize.directive';
import { ChipPicker, PickerOption, PickerValue, decodeEntities } from './chip-picker';

const OPTIONS: PickerOption[] = [
  { id: 4, name: 'BAO BÌ', hint: 'SẢN XUẤT' },
  { id: 5, name: 'BAO BÌ GIẤY', hint: 'BAO BÌ' },
];

@Component({
  template: `<jhi-chip-picker [formControl]="control" [search]="search" />
    <textarea jhiAutoResize [allowNewline]="false" rows="1" [formControl]="text"></textarea>`,
  imports: [ChipPicker, ReactiveFormsModule, AutoResizeDirective],
})
class Host {
  control = new FormControl<PickerValue[]>([{ id: 1, name: 'LỊCH' }]);
  text = new FormControl('');
  search = vi.fn((term: string): Observable<PickerOption[]> => of(OPTIONS.filter(o => o.name.toLowerCase().includes(term.toLowerCase()))));
}

describe('ChipPicker', () => {
  let fixture: ComponentFixture<Host>;
  let host: Host;
  let picker: ChipPicker;

  const keydown = (key: string): KeyboardEvent => {
    const event = new KeyboardEvent('keydown', { key, cancelable: true });
    picker.onKeydown(event);
    return event;
  };

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({ imports: [Host], providers: [provideTranslateService()] });
    TestBed.inject(FaIconLibrary).addIcons(...fontAwesomeIcons);
    fixture = TestBed.createComponent(Host);
    host = fixture.componentInstance;
    fixture.detectChanges();
    picker = fixture.debugElement.children[0].componentInstance as ChipPicker;
  });

  afterEach(() => vi.useRealTimers());

  it('shows the form value as chips', () => {
    expect(picker.selected()).toEqual([{ id: 1, name: 'LỊCH' }]);
    expect((fixture.nativeElement as HTMLElement).querySelector('.chip-text')?.textContent).toBe('LỊCH');
  });

  it('searches after 250 ms, Enter adds the active option and updates the form', () => {
    picker.onInput('bao');
    vi.advanceTimersByTime(200);
    expect(host.search).not.toHaveBeenCalled();
    vi.advanceTimersByTime(50);
    expect(host.search).toHaveBeenCalledWith('bao');
    expect(picker.open()).toBe(true);

    keydown('ArrowDown');
    const enter = keydown('Enter');
    expect(enter.defaultPrevented).toBe(true);
    expect(host.control.value).toEqual([
      { id: 1, name: 'LỊCH' },
      { id: 5, name: 'BAO BÌ GIẤY' },
    ]);
    expect(picker.hints.get(5)).toBe('BAO BÌ');
    expect(picker.term()).toBe('');
    expect(picker.open()).toBe(false);
    expect(host.control.dirty || host.control.touched).toBe(true);
  });

  it('does not add the same item twice', () => {
    picker.add({ id: 1, name: 'LỊCH' });
    expect(host.control.value).toEqual([{ id: 1, name: 'LỊCH' }]);
  });

  it('Backspace on an empty input removes the last chip; × removes one chip', () => {
    picker.add(OPTIONS[0]);
    keydown('Backspace');
    expect(host.control.value).toEqual([{ id: 1, name: 'LỊCH' }]);
    picker.remove({ id: 1 });
    expect(host.control.value).toEqual([]);
  });

  it('Escape closes the list; empty term does not call the API', () => {
    picker.onInput('bao');
    vi.advanceTimersByTime(250);
    keydown('Escape');
    expect(picker.open()).toBe(false);
    host.search.mockClear();
    picker.onInput('   ');
    vi.advanceTimersByTime(250);
    expect(host.search).not.toHaveBeenCalled();
  });

  it('follows the disabled state of the form control', () => {
    host.control.disable();
    expect(picker.disabled()).toBe(true);
  });
});

describe('decodeEntities', () => {
  it('decodes WordPress HTML entities for display', () => {
    expect(decodeEntities('TIỂU HỌC - THCS &amp;amp; THPT')).toBe('TIỂU HỌC - THCS &amp; THPT');
    expect(decodeEntities('A &amp; B &quot;C&quot; &#039;D&#039; &lt;E&gt;')).toBe(`A & B "C" 'D' <E>`);
    expect(decodeEntities(null)).toBe('');
  });
});

describe('AutoResizeDirective', () => {
  it('Enter does not insert a newline when allowNewline is false', () => {
    TestBed.configureTestingModule({ imports: [Host], providers: [provideTranslateService()] });
    TestBed.inject(FaIconLibrary).addIcons(...fontAwesomeIcons);
    const fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
    const textarea = (fixture.nativeElement as HTMLElement).querySelector('textarea')!;
    const event = new KeyboardEvent('keydown', { key: 'Enter', cancelable: true });
    textarea.dispatchEvent(event);
    expect(event.defaultPrevented).toBe(true);
    expect(textarea.classList).toContain('auto-resize');
  });
});
