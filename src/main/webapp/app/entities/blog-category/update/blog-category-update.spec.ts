import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpResponse } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { Subject, from, of } from 'rxjs';

import { IBlogPost } from 'app/entities/blog-post/blog-post.model';
import { BlogPostService } from 'app/entities/blog-post/service/blog-post.service';
import { IBlogCategory } from '../blog-category.model';
import { BlogCategoryService } from '../service/blog-category.service';

import { BlogCategoryFormService } from './blog-category-form.service';
import { BlogCategoryUpdate } from './blog-category-update';

describe('BlogCategory Management Update Component', () => {
  let comp: BlogCategoryUpdate;
  let fixture: ComponentFixture<BlogCategoryUpdate>;
  let activatedRoute: ActivatedRoute;
  let blogCategoryFormService: BlogCategoryFormService;
  let blogCategoryService: BlogCategoryService;
  let blogPostService: BlogPostService;

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

    fixture = TestBed.createComponent(BlogCategoryUpdate);
    activatedRoute = TestBed.inject(ActivatedRoute);
    blogCategoryFormService = TestBed.inject(BlogCategoryFormService);
    blogCategoryService = TestBed.inject(BlogCategoryService);
    blogPostService = TestBed.inject(BlogPostService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call BlogCategory query and add missing value', () => {
      const blogCategory: IBlogCategory = { id: 21032 };
      const parent: IBlogCategory = { id: 27812 };
      blogCategory.parent = parent;

      const blogCategoryCollection: IBlogCategory[] = [{ id: 27812 }];
      vi.spyOn(blogCategoryService, 'query').mockReturnValue(of(new HttpResponse({ body: blogCategoryCollection })));
      const additionalBlogCategories = [parent];
      const expectedCollection: IBlogCategory[] = [...additionalBlogCategories, ...blogCategoryCollection];
      vi.spyOn(blogCategoryService, 'addBlogCategoryToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ blogCategory });
      comp.ngOnInit();

      expect(blogCategoryService.query).toHaveBeenCalled();
      expect(blogCategoryService.addBlogCategoryToCollectionIfMissing).toHaveBeenCalledWith(
        blogCategoryCollection,
        ...additionalBlogCategories.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.blogCategoriesSharedCollection()).toEqual(expectedCollection);
    });

    it('should call BlogPost query and add missing value', () => {
      const blogCategory: IBlogCategory = { id: 21032 };
      const blogPosts: IBlogPost[] = [{ id: 11641 }];
      blogCategory.blogPosts = blogPosts;

      const blogPostCollection: IBlogPost[] = [{ id: 11641 }];
      vi.spyOn(blogPostService, 'query').mockReturnValue(of(new HttpResponse({ body: blogPostCollection })));
      const additionalBlogPosts = [...blogPosts];
      const expectedCollection: IBlogPost[] = [...additionalBlogPosts, ...blogPostCollection];
      vi.spyOn(blogPostService, 'addBlogPostToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ blogCategory });
      comp.ngOnInit();

      expect(blogPostService.query).toHaveBeenCalled();
      expect(blogPostService.addBlogPostToCollectionIfMissing).toHaveBeenCalledWith(
        blogPostCollection,
        ...additionalBlogPosts.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.blogPostsSharedCollection()).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const blogCategory: IBlogCategory = { id: 21032 };
      const parent: IBlogCategory = { id: 27812 };
      blogCategory.parent = parent;
      const blogPost: IBlogPost = { id: 11641 };
      blogCategory.blogPosts = [blogPost];

      activatedRoute.data = of({ blogCategory });
      comp.ngOnInit();

      expect(comp.blogCategoriesSharedCollection()).toContainEqual(parent);
      expect(comp.blogPostsSharedCollection()).toContainEqual(blogPost);
      expect(comp.blogCategory).toEqual(blogCategory);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<IBlogCategory>();
      const blogCategory = { id: 27812 };
      vi.spyOn(blogCategoryFormService, 'getBlogCategory').mockReturnValue(blogCategory);
      vi.spyOn(blogCategoryService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ blogCategory });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(blogCategory);
      saveSubject.complete();

      // THEN
      expect(blogCategoryFormService.getBlogCategory).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(blogCategoryService.update).toHaveBeenCalledWith(expect.objectContaining(blogCategory));
      expect(comp.isSaving()).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<IBlogCategory>();
      const blogCategory = { id: 27812 };
      vi.spyOn(blogCategoryFormService, 'getBlogCategory').mockReturnValue({ id: null });
      vi.spyOn(blogCategoryService, 'create').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ blogCategory: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(blogCategory);
      saveSubject.complete();

      // THEN
      expect(blogCategoryFormService.getBlogCategory).toHaveBeenCalled();
      expect(blogCategoryService.create).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<IBlogCategory>();
      const blogCategory = { id: 27812 };
      vi.spyOn(blogCategoryService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ blogCategory });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(blogCategoryService.update).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareBlogCategory', () => {
      it('should forward to blogCategoryService', () => {
        const entity = { id: 27812 };
        const entity2 = { id: 21032 };
        vi.spyOn(blogCategoryService, 'compareBlogCategory');
        comp.compareBlogCategory(entity, entity2);
        expect(blogCategoryService.compareBlogCategory).toHaveBeenCalledWith(entity, entity2);
      });
    });

    describe('compareBlogPost', () => {
      it('should forward to blogPostService', () => {
        const entity = { id: 11641 };
        const entity2 = { id: 15145 };
        vi.spyOn(blogPostService, 'compareBlogPost');
        comp.compareBlogPost(entity, entity2);
        expect(blogPostService.compareBlogPost).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
