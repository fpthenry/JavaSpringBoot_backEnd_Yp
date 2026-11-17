import { HttpClient } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { IconProp } from '@fortawesome/fontawesome-svg-core';
import { TranslatePipe } from '@ngx-translate/core';
import { catchError, of } from 'rxjs';

import { serverApiUrl } from 'app/config';
import { AccountService } from 'app/core/auth';
import Login from 'app/login/login';
import { canEditAuthorities, canViewAuthorities } from 'app/shared/auth/permissions';
import { Authority } from 'app/shared/jhipster/constants';
import { TranslateDirective } from 'app/shared/language';

/** Một ô số liệu trên trang chủ: số bản ghi của một loại dữ liệu, bấm vào mở danh sách. */
export interface DashboardStat {
  key: string;
  label: string;
  icon: IconProp;
  link: string;
  countUrl: string;
  /** Sửa tay: chỉ hiện (và chỉ gọi API đếm) khi tài khoản có quyền xem mục này. */
  authorities: string[];
}

export const DASHBOARD_STATS: DashboardStat[] = [
  {
    key: 'listing',
    label: 'home.stats.listings',
    icon: 'building',
    link: '/listing',
    countUrl: 'api/listings/count',
    authorities: canViewAuthorities('listing'),
  },
  {
    key: 'category',
    label: 'home.stats.categories',
    icon: 'briefcase',
    link: '/category/index',
    countUrl: 'api/categories/count',
    authorities: canViewAuthorities('catalog'),
  },
  {
    key: 'location',
    label: 'home.stats.locations',
    icon: 'map',
    link: '/location/index',
    countUrl: 'api/locations/count',
    authorities: canViewAuthorities('catalog'),
  },
  {
    key: 'blogPost',
    label: 'home.stats.blogPosts',
    icon: 'newspaper',
    link: '/blog-post',
    countUrl: 'api/blog-posts/count',
    authorities: canViewAuthorities('post'),
  },
  {
    key: 'gallery',
    label: 'home.stats.galleries',
    icon: 'images',
    link: '/gallery',
    countUrl: 'api/galleries/count',
    authorities: canViewAuthorities('banner'),
  },
];

/** Việc hay làm trên trang tổng quan. */
export interface QuickAction {
  label: string;
  icon: IconProp;
  link: string;
  /** Sửa tay: chỉ hiện khi tài khoản có một trong các quyền này. */
  authorities: string[];
}

export const QUICK_ACTIONS: QuickAction[] = [
  { label: 'home.quick.newListing', icon: 'building', link: '/listing/new', authorities: canEditAuthorities('listing') },
  { label: 'home.quick.importListing', icon: 'file-import', link: '/listing-import', authorities: [Authority.ADMIN] },
  { label: 'home.quick.newPost', icon: 'newspaper', link: '/blog-post/new', authorities: canEditAuthorities('post') },
  { label: 'home.quick.newBanner', icon: 'images', link: '/gallery-image/new', authorities: canEditAuthorities('banner') },
];

/**
 * Sửa tay: trang chủ quản trị thay cho trang giới thiệu JHipster. Đã đăng nhập: lời chào, số liệu từng loại dữ liệu
 * (bấm để mở), việc hay làm. Chưa đăng nhập: hiện form đăng nhập luôn, bớt một lần bấm.
 */
@Component({
  selector: 'jhi-home',
  templateUrl: './home.html',
  styleUrl: './home.scss',
  imports: [TranslateDirective, TranslatePipe, RouterLink, FontAwesomeModule, Login],
})
export default class Home {
  public readonly account = inject(AccountService).account;
  /** Sửa tay: chỉ các ô số liệu và việc hay làm mà tài khoản có quyền. */
  readonly stats = computed(() =>
    this.account() ? DASHBOARD_STATS.filter(stat => this.accountService.hasAnyAuthority(stat.authorities)) : [],
  );
  readonly quickActions = computed(() =>
    this.account() ? QUICK_ACTIONS.filter(action => this.accountService.hasAnyAuthority(action.authorities)) : [],
  );
  /** Số bản ghi theo khóa; null = đang tải hoặc lỗi. */
  readonly counts = signal<Record<string, number | null>>({});

  private readonly http = inject(HttpClient);
  private readonly accountService = inject(AccountService);

  constructor() {
    effect(() => {
      if (this.account()) {
        this.loadCounts();
      }
    });
  }

  formatCount(count: number | null | undefined): string {
    return count === null || count === undefined ? '…' : count.toLocaleString('vi-VN');
  }

  protected loadCounts(): void {
    for (const stat of this.stats()) {
      this.http
        .get<number>(`${serverApiUrl}${stat.countUrl}`)
        .pipe(catchError(() => of(null)))
        .subscribe(count => this.counts.update(counts => ({ ...counts, [stat.key]: count })));
    }
  }
}
