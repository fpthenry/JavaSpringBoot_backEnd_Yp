import { Component, computed, input, signal } from '@angular/core';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { TranslatePipe } from '@ngx-translate/core';

import { TranslateDirective } from 'app/shared/language';

import { SeoResult, SeoStatus } from './seo-analyzer';

/**
 * Sửa tay (2026-11-17): khung SEO trong trang soạn bài: điểm, xem trước trên Google, danh sách việc cần sửa.
 * Chỉ hiển thị; phần tính điểm ở seo-analyzer.ts.
 */
@Component({
  selector: 'jhi-seo-panel',
  templateUrl: './seo-panel.html',
  styleUrl: './seo-panel.scss',
  imports: [FontAwesomeModule, TranslatePipe, TranslateDirective],
})
export class SeoPanel {
  readonly result = input.required<SeoResult>();
  /** Hiện cả các mục đã đạt (mặc định chỉ hiện việc cần sửa). */
  readonly showPassed = signal(false);

  readonly problems = computed(() => this.result().checks.filter(check => check.status !== 'good'));
  readonly passed = computed(() => this.result().checks.filter(check => check.status === 'good'));

  /** Vòng tròn điểm: chu vi 2πr với r = 26. */
  readonly circumference = 2 * Math.PI * 26;
  readonly dashOffset = computed(() => this.circumference * (1 - this.result().score / 100));

  icon(status: SeoStatus): 'circle-check' | 'triangle-exclamation' | 'circle-xmark' {
    return status === 'good' ? 'circle-check' : status === 'ok' ? 'triangle-exclamation' : 'circle-xmark';
  }
}
