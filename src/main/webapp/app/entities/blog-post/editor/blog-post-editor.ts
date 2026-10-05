import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { TranslatePipe } from '@ngx-translate/core';
import dayjs from 'dayjs/esm';
import { finalize, map } from 'rxjs';

import { DATE_TIME_FORMAT } from 'app/config/input.constants';
import { IBlogCategory } from 'app/entities/blog-category/blog-category.model';
import { BlogCategoryService } from 'app/entities/blog-category/service/blog-category.service';
import { ITag } from 'app/entities/tag/tag.model';
import { TagService } from 'app/entities/tag/service/tag.service';
import { AlertError } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { extractImageUrls } from 'app/shared/rich-text/html-sanitizer';
import { RichTextEditor } from 'app/shared/rich-text/rich-text-editor';
import { IBlogPost, NewBlogPost } from '../blog-post.model';
import { BlogPostService } from '../service/blog-post.service';

/** Một danh mục trong cây checkbox, kèm độ sâu để thụt lề. */
interface CategoryRow {
  category: IBlogCategory;
  depth: number;
}

/** Tạo slug từ tiêu đề tiếng Việt: bỏ dấu, đ → d, chữ thường, nối bằng gạch ngang. */
export function slugify(text: string | null | undefined): string {
  return (text ?? '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[đĐ]/g, 'd')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 200);
}

/**
 * Trang soạn bài viết kiểu WordPress: tiêu đề, nội dung (dán từ website/Word), tóm tắt, danh mục dạng cây có ô tích,
 * thẻ, ảnh đại diện (URL hoặc chọn ảnh trong bài), trạng thái xuất bản.
 * Sửa tay: thay cho trang cập nhật JHipster sinh ra (route new và :id/edit trỏ về đây).
 */
@Component({
  selector: 'jhi-blog-post-editor',
  templateUrl: './blog-post-editor.html',
  imports: [ReactiveFormsModule, FormsModule, RouterLink, FontAwesomeModule, TranslatePipe, TranslateDirective, AlertError, RichTextEditor],
})
export class BlogPostEditor {
  readonly isSaving = signal(false);
  readonly categories = signal<IBlogCategory[]>([]);
  readonly tags = signal<ITag[]>([]);
  readonly selectedCategoryIds = signal(new Set<number>());
  readonly selectedTagIds = signal(new Set<number>());
  readonly categoryFilter = signal('');
  readonly tagFilter = signal('');
  readonly showContentImages = signal(false);

  readonly editForm = new FormGroup({
    title: new FormControl<string | null>(null, { validators: [Validators.required, Validators.maxLength(500)] }),
    slug: new FormControl<string | null>(null, { validators: [Validators.maxLength(500)] }),
    content: new FormControl<string | null>(null),
    excerpt: new FormControl<string | null>(null),
    thumbnail: new FormControl<string | null>(null, { validators: [Validators.maxLength(500)] }),
    status: new FormControl<string>('draft', { nonNullable: true }),
    publishedAt: new FormControl<string | null>(null),
    authorName: new FormControl<string | null>(null, { validators: [Validators.maxLength(255)] }),
  });

  /** Danh mục dạng cây (cha trước, con thụt lề), lọc theo ô tìm kiếm (giữ cả cha của mục khớp). */
  readonly categoryRows = computed<CategoryRow[]>(() => {
    const all = this.categories();
    const filter = slugify(this.categoryFilter());
    const children = new Map<number | null, IBlogCategory[]>();
    for (const category of all) {
      const parentId = category.parent?.id ?? null;
      children.set(parentId, [...(children.get(parentId) ?? []), category]);
    }
    const byName = (a: IBlogCategory, b: IBlogCategory): number => (a.name ?? '').localeCompare(b.name ?? '', 'vi');
    const matches = (category: IBlogCategory): boolean =>
      !filter || slugify(category.name).includes(filter) || (children.get(category.id) ?? []).some(matches);
    const rows: CategoryRow[] = [];
    const walk = (parentId: number | null, depth: number): void => {
      for (const category of (children.get(parentId) ?? []).sort(byName)) {
        if (matches(category)) {
          rows.push({ category, depth });
          walk(category.id, depth + 1);
        }
      }
    };
    walk(null, 0);
    return rows;
  });

  readonly filteredTags = computed(() => {
    const filter = slugify(this.tagFilter());
    return this.tags()
      .filter(tag => !filter || slugify(tag.name).includes(filter))
      .sort((a, b) => (a.name ?? '').localeCompare(b.name ?? '', 'vi'));
  });

  readonly contentImages = computed(() => extractImageUrls(this.contentValue()));

  /** Bài đang sửa (null khi thêm mới). Giữ các field không hiện trên form: wpId, viewCount, createdAt. */
  protected original: IBlogPost | null = null;
  /** Người dùng đã tự sửa slug thì không tự sinh theo tiêu đề nữa. */
  protected slugEdited = false;

  protected readonly blogPostService = inject(BlogPostService);
  protected readonly blogCategoryService = inject(BlogCategoryService);
  protected readonly tagService = inject(TagService);
  protected readonly router = inject(Router);

  protected readonly contentValue = toSignal(this.editForm.controls.content.valueChanges, { initialValue: null });
  protected readonly thumbnailValue = toSignal(this.editForm.controls.thumbnail.valueChanges, { initialValue: null });

  constructor() {
    inject(ActivatedRoute)
      .data.pipe(map(data => data['blogPost'] as IBlogPost | null))
      .subscribe(post => this.load(post));
    this.blogCategoryService.query({ page: 0, size: 1000, sort: ['name,asc'] }).subscribe(res => this.categories.set(res.body ?? []));
    this.tagService.query({ page: 0, size: 1000, sort: ['name,asc'] }).subscribe(res => this.tags.set(res.body ?? []));
    this.editForm.controls.title.valueChanges.subscribe(title => {
      if (!this.slugEdited && !this.original?.wpId) {
        this.editForm.controls.slug.setValue(slugify(title), { emitEvent: false });
      }
    });
  }

  isCategoryChecked(id: number): boolean {
    return this.selectedCategoryIds().has(id);
  }

  toggleCategory(id: number): void {
    this.selectedCategoryIds.update(ids => toggled(ids, id));
  }

  isTagChecked(id: number): boolean {
    return this.selectedTagIds().has(id);
  }

  toggleTag(id: number): void {
    this.selectedTagIds.update(ids => toggled(ids, id));
  }

  onSlugInput(): void {
    this.slugEdited = true;
  }

  useAsThumbnail(url: string): void {
    this.editForm.controls.thumbnail.setValue(url);
    this.showContentImages.set(false);
  }

  previousState(): void {
    window.history.back();
  }

  saveDraft(): void {
    this.save('draft');
  }

  publish(): void {
    this.save('publish');
  }

  protected save(status: 'draft' | 'publish'): void {
    this.editForm.markAllAsTouched();
    if (this.editForm.invalid) {
      return;
    }
    const value = this.editForm.getRawValue();
    const now = dayjs();
    let publishedAt = value.publishedAt ? dayjs(value.publishedAt, DATE_TIME_FORMAT) : null;
    if (status === 'publish' && !publishedAt) {
      publishedAt = now;
    }
    const pickById = <T extends { id: number }>(items: T[], ids: Set<number>): Pick<T, 'id'>[] =>
      items.filter(item => ids.has(item.id)).map(item => ({ id: item.id }));
    const post = {
      ...(this.original ?? {}),
      id: this.original?.id ?? null,
      title: value.title?.trim(),
      slug: blankToNull(value.slug) ?? slugify(value.title),
      content: value.content,
      excerpt: value.excerpt,
      thumbnail: blankToNull(value.thumbnail),
      status,
      publishedAt,
      authorName: blankToNull(value.authorName),
      viewCount: this.original?.viewCount ?? 0,
      createdAt: this.original?.createdAt ?? now,
      updatedAt: now,
      categories: pickById(this.categories(), this.selectedCategoryIds()),
      tags: pickById(this.tags(), this.selectedTagIds()),
    };
    this.isSaving.set(true);
    const request = post.id === null ? this.blogPostService.create(post as NewBlogPost) : this.blogPostService.update(post as IBlogPost);
    request.pipe(finalize(() => this.isSaving.set(false))).subscribe({
      next: saved => {
        void this.router.navigate(['/blog-post', saved.id, 'view']);
      },
    });
  }

  protected load(post: IBlogPost | null): void {
    this.original = post;
    this.slugEdited = !!post?.slug;
    this.editForm.reset({
      title: post?.title ?? null,
      slug: post?.slug ?? null,
      content: post?.content ?? null,
      excerpt: post?.excerpt ?? null,
      thumbnail: post?.thumbnail ?? null,
      status: post?.status ?? 'draft',
      publishedAt: post?.publishedAt ? post.publishedAt.format(DATE_TIME_FORMAT) : null,
      authorName: post?.authorName ?? null,
    });
    this.selectedCategoryIds.set(new Set((post?.categories ?? []).map(c => c.id)));
    this.selectedTagIds.set(new Set((post?.tags ?? []).map(t => t.id)));
  }
}

function toggled(ids: Set<number>, id: number): Set<number> {
  const next = new Set(ids);
  if (next.has(id)) {
    next.delete(id);
  } else {
    next.add(id);
  }
  return next;
}

/** Chuỗi rỗng hoặc chỉ có khoảng trắng thành null. */
function blankToNull(value: string | null | undefined): string | null {
  const trimmed = value?.trim();
  return trimmed === undefined || trimmed === '' ? null : trimmed;
}
