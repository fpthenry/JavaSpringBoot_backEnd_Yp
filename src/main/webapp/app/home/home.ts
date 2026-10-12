import { HttpClient } from '@angular/common/http';
import { Component, effect, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { IconProp } from '@fortawesome/fontawesome-svg-core';
import { TranslatePipe } from '@ngx-translate/core';
import { catchError, of } from 'rxjs';

import { serverApiUrl } from 'app/config';
import { AccountService } from 'app/core/auth';
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

/**
 * Sửa tay: trang chủ quản trị thay cho trang giới thiệu JHipster. Đã đăng nhập: lời chào, số liệu từng loại dữ liệu
 * (bấm để mở), lối tắt việc hay làm. Chưa đăng nhập: nút đăng nhập.
 */
@Component({
  selector: 'jhi-home',
  templateUrl: './home.html',
  styleUrl: './home.scss',
  imports: [TranslateDirective, TranslatePipe, RouterLink, FontAwesomeModule, HasAnyAuthorityDirective],
})
export default class Home {
  public readonly account = inject(AccountService).account;
  readonly stats = DASHBOARD_STATS;
  /** Số bản ghi theo khóa; null = đang tải hoặc lỗi. */
  readonly counts = signal<Record<string, number | null>>({});

  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);

  constructor() {
    effect(() => {
      if (this.account()) {
        this.loadCounts();
      }
    });
  }

  login(): void {
    this.router.navigate(['/login']);
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
