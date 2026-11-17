import { Component, HostListener, OnInit, computed, inject, signal } from '@angular/core';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { TranslatePipe } from '@ngx-translate/core';
import dayjs from 'dayjs/esm';
import { finalize } from 'rxjs';

import { AccountService } from 'app/core/auth';
import { AlertError } from 'app/shared/alert';
import { canEditAuthorities } from 'app/shared/auth/permissions';
import { TranslateDirective } from 'app/shared/language';

import { SiteEffectService, SiteEffects } from './site-effect.service';

/** Một cánh hoa trong khung xem trước: vị trí, cỡ, thời gian rơi (CSS animation). */
interface PreviewPetal {
  left: number;
  size: number;
  delay: number;
  duration: number;
  drift: number;
  flower: boolean;
}

/**
 * Sửa tay (2026-11-17): trang "Hiệu ứng Tết": một công tắc bật/tắt hoa đào rơi trên toàn bộ website yp.vn, kèm khung
 * xem trước. Website (Next.js) đọc trạng thái qua GET /api/public/v1/site-settings (không cache): tải lại trang là thấy.
 */
@Component({
  selector: 'jhi-site-effect',
  templateUrl: './site-effect.html',
  styleUrl: './site-effect.scss',
  imports: [FontAwesomeModule, TranslatePipe, TranslateDirective, AlertError],
})
export class SiteEffect implements OnInit {
  readonly effects = signal<SiteEffects | null>(null);
  readonly isSaving = signal(false);
  readonly justSaved = signal(false);

  readonly canEdit = computed(() => !!this.account() && this.accountService.hasAnyAuthority(canEditAuthorities('banner')));
  readonly updatedAt = computed(() => {
    const value = this.effects()?.updatedAt;
    return value ? dayjs(value).format('HH:mm DD/MM/YYYY') : null;
  });

  /** Cánh hoa cho khung xem trước, sinh một lần. */
  readonly petals: PreviewPetal[] = Array.from({ length: 18 }, (_, i) => ({
    left: (i * 37) % 100,
    size: 10 + ((i * 7) % 12),
    delay: -((i * 0.83) % 9),
    duration: 7 + ((i * 1.3) % 5),
    drift: ((i % 5) - 2) * 18,
    flower: i % 3 === 0,
  }));

  private readonly accountService = inject(AccountService);
  // eslint-disable-next-line @typescript-eslint/member-ordering
  readonly account = this.accountService.account;
  private readonly service = inject(SiteEffectService);

  ngOnInit(): void {
    this.reload();
  }

  /** Đọc lại trạng thái khi quay lại tab này (có thể đã đổi ở tab hoặc máy khác), tránh bấm theo trạng thái cũ. */
  @HostListener('document:visibilitychange')
  onVisibilityChange(): void {
    if (!document.hidden && !this.isSaving()) {
      this.reload();
    }
  }

  reload(): void {
    this.service.get().subscribe(effects => this.effects.set(effects));
  }

  toggle(): void {
    const current = this.effects();
    if (!current || this.isSaving()) {
      return;
    }
    this.isSaving.set(true);
    this.justSaved.set(false);
    this.service
      .setTet(!current.tetEffect)
      .pipe(finalize(() => this.isSaving.set(false)))
      .subscribe(effects => {
        this.effects.set(effects);
        this.justSaved.set(true);
      });
  }
}
