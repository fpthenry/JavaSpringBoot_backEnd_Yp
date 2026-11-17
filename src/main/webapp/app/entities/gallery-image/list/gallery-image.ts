import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap/modal';
import { TranslatePipe } from '@ngx-translate/core';
import dayjs from 'dayjs/esm';
import { filter, finalize } from 'rxjs';

import { ITEM_DELETED_EVENT } from 'app/config';
import { AccountService } from 'app/core/auth';
import { IGalleryBanner, IGalleryWithImages } from 'app/entities/gallery/gallery.model';
import { GalleryService } from 'app/entities/gallery/service/gallery.service';
import { Alert, AlertError } from 'app/shared/alert';
import { HasAnyAuthorityDirective } from 'app/shared/auth';
import { canEditAuthorities } from 'app/shared/auth/permissions';
import { TranslateDirective } from 'app/shared/language';
import { GalleryImageDeleteDialog } from '../delete/gallery-image-delete-dialog';
import { GalleryImageService } from '../service/gallery-image.service';

/** Trạng thái một banner nhìn từ website. */
export type BannerStatus = 'showing' | 'scheduled' | 'expired' | 'off' | 'noImage';

export const BANNER_STATUSES: BannerStatus[] = ['showing', 'scheduled', 'expired', 'off', 'noImage'];

/** Một banner kèm vị trí của nó, đã tính sẵn trạng thái. */
export interface BannerRow extends IGalleryBanner {
  position: Pick<IGalleryWithImages, 'id' | 'name' | 'code' | 'active'>;
  status: BannerStatus;
}

export function bannerStatus(banner: IGalleryBanner, positionActive: boolean | null | undefined, now = dayjs()): BannerStatus {
  if (!banner.active || positionActive === false) {
    return 'off';
  }
  if (!banner.previewUrl) {
    return 'noImage';
  }
  if (banner.startAt && dayjs(banner.startAt).isAfter(now)) {
    return 'scheduled';
  }
  if (banner.endAt && dayjs(banner.endAt).isBefore(now)) {
    return 'expired';
  }
  return 'showing';
}

/**
 * Sửa tay: trang quản lý banner thay cho bảng JHipster. Lấy mọi vị trí kèm mọi ảnh một lần (GET /api/galleries/with-images,
 * vài chục banner nên không cần phân trang), lọc theo vị trí, trạng thái, tên ngay trên trình duyệt. Bộ lọc nằm trên URL
 * (?position=&status=&q=) để tải lại hay gửi link vẫn giữ nguyên.
 */
@Component({
  selector: 'jhi-gallery-image',
  templateUrl: './gallery-image.html',
  styleUrl: './gallery-image.scss',
  imports: [RouterLink, FormsModule, FontAwesomeModule, AlertError, Alert, TranslateDirective, TranslatePipe, HasAnyAuthorityDirective],
})
export class GalleryImage implements OnInit {
  /** Nút thêm/sửa/xóa, công tắc bật/tắt chỉ có với quyền quản lý banner. */
  readonly editAuthorities = canEditAuthorities('banner');
  readonly statuses = BANNER_STATUSES;

  readonly positions = signal<IGalleryWithImages[]>([]);
  readonly isLoading = signal(false);
  readonly loadError = signal(false);
  /** Banner đang bật/tắt (khóa công tắc trong lúc chờ server). */
  readonly toggling = signal<number | null>(null);

  readonly positionFilter = signal<number | null>(null);
  readonly statusFilter = signal<BannerStatus | null>(null);
  readonly search = signal('');

  readonly canEdit = computed(() => !!this.account() && this.accountService.hasAnyAuthority(this.editAuthorities));

  /** Mọi banner của mọi vị trí, sắp theo vị trí rồi thứ tự hiển thị. */
  readonly banners = computed<BannerRow[]>(() => {
    const now = dayjs();
    return this.positions().flatMap(position =>
      [...position.images]
        .sort((a, b) => (a.displayOrder ?? Number.MAX_SAFE_INTEGER) - (b.displayOrder ?? Number.MAX_SAFE_INTEGER) || a.id - b.id)
        .map(image => ({
          ...image,
          position: { id: position.id, name: position.name, code: position.code, active: position.active },
          status: bannerStatus(image, position.active, now),
        })),
    );
  });

  /** Banner sau khi lọc vị trí và tên (chưa lọc trạng thái): dùng để đếm số trên các nút trạng thái. */
  readonly inPosition = computed(() => {
    const position = this.positionFilter();
    const query = this.search().trim().toLowerCase();
    return this.banners().filter(
      banner =>
        (position === null || banner.position.id === position) &&
        (!query || `${banner.title ?? ''} ${banner.linkUrl ?? ''}`.toLowerCase().includes(query)),
    );
  });

  readonly visible = computed(() => {
    const status = this.statusFilter();
    return status === null ? this.inPosition() : this.inPosition().filter(banner => banner.status === status);
  });

  /** Số banner theo trạng thái trong phạm vi vị trí đang chọn. */
  readonly statusCounts = computed(() => {
    const counts: Record<BannerStatus, number> = { showing: 0, scheduled: 0, expired: 0, off: 0, noImage: 0 };
    for (const banner of this.inPosition()) {
      counts[banner.status]++;
    }
    return counts;
  });

  readonly showingTotal = computed(() => this.banners().filter(banner => banner.status === 'showing').length);

  /** Số banner đang hiện trên website theo từng vị trí. */
  readonly showingByPosition = computed(() => {
    const counts = new Map<number, number>();
    for (const banner of this.banners()) {
      if (banner.status === 'showing') {
        counts.set(banner.position.id, (counts.get(banner.position.id) ?? 0) + 1);
      }
    }
    return counts;
  });

  readonly selectedPosition = computed(() => this.positions().find(position => position.id === this.positionFilter()) ?? null);

  private readonly accountService = inject(AccountService);
  // eslint-disable-next-line @typescript-eslint/member-ordering
  readonly account = this.accountService.account;
  private readonly galleryService = inject(GalleryService);
  private readonly galleryImageService = inject(GalleryImageService);
  private readonly modalService = inject(NgbModal);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.route.queryParamMap.subscribe(params => {
      const position = Number(params.get('position'));
      this.positionFilter.set(Number.isInteger(position) && position > 0 ? position : null);
      const status = params.get('status') as BannerStatus | null;
      this.statusFilter.set(status && BANNER_STATUSES.includes(status) ? status : null);
      this.search.set(params.get('q') ?? '');
    });
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.loadError.set(false);
    this.galleryService
      .withImages()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: positions => this.positions.set(positions),
        error: () => this.loadError.set(true),
      });
  }

  setPosition(id: number | null): void {
    this.navigate({ position: id });
  }

  setStatus(status: BannerStatus | null): void {
    this.navigate({ status: this.statusFilter() === status ? null : status });
  }

  setSearch(query: string): void {
    this.navigate({ q: query.trim() || null });
  }

  countIn(position: IGalleryWithImages): number {
    return position.images.length;
  }

  /** Khoảng ngày hiển thị gọn: "05/10/2026 → 31/12/2026", "Từ 05/10/2026", "Không giới hạn". */
  dateRange(banner: IGalleryBanner): { from: string | null; to: string | null } {
    return {
      from: banner.startAt ? dayjs(banner.startAt).format('DD/MM/YYYY') : null,
      to: banner.endAt ? dayjs(banner.endAt).format('DD/MM/YYYY') : null,
    };
  }

  linkHost(url: string | null | undefined): string {
    if (!url) {
      return '';
    }
    try {
      return new URL(url, globalThis.location.origin).host;
    } catch {
      return url;
    }
  }

  toggleActive(banner: BannerRow): void {
    this.toggling.set(banner.id);
    this.galleryImageService
      .partialUpdate({ id: banner.id, active: !banner.active })
      .pipe(finalize(() => this.toggling.set(null)))
      .subscribe(() => this.load());
  }

  delete(banner: BannerRow): void {
    const modalRef = this.modalService.open(GalleryImageDeleteDialog, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.galleryImage = { id: banner.id, title: banner.title };
    modalRef.closed.pipe(filter(reason => reason === ITEM_DELETED_EVENT)).subscribe(() => this.load());
  }

  private navigate(change: Record<string, string | number | null>): void {
    this.router.navigate([], { relativeTo: this.route, queryParams: change, queryParamsHandling: 'merge', replaceUrl: true });
  }
}
