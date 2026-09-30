import { HttpResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Observable, finalize, map } from 'rxjs';

import { DataUtils, EventManager, EventWithContent, FileLoadError } from 'app/core/util';
import { IArticle } from 'app/entities/article/article.model';
import { ArticleService } from 'app/entities/article/service/article.service';
import { AlertError, AlertErrorModel } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { IArticleCategory } from '../article-category.model';
import { ArticleCategoryService } from '../service/article-category.service';

import { ArticleCategoryFormGroup, ArticleCategoryFormService } from './article-category-form.service';

@Component({
  selector: 'jhi-article-category-update',
  templateUrl: './article-category-update.html',
  imports: [TranslateDirective, FontAwesomeModule, AlertError, ReactiveFormsModule],
})
export class ArticleCategoryUpdate implements OnInit {
  readonly isSaving = signal(false);
  articleCategory: IArticleCategory | null = null;

  articleCategoriesSharedCollection = signal<IArticleCategory[]>([]);
  articlesSharedCollection = signal<IArticle[]>([]);

  protected dataUtils = inject(DataUtils);
  protected eventManager = inject(EventManager);
  protected articleCategoryService = inject(ArticleCategoryService);
  protected articleCategoryFormService = inject(ArticleCategoryFormService);
  protected articleService = inject(ArticleService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: ArticleCategoryFormGroup = this.articleCategoryFormService.createArticleCategoryFormGroup();

  compareArticleCategory = (o1: IArticleCategory | null, o2: IArticleCategory | null): boolean =>
    this.articleCategoryService.compareArticleCategory(o1, o2);

  compareArticle = (o1: IArticle | null, o2: IArticle | null): boolean => this.articleService.compareArticle(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ articleCategory }) => {
      this.articleCategory = articleCategory;
      if (articleCategory) {
        this.updateForm(articleCategory);
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
    const articleCategory = this.articleCategoryFormService.getArticleCategory(this.editForm);
    if (articleCategory.id === null) {
      this.subscribeToSaveResponse(this.articleCategoryService.create(articleCategory));
    } else {
      this.subscribeToSaveResponse(this.articleCategoryService.update(articleCategory));
    }
  }

  protected subscribeToSaveResponse(result: Observable<IArticleCategory | null>): void {
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

  protected updateForm(articleCategory: IArticleCategory): void {
    this.articleCategory = articleCategory;
    this.articleCategoryFormService.resetForm(this.editForm, articleCategory);

    this.articleCategoriesSharedCollection.update(articleCategories =>
      this.articleCategoryService.addArticleCategoryToCollectionIfMissing<IArticleCategory>(articleCategories, articleCategory.parent),
    );
    this.articlesSharedCollection.update(articles =>
      this.articleService.addArticleToCollectionIfMissing<IArticle>(articles, ...(articleCategory.articleses ?? [])),
    );
  }

  protected loadRelationshipsOptions(): void {
    this.articleCategoryService
      .query()
      .pipe(map((res: HttpResponse<IArticleCategory[]>) => res.body ?? []))
      .pipe(
        map((articleCategories: IArticleCategory[]) =>
          this.articleCategoryService.addArticleCategoryToCollectionIfMissing<IArticleCategory>(
            articleCategories,
            this.articleCategory?.parent,
          ),
        ),
      )
      .subscribe((articleCategories: IArticleCategory[]) => this.articleCategoriesSharedCollection.set(articleCategories));

    this.articleService
      .query()
      .pipe(map((res: HttpResponse<IArticle[]>) => res.body ?? []))
      .pipe(
        map((articles: IArticle[]) =>
          this.articleService.addArticleToCollectionIfMissing<IArticle>(articles, ...(this.articleCategory?.articleses ?? [])),
        ),
      )
      .subscribe((articles: IArticle[]) => this.articlesSharedCollection.set(articles));
  }
}
