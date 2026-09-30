import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpResponse } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { Subject, from, of } from 'rxjs';

import { IArticle } from 'app/entities/article/article.model';
import { ArticleService } from 'app/entities/article/service/article.service';
import { IArticleTag } from '../article-tag.model';
import { ArticleTagService } from '../service/article-tag.service';

import { ArticleTagFormService } from './article-tag-form.service';
import { ArticleTagUpdate } from './article-tag-update';

describe('ArticleTag Management Update Component', () => {
  let comp: ArticleTagUpdate;
  let fixture: ComponentFixture<ArticleTagUpdate>;
  let activatedRoute: ActivatedRoute;
  let articleTagFormService: ArticleTagFormService;
  let articleTagService: ArticleTagService;
  let articleService: ArticleService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            params: from([{}]),
          },
        },
      ],
    });

    fixture = TestBed.createComponent(ArticleTagUpdate);
    activatedRoute = TestBed.inject(ActivatedRoute);
    articleTagFormService = TestBed.inject(ArticleTagFormService);
    articleTagService = TestBed.inject(ArticleTagService);
    articleService = TestBed.inject(ArticleService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call Article query and add missing value', () => {
      const articleTag: IArticleTag = { id: 25497 };
      const articleses: IArticle[] = [{ id: 24128 }];
      articleTag.articleses = articleses;

      const articleCollection: IArticle[] = [{ id: 24128 }];
      vi.spyOn(articleService, 'query').mockReturnValue(of(new HttpResponse({ body: articleCollection })));
      const additionalArticles = [...articleses];
      const expectedCollection: IArticle[] = [...additionalArticles, ...articleCollection];
      vi.spyOn(articleService, 'addArticleToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ articleTag });
      comp.ngOnInit();

      expect(articleService.query).toHaveBeenCalled();
      expect(articleService.addArticleToCollectionIfMissing).toHaveBeenCalledWith(
        articleCollection,
        ...additionalArticles.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.articlesSharedCollection()).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const articleTag: IArticleTag = { id: 25497 };
      const articles: IArticle = { id: 24128 };
      articleTag.articleses = [articles];

      activatedRoute.data = of({ articleTag });
      comp.ngOnInit();

      expect(comp.articlesSharedCollection()).toContainEqual(articles);
      expect(comp.articleTag).toEqual(articleTag);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<IArticleTag>();
      const articleTag = { id: 22923 };
      vi.spyOn(articleTagFormService, 'getArticleTag').mockReturnValue(articleTag);
      vi.spyOn(articleTagService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ articleTag });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(articleTag);
      saveSubject.complete();

      // THEN
      expect(articleTagFormService.getArticleTag).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(articleTagService.update).toHaveBeenCalledWith(expect.objectContaining(articleTag));
      expect(comp.isSaving()).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<IArticleTag>();
      const articleTag = { id: 22923 };
      vi.spyOn(articleTagFormService, 'getArticleTag').mockReturnValue({ id: null });
      vi.spyOn(articleTagService, 'create').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ articleTag: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(articleTag);
      saveSubject.complete();

      // THEN
      expect(articleTagFormService.getArticleTag).toHaveBeenCalled();
      expect(articleTagService.create).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<IArticleTag>();
      const articleTag = { id: 22923 };
      vi.spyOn(articleTagService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ articleTag });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(articleTagService.update).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareArticle', () => {
      it('should forward to articleService', () => {
        const entity = { id: 24128 };
        const entity2 = { id: 30377 };
        vi.spyOn(articleService, 'compareArticle');
        comp.compareArticle(entity, entity2);
        expect(articleService.compareArticle).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
