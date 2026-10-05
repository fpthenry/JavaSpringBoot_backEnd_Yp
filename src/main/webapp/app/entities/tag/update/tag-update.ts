import { HttpResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap/tooltip';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable, finalize, map } from 'rxjs';

import { IBlogPost } from 'app/entities/blog-post/blog-post.model';
import { BlogPostService } from 'app/entities/blog-post/service/blog-post.service';
import { AlertError } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { TagService } from '../service/tag.service';
import { ITag } from '../tag.model';

import { TagFormGroup, TagFormService } from './tag-form.service';

@Component({
  selector: 'jhi-tag-update',
  templateUrl: './tag-update.html',
  imports: [TranslateDirective, TranslatePipe, FontAwesomeModule, AlertError, ReactiveFormsModule, NgbTooltip],
})
export class TagUpdate implements OnInit {
  readonly isSaving = signal(false);
  tag: ITag | null = null;

  blogPostsSharedCollection = signal<IBlogPost[]>([]);

  protected tagService = inject(TagService);
  protected tagFormService = inject(TagFormService);
  protected blogPostService = inject(BlogPostService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: TagFormGroup = this.tagFormService.createTagFormGroup();

  compareBlogPost = (o1: IBlogPost | null, o2: IBlogPost | null): boolean => this.blogPostService.compareBlogPost(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ tag }) => {
      this.tag = tag;
      if (tag) {
        this.updateForm(tag);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    globalThis.history.back();
  }

  save(): void {
    this.isSaving.set(true);
    const tag = this.tagFormService.getTag(this.editForm);
    if (tag.id === null) {
      this.subscribeToSaveResponse(this.tagService.create(tag));
    } else {
      this.subscribeToSaveResponse(this.tagService.update(tag));
    }
  }

  protected subscribeToSaveResponse(result: Observable<ITag | null>): void {
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

  protected updateForm(tag: ITag): void {
    this.tag = tag;
    this.tagFormService.resetForm(this.editForm, tag);

    this.blogPostsSharedCollection.update(blogPosts =>
      this.blogPostService.addBlogPostToCollectionIfMissing<IBlogPost>(blogPosts, ...(tag.blogPosts ?? [])),
    );
  }

  protected loadRelationshipsOptions(): void {
    this.blogPostService
      .query()
      .pipe(map((res: HttpResponse<IBlogPost[]>) => res.body ?? []))
      .pipe(
        map((blogPosts: IBlogPost[]) =>
          this.blogPostService.addBlogPostToCollectionIfMissing<IBlogPost>(blogPosts, ...(this.tag?.blogPosts ?? [])),
        ),
      )
      .subscribe((blogPosts: IBlogPost[]) => this.blogPostsSharedCollection.set(blogPosts));
  }
}
