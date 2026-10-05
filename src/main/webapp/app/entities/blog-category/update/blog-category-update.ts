import { HttpResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap/tooltip';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable, finalize, map } from 'rxjs';

import { DataUtils, EventManager, EventWithContent, FileLoadError } from 'app/core/util';
import { IBlogPost } from 'app/entities/blog-post/blog-post.model';
import { BlogPostService } from 'app/entities/blog-post/service/blog-post.service';
import { AlertError, AlertErrorModel } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { IBlogCategory } from '../blog-category.model';
import { BlogCategoryService } from '../service/blog-category.service';

import { BlogCategoryFormGroup, BlogCategoryFormService } from './blog-category-form.service';

@Component({
  selector: 'jhi-blog-category-update',
  templateUrl: './blog-category-update.html',
  imports: [TranslateDirective, TranslatePipe, FontAwesomeModule, AlertError, ReactiveFormsModule, NgbTooltip],
})
export class BlogCategoryUpdate implements OnInit {
  readonly isSaving = signal(false);
  blogCategory: IBlogCategory | null = null;

  blogCategoriesSharedCollection = signal<IBlogCategory[]>([]);
  blogPostsSharedCollection = signal<IBlogPost[]>([]);

  protected dataUtils = inject(DataUtils);
  protected eventManager = inject(EventManager);
  protected blogCategoryService = inject(BlogCategoryService);
  protected blogCategoryFormService = inject(BlogCategoryFormService);
  protected blogPostService = inject(BlogPostService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: BlogCategoryFormGroup = this.blogCategoryFormService.createBlogCategoryFormGroup();

  compareBlogCategory = (o1: IBlogCategory | null, o2: IBlogCategory | null): boolean =>
    this.blogCategoryService.compareBlogCategory(o1, o2);

  compareBlogPost = (o1: IBlogPost | null, o2: IBlogPost | null): boolean => this.blogPostService.compareBlogPost(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ blogCategory }) => {
      this.blogCategory = blogCategory;
      if (blogCategory) {
        this.updateForm(blogCategory);
      }

      this.loadRelationshipsOptions();
    });
  }

  byteSize(base64String: string): string {
    return this.dataUtils.byteSize(base64String);
  }

  openFile(base64String: string, contentType: string | null | undefined): void {
    this.dataUtils.openFile(base64String, contentType);
  }

  setFileData(event: Event, field: string, isImage: boolean): void {
    this.dataUtils.loadFileToForm(event, this.editForm, field, isImage).subscribe({
      error: (err: FileLoadError) =>
        this.eventManager.broadcast(
          new EventWithContent<AlertErrorModel>('javaSpringBootBackEndApp.error', { ...err, key: `error.file.${err.key}` }),
        ),
    });
  }

  previousState(): void {
    globalThis.history.back();
  }

  save(): void {
    this.isSaving.set(true);
    const blogCategory = this.blogCategoryFormService.getBlogCategory(this.editForm);
    if (blogCategory.id === null) {
      this.subscribeToSaveResponse(this.blogCategoryService.create(blogCategory));
    } else {
      this.subscribeToSaveResponse(this.blogCategoryService.update(blogCategory));
    }
  }

  protected subscribeToSaveResponse(result: Observable<IBlogCategory | null>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.previousState();
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }

  protected onSaveFinalize(): void {
    this.isSaving.set(false);
  }

  protected updateForm(blogCategory: IBlogCategory): void {
    this.blogCategory = blogCategory;
    this.blogCategoryFormService.resetForm(this.editForm, blogCategory);

    this.blogCategoriesSharedCollection.update(blogCategories =>
      this.blogCategoryService.addBlogCategoryToCollectionIfMissing<IBlogCategory>(blogCategories, blogCategory.parent),
    );
    this.blogPostsSharedCollection.update(blogPosts =>
      this.blogPostService.addBlogPostToCollectionIfMissing<IBlogPost>(blogPosts, ...(blogCategory.blogPosts ?? [])),
    );
  }

  protected loadRelationshipsOptions(): void {
    this.blogCategoryService
      .query()
      .pipe(map((res: HttpResponse<IBlogCategory[]>) => res.body ?? []))
      .pipe(
        map((blogCategories: IBlogCategory[]) =>
          this.blogCategoryService.addBlogCategoryToCollectionIfMissing<IBlogCategory>(blogCategories, this.blogCategory?.parent),
        ),
      )
      .subscribe((blogCategories: IBlogCategory[]) => this.blogCategoriesSharedCollection.set(blogCategories));

    this.blogPostService
      .query()
      .pipe(map((res: HttpResponse<IBlogPost[]>) => res.body ?? []))
      .pipe(
        map((blogPosts: IBlogPost[]) =>
          this.blogPostService.addBlogPostToCollectionIfMissing<IBlogPost>(blogPosts, ...(this.blogCategory?.blogPosts ?? [])),
        ),
      )
      .subscribe((blogPosts: IBlogPost[]) => this.blogPostsSharedCollection.set(blogPosts));
  }
}
