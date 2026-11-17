import { beforeEach, describe, expect, it, vi } from 'vitest';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FaIconLibrary } from '@fortawesome/angular-fontawesome';
import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

import { fontAwesomeIcons } from 'app/config/font-awesome-icons';
import { AccountService } from 'app/core/auth';

import { SiteEffect } from './site-effect';
import { SiteEffectService } from './site-effect.service';

describe('SiteEffect (bật/tắt hiệu ứng Tết)', () => {
  let comp: SiteEffect;
  let fixture: ComponentFixture<SiteEffect>;
  let service: SiteEffectService;

  const login = (authorities: string[]): void =>
    TestBed.inject(AccountService).authenticate({
      activated: true,
      authorities,
      email: 'a@yp.vn',
      firstName: 'A',
      langKey: 'vi',
      lastName: null,
      login: 'a',
      imageUrl: null,
    });

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideTranslateService(), provideHttpClient(), provideHttpClientTesting()] });
    TestBed.inject(FaIconLibrary).addIcons(...fontAwesomeIcons);
    service = TestBed.inject(SiteEffectService);
    vi.spyOn(service, 'get').mockReturnValue(of({ tetEffect: false }));
    fixture = TestBed.createComponent(SiteEffect);
    comp = fixture.componentInstance;
  });

  it('loads the current state', () => {
    fixture.detectChanges();
    expect(comp.effects()).toEqual({ tetEffect: false });
  });

  it('turns the effect on for banner managers', () => {
    login(['ROLE_USER', 'ROLE_BANNER_EDIT']);
    const setTet = vi.spyOn(service, 'setTet').mockReturnValue(of({ tetEffect: true, updatedBy: 'a' }));
    fixture.detectChanges();
    expect(comp.canEdit()).toBe(true);

    comp.toggle();

    expect(setTet).toHaveBeenCalledWith(true);
    expect(comp.effects()?.tetEffect).toBe(true);
    expect(comp.justSaved()).toBe(true);
  });

  it('is read-only without banner management permission', () => {
    login(['ROLE_USER', 'ROLE_BANNER_VIEW']);
    fixture.detectChanges();
    expect(comp.canEdit()).toBe(false);
    const button = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('[data-cy=tetToggle]');
    expect(button?.disabled).toBe(true);
  });
});
