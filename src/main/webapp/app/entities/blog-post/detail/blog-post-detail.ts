import { Component, ElementRef, computed, effect, inject, input, signal, viewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  NgbDropdown,
  NgbDropdownButtonItem,
  NgbDropdownItem,
  NgbDropdownMenu,
  NgbDropdownToggle,
} from '@ng-bootstrap/ng-bootstrap/dropdown';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap/modal';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { filter } from 'rxjs';

import { ITEM_DELETED_EVENT } from 'app/config';
import { Alert, AlertError } from 'app/shared/alert';
import { FormatMediumDatePipe, FormatMediumDatetimePipe } from 'app/shared/date';
import { CopyButton } from 'app/shared/detail';
import { decodeEntities } from 'app/shared/form';
import { TranslateDirective } from 'app/shared/language';
import {
  blogStatusLabelKey,
  cleanArticleHtml,
  httpsUrl,
  excerptWithoutLink,
  plainExcerpt,
  publicUrlFromExcerpt,
  readingStats,
} from '../blog-post-display';
import { IBlogPost } from '../blog-post.model';
import { BlogPostDeleteDialog } from '../delete/blog-post-delete-dialog';

/** Tiêu đề dài hơn mức này dễ bị Google cắt. */
export const TITLE_MAX = 70;

/**
 * Sửa tay: trang xem bài viết, cùng kiểu trang xem ngành nghề. Đầu trang: tiêu đề, một dòng trạng thái · ngày đăng ·
 * tác giả · lượt xem · phút đọc; nút Sửa (phím E), Xem trên website (link "Xem bài viết" có sẵn trong tóm tắt), menu ⋯.
 * Bài: tóm tắt làm đoạn dẫn, nội dung đã bỏ shortcode WPBakery; ảnh không tải được (bị Content-Security-Policy chặn)
 * thay bằng khung "Ảnh". Cột phải: danh mục, thẻ (bấm để lọc danh sách), xuất bản, gợi ý hoàn thiện, thông tin.
 */
@Component({
  selector: 'jhi-blog-post-detail',
  templateUrl: './blog-post-detail.html',
  imports: [
    FontAwesomeModule,
    Alert,
    AlertError,
    TranslateDirective,
    TranslatePipe,
    RouterLink,
    FormatMediumDatePipe,
    FormatMediumDatetimePipe,
    CopyButton,
    NgbDropdown,
    NgbDropdownToggle,
    NgbDropdownMenu,
    NgbDropdownItem,
    // Cần cho <button ngbDropdownItem [disabled]>
    NgbDropdownButtonItem,
  ],
  host: { '(document:keydown)': 'onKeydown($event)' },
})
export class BlogPostDetail {
  readonly blogPost = input<IBlogPost | null>(null);

  readonly title = computed(() => decodeEntities(this.blogPost()?.title));
  readonly lead = computed(() => plainExcerpt(excerptWithoutLink(this.blogPost()?.excerpt), 600));
  /**
   * Tóm tắt WordPress tự sinh = vài câu đầu của bài (kết thúc bằng "…"): trùng nội dung nên không hiện làm đoạn dẫn.
   * Chỉ hiện khi tóm tắt được viết riêng.
   */
  readonly showLead = computed(() => {
    const lead = this.lead().replace(/…$/, '').trim();
    if (!lead) {
      return false;
    }
    const body = plainExcerpt(this.blogPost()?.content, lead.length + 20);
    return !body.startsWith(lead);
  });
  /** Ảnh bìa không tải được (bị chặn, hỏng): ẩn cả khung. */
  readonly coverFailed = signal(false);
  readonly articleHtml = computed(() => cleanArticleHtml(this.blogPost()?.content));
  readonly stats = computed(() => readingStats(this.blogPost()?.content));
  readonly publicUrl = computed(() => publicUrlFromExcerpt(this.blogPost()?.excerpt));
  /** Gợi ý hoàn thiện bài (để trong form sửa). */
  readonly suggestions = computed(() => {
    const post = this.blogPost();
    if (!post) {
      return [];
    }
    return [
      { key: 'excerpt', ok: !!this.lead() },
      { key: 'thumbnail', ok: !!post.thumbnail?.trim() },
      { key: 'category', ok: (post.categories?.length ?? 0) > 0 },
      { key: 'tag', ok: (post.tags?.length ?? 0) > 0 },
      { key: 'titleLength', ok: this.title().length <= TITLE_MAX },
    ].filter(item => !item.ok);
  });

  protected readonly decode = decodeEntities;
  protected readonly blogStatusLabelKey = blogStatusLabelKey;
  protected readonly httpsUrl = httpsUrl;
  protected readonly titleMax = TITLE_MAX;
  protected readonly modalService = inject(NgbModal);
  protected readonly router = inject(Router);
  protected readonly translateService = inject(TranslateService);

  private readonly article = viewChild<ElementRef<HTMLElement>>('article');

  constructor() {
    // Ảnh trong bài nằm trên website cũ: bị chặn hoặc hỏng thì thay bằng khung "Ảnh" thay vì biểu tượng ảnh vỡ
    effect(() => {
      this.blogPost();
      this.coverFailed.set(false);
    });
    effect(() => {
      this.articleHtml();
      const root = this.article()?.nativeElement;
      if (!root) {
        return;
      }
      queueMicrotask(() => {
        for (const image of Array.from(root.querySelectorAll('img'))) {
          image.addEventListener('error', () => this.replaceBrokenImage(image), { once: true });
          if (image.complete && image.naturalWidth === 0 && image.getAttribute('src')) {
            this.replaceBrokenImage(image);
          }
        }
      });
    });
  }

  categoryQueryParams(id: number): Record<string, number> {
    return { categoryId: id };
  }

  tagQueryParams(id: number): Record<string, number> {
    return { tagId: id };
  }

  /** Phím E (không gõ trong ô nhập, không giữ Ctrl/Alt): mở trang sửa. */
  onKeydown(event: KeyboardEvent): void {
    const target = event.target as HTMLElement | null;
    const typing = !!target?.closest('input, textarea, select, [contenteditable="true"]');
    const post = this.blogPost();
    if (post && !typing && !event.ctrlKey && !event.metaKey && !event.altKey && event.key.toLowerCase() === 'e') {
      event.preventDefault();
      void this.router.navigate(['/blog-post', post.id, 'edit']);
    }
  }

  delete(blogPost: IBlogPost): void {
    const modalRef = this.modalService.open(BlogPostDeleteDialog, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.blogPost = blogPost;
    modalRef.closed.pipe(filter(reason => reason === ITEM_DELETED_EVENT)).subscribe(() => void this.router.navigate(['/blog-post']));
  }

  /** Về trang trước trong ứng dụng; mở thẳng trang xem (link, tab mới) thì về danh sách. */
  previousState(): void {
    const navigationId = (globalThis.history.state as { navigationId?: number } | null)?.navigationId ?? 1;
    if (navigationId > 1) {
      globalThis.history.back();
    } else {
      void this.router.navigate(['/blog-post']);
    }
  }

  protected replaceBrokenImage(image: HTMLImageElement): void {
    const box = document.createElement('span');
    box.className = 'article-image-missing';
    box.textContent = image.alt.trim() || this.translateService.instant('javaSpringBootBackEndApp.blogPost.detail.imageBlocked');
    box.title = image.getAttribute('src') ?? '';
    image.replaceWith(box);
  }
}
