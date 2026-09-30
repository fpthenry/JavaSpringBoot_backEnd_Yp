import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpResponse } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { Subject, from, of } from 'rxjs';

import { IArticleCategory } from 'app/entities/article-category/article-category.model';
import { ArticleCategoryService } from 'app/entities/article-category/service/article-category.service';
import { IArticleTag } from 'app/entities/article-tag/article-tag.model';
import { ArticleTagService } from 'app/entities/article-tag/service/article-tag.service';
import { UserService } from 'app/entities/user/service/user.service';
import { IUser } from 'app/entities/user/user.model';
import { IArticle } from '../article.model';
import { ArticleService } from '../service/article.service';

import { ArticleFormService } from './article-form.service';
import { ArticleUpdate } from './article-update';

describe('Article Management Update Component', () => {
  let comp: ArticleUpdate;
  let fixture: ComponentFixture<ArticleUpdate>;
  let activatedRoute: ActivatedRoute;
  let articleFormService: ArticleFormService;
  let articleService: ArticleService;
  let userService: UserService;
  let articleCategoryService: ArticleCategoryService;
  let articleTagService: ArticleTagService;

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

    fixture = TestBed.createComponent(ArticleUpdate);
    activatedRoute = TestBed.inject(ActivatedRoute);
    articleFormService = TestBed.inject(ArticleFormService);
    articleService = TestBed.inject(ArticleService);
    userService = TestBed.inject(UserService);
    articleCategoryService = TestBed.inject(ArticleCategoryService);
    articleTagService = TestBed.inject(ArticleTagService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call User query and add missing value', () => {
      const article: IArticle = { id: 30377 };
      const author: IUser = { id: 3944 };
      article.author = author;

      const userCollection: IUser[] = [{ id: 3944 }];
      vi.spyOn(userService, 'query').mockReturnValue(of(new HttpResponse({ body: userCollection })));
      const additionalUsers = [author];
      const expectedCollection: IUser[] = [...additionalUsers, ...userCollection];
      vi.spyOn(userService, 'addUserToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ article });
      comp.ngOnInit();

      expect(userService.query).toHaveBeenCalled();
      expect(userService.addUserToCollectionIfMissing).toHaveBeenCalledWith(
        userCollection,
        ...additionalUsers.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.usersSharedCollection()).toEqual(expectedCollection);
    });

    it('should call ArticleCategory query and add missing value', () => {
      const article: IArticle = { id: 30377 };
      const categorieses: IArticleCategory[] = [{ id: 11547 }];
      article.categorieses = categorieses;

      const articleCategoryCollection: IArticleCategory[] = [{ id: 11547 }];
      vi.spyOn(articleCategoryService, 'query').mockReturnValue(of(new HttpResponse({ body: articleCategoryCollection })));
      const additionalArticleCategories = [...categorieses];
      const expectedCollection: IArticleCategory[] = [...additionalArticleCategories, ...articleCategoryCollection];
      vi.spyOn(articleCategoryService, 'addArticleCategoryToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ article });
      comp.ngOnInit();

      expect(articleCategoryService.query).toHaveBeenCalled();
      expect(articleCategoryService.addArticleCategoryToCollectionIfMissing).toHaveBeenCalledWith(
        articleCategoryCollection,
        ...additionalArticleCategories.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.articleCategoriesSharedCollection()).toEqual(expectedCollection);
    });

    it('should call ArticleTag query and add missing value', () => {
      const article: IArticle = { id: 30377 };
      const tagses: IArticleTag[] = [{ id: 22923 }];
      article.tagses = tagses;

      const articleTagCollection: IArticleTag[] = [{ id: 22923 }];
      vi.spyOn(articleTagService, 'query').mockReturnValue(of(new HttpResponse({ body: articleTagCollection })));
      const additionalArticleTags = [...tagses];
      const expectedCollection: IArticleTag[] = [...additionalArticleTags, ...articleTagCollection];
      vi.spyOn(articleTagService, 'addArticleTagToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ article });
      comp.ngOnInit();

      expect(articleTagService.query).toHaveBeenCalled();
      expect(articleTagService.addArticleTagToCollectionIfMissing).toHaveBeenCalledWith(
        articleTagCollection,
        ...additionalArticleTags.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.articleTagsSharedCollection()).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const article: IArticle = { id: 30377 };
      const author: IUser = { id: 3944 };
      article.author = author;
      const categories: IArticleCategory = { id: 11547 };
      article.categorieses = [categories];
      const tags: IArticleTag = { id: 22923 };
      article.tagses = [tags];

      activatedRoute.data = of({ article });
      comp.ngOnInit();

      expect(comp.usersSharedCollection()).toContainEqual(author);
      expect(comp.articleCategoriesSharedCollection()).toContainEqual(categories);
      expect(comp.articleTagsSharedCollection()).toContainEqual(tags);
      expect(comp.article).toEqual(article);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<IArticle>();
      const article = { id: 24128 };
      vi.spyOn(articleFormService, 'getArticle').mockReturnValue(article);
      vi.spyOn(articleService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ article });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(article);
      saveSubject.complete();

      // THEN
      expect(articleFormService.getArticle).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(articleService.update).toHaveBeenCalledWith(expect.objectContaining(article));
      expect(comp.isSaving()).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<IArticle>();
      const article = { id: 24128 };
      vi.spyOn(articleFormService, 'getArticle').mockReturnValue({ id: null });
      vi.spyOn(articleService, 'create').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ article: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(article);
      saveSubject.complete();

      // THEN
      expect(articleFormService.getArticle).toHaveBeenCalled();
      expect(articleService.create).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<IArticle>();
      const article = { id: 24128 };
      vi.spyOn(articleService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ article });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(articleService.update).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareUser', () => {
      it('should forward to userService', () => {
        const entity = { id: 3944 };
        const entity2 = { id: 6275 };
        vi.spyOn(userService, 'compareUser');
        comp.compareUser(entity, entity2);
        expect(userService.compareUser).toHaveBeenCalledWith(entity, entity2);
      });
    });

    describe('compareArticleCategory', () => {
      it('should forward to articleCategoryService', () => {
        const entity = { id: 11547 };
        const entity2 = { id: 17997 };
        vi.spyOn(articleCategoryService, 'compareArticleCategory');
        comp.compareArticleCategory(entity, entity2);
        expect(articleCategoryService.compareArticleCategory).toHaveBeenCalledWith(entity, entity2);
      });
    });

    describe('compareArticleTag', () => {
      it('should forward to articleTagService', () => {
        const entity = { id: 22923 };
        const entity2 = { id: 25497 };
        vi.spyOn(articleTagService, 'compareArticleTag');
        comp.compareArticleTag(entity, entity2);
        expect(articleTagService.compareArticleTag).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
