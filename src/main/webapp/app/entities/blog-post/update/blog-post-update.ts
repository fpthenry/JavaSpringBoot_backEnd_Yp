import { HttpResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap/tooltip';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable, finalize, map } from 'rxjs';

import { DataUtils, EventManager, EventWithContent, FileLoadError } from 'app/core/util';
import { IBlogCategory } from 'app/entities/blog-category/blog-category.model';
import { BlogCategoryService } from 'app/entities/blog-category/service/blog-category.service';
import { TagService } from 'app/entities/tag/service/tag.service';
import { ITag } from 'app/entities/tag/tag.model';
import { AlertError, AlertErrorModel } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { IBlogPost } from '../blog-post.model';
import { BlogPostService } from '../service/blog-post.service';

import { BlogPostFormGroup, BlogPostFormService } from './blog-post-form.service';

@Component({
  selector: 'jhi-blog-post-update',
  templateUrl: './blog-post-update.html',
  imports: [TranslateDirective, TranslatePipe, FontAwesomeModule, AlertError, ReactiveFormsModule, NgbTooltip],
})
export class BlogPostUpdate implements OnInit {
  readonly isSaving = signal(false);
  blogPost: IBlogPost | null = null;

  blogCategoriesSharedCollection = signal<IBlogCategory[]>([]);
  tagsSharedCollection = signal<ITag[]>([]);

  protected dataUtils = inject(DataUtils);
  protected eventManager = inject(EventManager);
  protected blogPostService = inject(BlogPostService);
  protected blogPostFormService = inject(BlogPostFormService);
  protected blogCategoryService = inject(BlogCategoryService);
  protected tagService = inject(TagService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: BlogPostFormGroup = this.blogPostFormService.createBlogPostFormGroup();

  compareBlogCategory = (o1: IBlogCategory | null, o2: IBlogCategory | null): boolean =>
    this.blogCategoryService.compareBlogCategory(o1, o2);

  compareTag = (o1: ITag | null, o2: ITag | null): boolean => this.tagService.compareTag(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ blogPost }) => {
      this.blogPost = blogPost;
      if (blogPost) {
        this.updateForm(blogPost);
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
    const blogPost = this.blogPostFormService.getBlogPost(this.editForm);
    if (blogPost.id === null) {
      this.subscribeToSaveResponse(this.blogPostService.create(blogPost));
    } else {
      this.subscribeToSaveResponse(this.blogPostService.update(blogPost));
    }
  }

  protected subscribeToSaveResponse(result: Observable<IBlogPost | null>): void {
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

  protected updateForm(blogPost: IBlogPost): void {
    this.blogPost = blogPost;
    this.blogPostFormService.resetForm(this.editForm, blogPost);

    this.blogCategoriesSharedCollection.update(blogCategories =>
      this.blogCategoryService.addBlogCategoryToCollectionIfMissing<IBlogCategory>(blogCategories, ...(blogPost.categories ?? [])),
    );
    this.tagsSharedCollection.update(tags => this.tagService.addTagToCollectionIfMissing<ITag>(tags, ...(blogPost.tags ?? [])));
  }

  protected loadRelationshipsOptions(): void {
    this.blogCategoryService
      .query()
      .pipe(map((res: HttpResponse<IBlogCategory[]>) => res.body ?? []))
      .pipe(
        map((blogCategories: IBlogCategory[]) =>
          this.blogCategoryService.addBlogCategoryToCollectionIfMissing<IBlogCategory>(
            blogCategories,
            ...(this.blogPost?.categories ?? []),
          ),
        ),
      )
      .subscribe((blogCategories: IBlogCategory[]) => this.blogCategoriesSharedCollection.set(blogCategories));

    this.tagService
      .query()
      .pipe(map((res: HttpResponse<ITag[]>) => res.body ?? []))
      .pipe(map((tags: ITag[]) => this.tagService.addTagToCollectionIfMissing<ITag>(tags, ...(this.blogPost?.tags ?? []))))
      .subscribe((tags: ITag[]) => this.tagsSharedCollection.set(tags));
  }
}
