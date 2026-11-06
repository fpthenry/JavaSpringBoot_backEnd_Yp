import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { IsActiveMatchOptions, NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbDropdown, NgbDropdownMenu, NgbDropdownToggle } from '@ng-bootstrap/ng-bootstrap/dropdown';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { environment } from 'environments/environment';
import { filter } from 'rxjs';

import { LANGUAGES } from 'app/config';
import { AccountService, StateStorageService } from 'app/core/auth';
import { ProfileService } from 'app/layouts/profiles/profile.service';
import { LoginService } from 'app/login/login.service';
import { Authority } from 'app/shared/jhipster/constants';
import { FindLanguageFromKeyPipe, TranslateDirective } from 'app/shared/language';

import ActiveMenuDirective from './active-menu.directive';
import { MENU_GROUPS, MenuGroup, MenuItem, isUnder } from './navbar-menu';

@Component({
  selector: 'jhi-navbar',
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
  imports: [
    RouterLink,
    RouterLinkActive,
    FontAwesomeModule,
    NgbDropdown,
    NgbDropdownMenu,
    NgbDropdownToggle,
    ActiveMenuDirective,
    FindLanguageFromKeyPipe,
    TranslateDirective,
    TranslatePipe,
  ],
})
export default class Navbar implements OnInit {
  readonly inProduction = signal(true);
  readonly isNavbarCollapsed = signal(true);
  readonly languages = LANGUAGES;
  readonly openAPIEnabled = signal(false);
  readonly version: string;
  readonly account = inject(AccountService).account;
  /** Nhóm con đang mở; tự mở theo trang hiện tại, bấm tay thì đè lên. */
  readonly openMenu = signal<string | null>(null);
  /** Trang danh sách doanh nghiệp đã có ô tìm kiếm đầy đủ, ô tìm nhanh ở thanh trên ẩn đi. */
  readonly onListing = signal(false);
  /** Menu bên trái theo quyền của tài khoản; nhóm không còn mục nào thì ẩn. */
  readonly menu = computed<MenuGroup[]>(() => {
    const isAdmin = this.account()?.authorities.includes(Authority.ADMIN) ?? false;
    return MENU_GROUPS.map(group => ({
      ...group,
      items: group.items.filter(item => (!item.adminOnly || isAdmin) && (!item.openApi || this.openAPIEnabled())),
    })).filter(group => group.items.length > 0);
  });

  private readonly loginService = inject(LoginService);
  private readonly translateService = inject(TranslateService);
  private readonly stateStorageService = inject(StateStorageService);
  private readonly profileService = inject(ProfileService);
  private readonly router = inject(Router);

  constructor() {
    const { VERSION } = environment;
    if (VERSION) {
      this.version = VERSION.toLowerCase().startsWith('v') ? VERSION : `v${VERSION}`;
    } else {
      this.version = '';
    }
  }

  ngOnInit(): void {
    this.openMenuOf(this.router.url);
    this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe(event => this.openMenuOf(event.urlAfterRedirects));
    this.profileService.getProfileInfo().subscribe(profileInfo => {
      this.inProduction.set(profileInfo.inProduction ?? true);
      this.openAPIEnabled.set(profileInfo.openAPIEnabled ?? false);
    });
  }

  changeLanguage(languageKey: string): void {
    this.stateStorageService.storeLocale(languageKey);
    this.translateService.use(languageKey);
  }

  activeOptions(item: MenuItem): IsActiveMatchOptions {
    return { paths: item.exact ? 'exact' : 'subset', queryParams: 'ignored', fragment: 'ignored', matrixParams: 'ignored' };
  }

  toggleMenu(label: string): void {
    this.openMenu.update(open => (open === label ? null : label));
  }

  /** Ô tìm nhanh trên thanh trên: mở danh sách doanh nghiệp với từ khóa. */
  searchListing(query: string): void {
    const search = query.trim();
    this.collapseNavbar();
    this.router.navigate(['/listing'], { queryParams: search ? { search } : {} });
  }

  collapseNavbar(): void {
    this.isNavbarCollapsed.set(true);
  }

  logout(): void {
    this.collapseNavbar();
    this.loginService.logout();
    this.router.navigate(['']);
  }

  private openMenuOf(url: string): void {
    const path = url.split('?')[0];
    this.onListing.set(path === '/listing');
    const owner = MENU_GROUPS.flatMap(group => group.items).find(item => item.children?.some(child => isUnder(path, child.link!)));
    if (owner) {
      this.openMenu.set(owner.label);
    }
  }
}
