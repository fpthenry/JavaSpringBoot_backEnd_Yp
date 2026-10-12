import { AfterViewInit, Component, DestroyRef, inject, input, signal } from '@angular/core';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { IconProp } from '@fortawesome/fontawesome-svg-core';
import { TranslatePipe } from '@ngx-translate/core';

/** Một mục trong thanh mục lục: id của phần tử (section) trên trang, khóa dịch, icon. */
export interface DetailSection {
  id: string;
  label: string;
  icon?: IconProp;
}

/**
 * Thanh mục lục dính đầu trang cho trang xem dài: bấm để cuộn tới phần đó, phần đang xem được tô đậm
 * (IntersectionObserver). Các section cần có id trùng và lớp {@code .detail-section} (để chừa chỗ cho thanh khi cuộn).
 */
@Component({
  selector: 'jhi-section-nav',
  template: `<nav class="section-nav" [attr.aria-label]="'global.detail.sections' | translate">
    @for (section of sections(); track section.id) {
      <a
        class="section-nav-link"
        [class.active]="active() === section.id"
        [attr.href]="'#' + section.id"
        [attr.aria-current]="active() === section.id ? 'true' : null"
        (click)="go(section.id, $event)"
      >
        @if (section.icon) {
          <fa-icon [icon]="section.icon" class="me-1" />
        }
        {{ section.label | translate }}
      </a>
    }
  </nav>`,
  imports: [FontAwesomeModule, TranslatePipe],
})
export class SectionNav implements AfterViewInit {
  readonly sections = input.required<DetailSection[]>();
  readonly active = signal<string | null>(null);

  private readonly destroyRef = inject(DestroyRef);
  /** Sau khi bấm, giữ mục đã chọn trong lúc trang cuộn mượt (không để observer đổi sang mục đi ngang qua). */
  private lockedUntil = 0;

  ngAfterViewInit(): void {
    this.active.set(this.sections()[0]?.id ?? null);
    if (typeof IntersectionObserver === 'undefined') {
      return;
    }
    // Phần nằm ở dải 20–45% chiều cao màn hình là phần "đang xem"
    const observer = new IntersectionObserver(
      entries => {
        if (Date.now() < this.lockedUntil) {
          return;
        }
        const visible = entries.filter(entry => entry.isIntersecting).sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);
        if (visible.length > 0) {
          this.active.set(visible[0].target.id);
        }
      },
      { rootMargin: '-20% 0px -55% 0px' },
    );
    // Section có thể vẽ sau (dữ liệu tải xong): đợi một nhịp rồi mới tìm
    setTimeout(() => {
      for (const section of this.sections()) {
        const element = document.getElementById(section.id);
        if (element) {
          observer.observe(element);
        }
      }
    });
    // Cuộn tới cuối trang: các section cuối không lên được tới đầu màn hình, coi section cuối là đang xem
    const onScroll = (): void => {
      const atBottom = globalThis.innerHeight + globalThis.scrollY >= document.documentElement.scrollHeight - 4;
      const last = this.sections().at(-1);
      if (atBottom && last && Date.now() >= this.lockedUntil) {
        this.active.set(last.id);
      }
    };
    globalThis.addEventListener('scroll', onScroll, { passive: true });
    this.destroyRef.onDestroy(() => {
      observer.disconnect();
      globalThis.removeEventListener('scroll', onScroll);
    });
  }

  go(id: string, event: Event): void {
    event.preventDefault();
    this.active.set(id);
    this.lockedUntil = Date.now() + 1000;
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}
