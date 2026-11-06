import { HttpClient } from '@angular/common/http';
import { Component, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { IconProp } from '@fortawesome/fontawesome-svg-core';
import { TranslatePipe } from '@ngx-translate/core';
import { catchError, of } from 'rxjs';

import { serverApiUrl } from 'app/config';
import { AccountService } from 'app/core/auth';
import Login from 'app/login/login';
import { HasAnyAuthorityDirective } from 'app/shared/auth';
import { TranslateDirective } from 'app/shared/language';

/** Một ô số liệu trên trang chủ: số bản ghi của một loại dữ liệu, bấm vào mở danh sách. */
export interface DashboardStat {
  key: string;
  label: string;
  icon: IconProp;
  link: string;
  countUrl: string;
}

export const DASHBOARD_STATS: DashboardStat[] = [
  { key: 'listing', label: 'home.stats.listings', icon: 'building', link: '/listing', countUrl: 'api/listings/count' },
  { key: 'category', label: 'home.stats.categories', icon: 'briefcase', link: '/category/index', countUrl: 'api/categories/count' },
  { key: 'location', label: 'home.stats.locations', icon: 'map', link: '/location/index', countUrl: 'api/locations/count' },
  { key: 'blogPost', label: 'home.stats.blogPosts', icon: 'newspaper', link: '/blog-post', countUrl: 'api/blog-posts/count' },
  { key: 'gallery', label: 'home.stats.galleries', icon: 'images', link: '/gallery', countUrl: 'api/galleries/count' },
];

/** Việc hay làm trên trang tổng quan. */
export interface QuickAction {
  label: string;
  icon: IconProp;
  link: string;
  adminOnly?: boolean;
}

export const QUICK_ACTIONS: QuickAction[] = [
  { label: 'home.quick.newListing', icon: 'building', link: '/listing/new' },
  { label: 'home.quick.importListing', icon: 'file-import', link: '/listing-import', adminOnly: true },
  { label: 'home.quick.newPost', icon: 'newspaper', link: '/blog-post/new' },
  { label: 'home.quick.newBanner', icon: 'images', link: '/gallery-image/new' },
];

/**
 * Sửa tay: trang chủ quản trị thay cho trang giới thiệu JHipster. Đã đăng nhập: lời chào, số liệu từng loại dữ liệu
 * (bấm để mở), việc hay làm. Chưa đăng nhập: hiện form đăng nhập luôn, bớt một lần bấm.
 */
@Component({
  selector: 'jhi-home',
  templateUrl: './home.html',
  styleUrl: './home.scss',
  imports: [TranslateDirective, TranslatePipe, RouterLink, FontAwesomeModule, HasAnyAuthorityDirective, Login],
})
export default class Home {
  public readonly account = inject(AccountService).account;
  readonly stats = DASHBOARD_STATS;
  readonly quickActions = QUICK_ACTIONS;
  /** Số bản ghi theo khóa; null = đang tải hoặc lỗi. */
  readonly counts = signal<Record<string, number | null>>({});

  private readonly http = inject(HttpClient);

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
    for (const stat of this.stats) {
      this.http
        .get<number>(`${serverApiUrl}${stat.countUrl}`)
        .pipe(catchError(() => of(null)))
        .subscribe(count => this.counts.update(counts => ({ ...counts, [stat.key]: count })));
    }
  }
}
