import { HttpResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Observable, finalize, map } from 'rxjs';

import { DataUtils, EventManager, EventWithContent, FileLoadError } from 'app/core/util';
import { IArticleCategory } from 'app/entities/article-category/article-category.model';
import { ArticleCategoryService } from 'app/entities/article-category/service/article-category.service';
import { IArticleTag } from 'app/entities/article-tag/article-tag.model';
import { ArticleTagService } from 'app/entities/article-tag/service/article-tag.service';
import { UserService } from 'app/entities/user/service/user.service';
import { IUser } from 'app/entities/user/user.model';
import { AlertError, AlertErrorModel } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { IArticle } from '../article.model';
import { ArticleService } from '../service/article.service';

import { ArticleFormGroup, ArticleFormService } from './article-form.service';

@Component({
  selector: 'jhi-article-update',
  templateUrl: './article-update.html',
  imports: [TranslateDirective, FontAwesomeModule, AlertError, ReactiveFormsModule],
})
export class ArticleUpdate implements OnInit {
  readonly isSaving = signal(false);
  article: IArticle | null = null;

  usersSharedCollection = signal<IUser[]>([]);
  articleCategoriesSharedCollection = signal<IArticleCategory[]>([]);
  articleTagsSharedCollection = signal<IArticleTag[]>([]);

  protected dataUtils = inject(DataUtils);
  protected eventManager = inject(EventManager);
  protected articleService = inject(ArticleService);
  protected articleFormService = inject(ArticleFormService);
  protected userService = inject(UserService);
  protected articleCategoryService = inject(ArticleCategoryService);
  protected articleTagService = inject(ArticleTagService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: ArticleFormGroup = this.articleFormService.createArticleFormGroup();

  compareUser = (o1: IUser | null, o2: IUser | null): boolean => this.userService.compareUser(o1, o2);

  compareArticleCategory = (o1: IArticleCategory | null, o2: IArticleCategory | null): boolean =>
    this.articleCategoryService.compareArticleCategory(o1, o2);

  compareArticleTag = (o1: IArticleTag | null, o2: IArticleTag | null): boolean => this.articleTagService.compareArticleTag(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ article }) => {
      this.article = article;
      if (article) {
        this.updateForm(article);
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
    const article = this.articleFormService.getArticle(this.editForm);
    if (article.id === null) {
      this.subscribeToSaveResponse(this.articleService.create(article));
    } else {
      this.subscribeToSaveResponse(this.articleService.update(article));
    }
  }

  protected subscribeToSaveResponse(result: Observable<IArticle | null>): void {
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

  protected updateForm(article: IArticle): void {
    this.article = article;
    this.articleFormService.resetForm(this.editForm, article);

    this.usersSharedCollection.update(users => this.userService.addUserToCollectionIfMissing<IUser>(users, article.author));
    this.articleCategoriesSharedCollection.update(articleCategories =>
      this.articleCategoryService.addArticleCategoryToCollectionIfMissing<IArticleCategory>(
        articleCategories,
        ...(article.categorieses ?? []),
      ),
    );
    this.articleTagsSharedCollection.update(articleTags =>
      this.articleTagService.addArticleTagToCollectionIfMissing<IArticleTag>(articleTags, ...(article.tagses ?? [])),
    );
  }

  protected loadRelationshipsOptions(): void {
    this.userService
      .query()
      .pipe(map((res: HttpResponse<IUser[]>) => res.body ?? []))
      .pipe(map((users: IUser[]) => this.userService.addUserToCollectionIfMissing<IUser>(users, this.article?.author)))
      .subscribe((users: IUser[]) => this.usersSharedCollection.set(users));

    this.articleCategoryService
      .query()
      .pipe(map((res: HttpResponse<IArticleCategory[]>) => res.body ?? []))
      .pipe(
        map((articleCategories: IArticleCategory[]) =>
          this.articleCategoryService.addArticleCategoryToCollectionIfMissing<IArticleCategory>(
            articleCategories,
            ...(this.article?.categorieses ?? []),
          ),
        ),
      )
      .subscribe((articleCategories: IArticleCategory[]) => this.articleCategoriesSharedCollection.set(articleCategories));

    this.articleTagService
      .query()
      .pipe(map((res: HttpResponse<IArticleTag[]>) => res.body ?? []))
      .pipe(
        map((articleTags: IArticleTag[]) =>
          this.articleTagService.addArticleTagToCollectionIfMissing<IArticleTag>(articleTags, ...(this.article?.tagses ?? [])),
        ),
      )
      .subscribe((articleTags: IArticleTag[]) => this.articleTagsSharedCollection.set(articleTags));
  }
}
