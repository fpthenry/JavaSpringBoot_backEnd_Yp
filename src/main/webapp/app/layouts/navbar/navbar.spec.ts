import { beforeEach, describe, expect, it, vi } from 'vitest';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

import { Account, AccountService } from 'app/core/auth';
import { ProfileInfo } from 'app/layouts/profiles/profile-info.model';
import { ProfileService } from 'app/layouts/profiles/profile.service';
import { LoginService } from 'app/login/login.service';

import Navbar from './navbar';
import { isUnder } from './navbar-menu';

describe('Navbar Component', () => {
  let comp: Navbar;
  let fixture: ComponentFixture<Navbar>;
  let accountService: AccountService;
  let profileService: ProfileService;
  const account: Account = {
    activated: true,
    authorities: [],
    email: '',
    firstName: 'John',
    langKey: '',
    lastName: 'Doe',
    login: 'john.doe',
    imageUrl: '',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        {
          provide: ActivatedRoute,
          useValue: {},
        },
        provideHttpClientTesting(),
        LoginService,
      ],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(Navbar);
    comp = fixture.componentInstance;
    accountService = TestBed.inject(AccountService);
    profileService = TestBed.inject(ProfileService);
  });

  it('should call profileService.getProfileInfo on init', () => {
    // GIVEN
    vi.spyOn(profileService, 'getProfileInfo').mockReturnValue(of(new ProfileInfo()));

    // WHEN
    comp.ngOnInit();

    // THEN
    expect(profileService.getProfileInfo).toHaveBeenCalled();
  });

  it('should hold current authenticated user in variable account', () => {
    // WHEN
    comp.ngOnInit();

    // THEN
    expect(comp.account()).toBeNull();

    // WHEN
    accountService.authenticate(account);

    // THEN
    expect(comp.account()).toEqual(account);

    // WHEN
    accountService.authenticate(null);

    // THEN
    expect(comp.account()).toBeNull();
  });

  it('should hold current authenticated user in variable account if user is authenticated before page load', () => {
    // GIVEN
    accountService.authenticate(account);

    // WHEN
    comp.ngOnInit();

    // THEN
    expect(comp.account()).toEqual(account);

    // WHEN
    accountService.authenticate(null);

    // THEN
    expect(comp.account()).toBeNull();
  });

  describe('menu', () => {
    const links = (): (string | undefined)[] => comp.menu().flatMap(group => group.items.map(item => item.link));

    it('should hide admin items and empty groups from non-admin users', () => {
      accountService.authenticate(account);
      expect(links()).toContain('/listing');
      expect(links()).not.toContain('/user-management');
      expect(links()).not.toContain('/listing-import');
    });

    it('should show admin items to admins, API docs only when OpenAPI is enabled', () => {
      accountService.authenticate({ ...account, authorities: ['ROLE_ADMIN'] });
      expect(links()).toContain('/user-management');
      expect(links()).not.toContain('/admin/docs');

      comp.openAPIEnabled.set(true);
      expect(links()).toContain('/admin/docs');
    });

    it('should toggle a submenu', () => {
      comp.toggleMenu('a');
      expect(comp.openMenu()).toBe('a');
      comp.toggleMenu('a');
      expect(comp.openMenu()).toBeNull();
    });

    it('should tell a child page from a sibling with the same prefix', () => {
      expect(isUnder('/category/5/edit', '/category')).toBe(true);
      expect(isUnder('/gallery-image', '/gallery')).toBe(false);
    });
  });
});
