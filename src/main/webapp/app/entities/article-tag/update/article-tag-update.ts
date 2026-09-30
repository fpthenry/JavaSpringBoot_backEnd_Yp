import { HttpResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Observable, finalize, map } from 'rxjs';

import { IArticle } from 'app/entities/article/article.model';
import { ArticleService } from 'app/entities/article/service/article.service';
import { AlertError } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { IArticleTag } from '../article-tag.model';
import { ArticleTagService } from '../service/article-tag.service';

import { ArticleTagFormGroup, ArticleTagFormService } from './article-tag-form.service';

@Component({
  selector: 'jhi-article-tag-update',
  templateUrl: './article-tag-update.html',
  imports: [TranslateDirective, FontAwesomeModule, AlertError, ReactiveFormsModule],
})
export class ArticleTagUpdate implements OnInit {
  readonly isSaving = signal(false);
  articleTag: IArticleTag | null = null;

  articlesSharedCollection = signal<IArticle[]>([]);

  protected articleTagService = inject(ArticleTagService);
  protected articleTagFormService = inject(ArticleTagFormService);
  protected articleService = inject(ArticleService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: ArticleTagFormGroup = this.articleTagFormService.createArticleTagFormGroup();

  compareArticle = (o1: IArticle | null, o2: IArticle | null): boolean => this.articleService.compareArticle(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ articleTag }) => {
      this.articleTag = articleTag;
      if (articleTag) {
        this.updateForm(articleTag);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    globalThis.history.back();
  }

  save(): void {
    this.isSaving.set(true);
    const articleTag = this.articleTagFormService.getArticleTag(this.editForm);
    if (articleTag.id === null) {
      this.subscribeToSaveResponse(this.articleTagService.create(articleTag));
    } else {
      this.subscribeToSaveResponse(this.articleTagService.update(articleTag));
    }
  }

  protected subscribeToSaveResponse(result: Observable<IArticleTag | null>): void {
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

  protected updateForm(articleTag: IArticleTag): void {
    this.articleTag = articleTag;
    this.articleTagFormService.resetForm(this.editForm, articleTag);

    this.articlesSharedCollection.update(articles =>
      this.articleService.addArticleToCollectionIfMissing<IArticle>(articles, ...(articleTag.articleses ?? [])),
    );
  }

  protected loadRelationshipsOptions(): void {
    this.articleService
      .query()
      .pipe(map((res: HttpResponse<IArticle[]>) => res.body ?? []))
      .pipe(
        map((articles: IArticle[]) =>
          this.articleService.addArticleToCollectionIfMissing<IArticle>(articles, ...(this.articleTag?.articleses ?? [])),
        ),
      )
      .subscribe((articles: IArticle[]) => this.articlesSharedCollection.set(articles));
  }
}
