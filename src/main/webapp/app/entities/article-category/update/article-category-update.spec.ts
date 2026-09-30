import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpResponse } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { Subject, from, of } from 'rxjs';

import { IArticle } from 'app/entities/article/article.model';
import { ArticleService } from 'app/entities/article/service/article.service';
import { IArticleCategory } from '../article-category.model';
import { ArticleCategoryService } from '../service/article-category.service';

import { ArticleCategoryFormService } from './article-category-form.service';
import { ArticleCategoryUpdate } from './article-category-update';

describe('ArticleCategory Management Update Component', () => {
  let comp: ArticleCategoryUpdate;
  let fixture: ComponentFixture<ArticleCategoryUpdate>;
  let activatedRoute: ActivatedRoute;
  let articleCategoryFormService: ArticleCategoryFormService;
  let articleCategoryService: ArticleCategoryService;
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

    fixture = TestBed.createComponent(ArticleCategoryUpdate);
    activatedRoute = TestBed.inject(ActivatedRoute);
    articleCategoryFormService = TestBed.inject(ArticleCategoryFormService);
    articleCategoryService = TestBed.inject(ArticleCategoryService);
    articleService = TestBed.inject(ArticleService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call ArticleCategory query and add missing value', () => {
      const articleCategory: IArticleCategory = { id: 17997 };
      const parent: IArticleCategory = { id: 11547 };
      articleCategory.parent = parent;

      const articleCategoryCollection: IArticleCategory[] = [{ id: 11547 }];
      vi.spyOn(articleCategoryService, 'query').mockReturnValue(of(new HttpResponse({ body: articleCategoryCollection })));
      const additionalArticleCategories = [parent];
      const expectedCollection: IArticleCategory[] = [...additionalArticleCategories, ...articleCategoryCollection];
      vi.spyOn(articleCategoryService, 'addArticleCategoryToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ articleCategory });
      comp.ngOnInit();

      expect(articleCategoryService.query).toHaveBeenCalled();
      expect(articleCategoryService.addArticleCategoryToCollectionIfMissing).toHaveBeenCalledWith(
        articleCategoryCollection,
        ...additionalArticleCategories.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.articleCategoriesSharedCollection()).toEqual(expectedCollection);
    });

    it('should call Article query and add missing value', () => {
      const articleCategory: IArticleCategory = { id: 17997 };
      const articleses: IArticle[] = [{ id: 24128 }];
      articleCategory.articleses = articleses;

      const articleCollection: IArticle[] = [{ id: 24128 }];
      vi.spyOn(articleService, 'query').mockReturnValue(of(new HttpResponse({ body: articleCollection })));
      const additionalArticles = [...articleses];
      const expectedCollection: IArticle[] = [...additionalArticles, ...articleCollection];
      vi.spyOn(articleService, 'addArticleToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ articleCategory });
      comp.ngOnInit();

      expect(articleService.query).toHaveBeenCalled();
      expect(articleService.addArticleToCollectionIfMissing).toHaveBeenCalledWith(
        articleCollection,
        ...additionalArticles.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.articlesSharedCollection()).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const articleCategory: IArticleCategory = { id: 17997 };
      const parent: IArticleCategory = { id: 11547 };
      articleCategory.parent = parent;
      const articles: IArticle = { id: 24128 };
      articleCategory.articleses = [articles];

      activatedRoute.data = of({ articleCategory });
      comp.ngOnInit();

      expect(comp.articleCategoriesSharedCollection()).toContainEqual(parent);
      expect(comp.articlesSharedCollection()).toContainEqual(articles);
      expect(comp.articleCategory).toEqual(articleCategory);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<IArticleCategory>();
      const articleCategory = { id: 11547 };
      vi.spyOn(articleCategoryFormService, 'getArticleCategory').mockReturnValue(articleCategory);
      vi.spyOn(articleCategoryService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ articleCategory });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(articleCategory);
      saveSubject.complete();

      // THEN
      expect(articleCategoryFormService.getArticleCategory).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(articleCategoryService.update).toHaveBeenCalledWith(expect.objectContaining(articleCategory));
      expect(comp.isSaving()).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<IArticleCategory>();
      const articleCategory = { id: 11547 };
      vi.spyOn(articleCategoryFormService, 'getArticleCategory').mockReturnValue({ id: null });
      vi.spyOn(articleCategoryService, 'create').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ articleCategory: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(articleCategory);
      saveSubject.complete();

      // THEN
      expect(articleCategoryFormService.getArticleCategory).toHaveBeenCalled();
      expect(articleCategoryService.create).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<IArticleCategory>();
      const articleCategory = { id: 11547 };
      vi.spyOn(articleCategoryService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ articleCategory });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(articleCategoryService.update).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareArticleCategory', () => {
      it('should forward to articleCategoryService', () => {
        const entity = { id: 11547 };
        const entity2 = { id: 17997 };
        vi.spyOn(articleCategoryService, 'compareArticleCategory');
        comp.compareArticleCategory(entity, entity2);
        expect(articleCategoryService.compareArticleCategory).toHaveBeenCalledWith(entity, entity2);
      });
    });

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
