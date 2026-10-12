import { afterEach, describe, expect, it, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { FaIconLibrary } from '@fortawesome/angular-fontawesome';
import { provideTranslateService } from '@ngx-translate/core';

import { fontAwesomeIcons } from 'app/config/font-awesome-icons';

import { CopyButton, copyText } from './copy-button';
import { avatarHue, initials } from './initials';
import { SectionNav } from './section-nav';

describe('initials', () => {
  it('skips company-type words, keeps Vietnamese letters', () => {
    expect(initials('CTY TNHH TM ĐẦU TƯ HÓA DẦU VIỆT NAM')).toBe('ĐT');
    expect(initials('CÔNG TY CỔ PHẦN BAO BÌ SÀI GÒN')).toBe('BB');
    expect(initials('TRƯỜNG THPT YÊN LÃNG')).toBe('TT');
    expect(initials('CH MY MY')).toBe('MM');
    expect(initials('CÔNG TY TNHH')).toBe('CT');
    expect(initials('')).toBe('?');
  });

  it('avatar colour is stable for the same name', () => {
    expect(avatarHue('BAO BÌ')).toBe(avatarHue('BAO BÌ'));
    expect(avatarHue('BAO BÌ')).toBeGreaterThanOrEqual(0);
    expect(avatarHue('BAO BÌ')).toBeLessThan(360);
  });
});

describe('CopyButton', () => {
  afterEach(() => vi.useRealTimers());

  it('copies the text and shows a check mark for 1.5 s', async () => {
    vi.useFakeTimers();
    Object.defineProperty(globalThis, 'isSecureContext', { value: true, configurable: true });
    const writeText = vi.fn(() => Promise.resolve());
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
    TestBed.configureTestingModule({ providers: [provideTranslateService()] });
    TestBed.inject(FaIconLibrary).addIcons(...fontAwesomeIcons);
    const fixture = TestBed.createComponent(CopyButton);
    fixture.componentRef.setInput('text', '0100100079');
    fixture.detectChanges();

    await fixture.componentInstance.copy();
    expect(writeText).toHaveBeenCalledWith('0100100079');
    expect(fixture.componentInstance.copied()).toBe(true);
    vi.advanceTimersByTime(1500);
    expect(fixture.componentInstance.copied()).toBe(false);
  });

  it('falls back to a hidden textarea without the Clipboard API (http)', async () => {
    Object.defineProperty(globalThis, 'isSecureContext', { value: false, configurable: true });
    const exec = vi.fn(() => true);
    Object.defineProperty(document, 'execCommand', { value: exec, configurable: true });
    expect(await copyText('abc')).toBe(true);
    expect(exec).toHaveBeenCalledWith('copy');
    expect(document.querySelectorAll('textarea').length).toBe(0);
  });
});

describe('SectionNav', () => {
  it('clicking a section scrolls to it and marks it active', () => {
    TestBed.configureTestingModule({ providers: [provideTranslateService()] });
    const target = document.createElement('section');
    target.id = 'section-contact';
    target.scrollIntoView = vi.fn();
    document.body.appendChild(target);
    const fixture = TestBed.createComponent(SectionNav);
    fixture.componentRef.setInput('sections', [
      { id: 'section-overview', label: 'a' },
      { id: 'section-contact', label: 'b' },
    ]);
    fixture.detectChanges();
    expect(fixture.componentInstance.active()).toBe('section-overview');

    const event = new MouseEvent('click', { cancelable: true });
    fixture.componentInstance.go('section-contact', event);
    expect(event.defaultPrevented).toBe(true);
    expect(target.scrollIntoView).toHaveBeenCalled();
    expect(fixture.componentInstance.active()).toBe('section-contact');
    target.remove();
  });
});
