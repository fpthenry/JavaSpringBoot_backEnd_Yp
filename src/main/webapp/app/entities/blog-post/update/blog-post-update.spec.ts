import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpResponse } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { Subject, from, of } from 'rxjs';

import { IBlogCategory } from 'app/entities/blog-category/blog-category.model';
import { BlogCategoryService } from 'app/entities/blog-category/service/blog-category.service';
import { TagService } from 'app/entities/tag/service/tag.service';
import { ITag } from 'app/entities/tag/tag.model';
import { IBlogPost } from '../blog-post.model';
import { BlogPostService } from '../service/blog-post.service';

import { BlogPostFormService } from './blog-post-form.service';
import { BlogPostUpdate } from './blog-post-update';

describe('BlogPost Management Update Component', () => {
  let comp: BlogPostUpdate;
  let fixture: ComponentFixture<BlogPostUpdate>;
  let activatedRoute: ActivatedRoute;
  let blogPostFormService: BlogPostFormService;
  let blogPostService: BlogPostService;
  let blogCategoryService: BlogCategoryService;
  let tagService: TagService;

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

    fixture = TestBed.createComponent(BlogPostUpdate);
    activatedRoute = TestBed.inject(ActivatedRoute);
    blogPostFormService = TestBed.inject(BlogPostFormService);
    blogPostService = TestBed.inject(BlogPostService);
    blogCategoryService = TestBed.inject(BlogCategoryService);
    tagService = TestBed.inject(TagService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call BlogCategory query and add missing value', () => {
      const blogPost: IBlogPost = { id: 15145 };
      const categories: IBlogCategory[] = [{ id: 27812 }];
      blogPost.categories = categories;

      const blogCategoryCollection: IBlogCategory[] = [{ id: 27812 }];
      vi.spyOn(blogCategoryService, 'query').mockReturnValue(of(new HttpResponse({ body: blogCategoryCollection })));
      const additionalBlogCategories = [...categories];
      const expectedCollection: IBlogCategory[] = [...additionalBlogCategories, ...blogCategoryCollection];
      vi.spyOn(blogCategoryService, 'addBlogCategoryToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ blogPost });
      comp.ngOnInit();

      expect(blogCategoryService.query).toHaveBeenCalled();
      expect(blogCategoryService.addBlogCategoryToCollectionIfMissing).toHaveBeenCalledWith(
        blogCategoryCollection,
        ...additionalBlogCategories.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.blogCategoriesSharedCollection()).toEqual(expectedCollection);
    });

    it('should call Tag query and add missing value', () => {
      const blogPost: IBlogPost = { id: 15145 };
      const tags: ITag[] = [{ id: 19931 }];
      blogPost.tags = tags;

      const tagCollection: ITag[] = [{ id: 19931 }];
      vi.spyOn(tagService, 'query').mockReturnValue(of(new HttpResponse({ body: tagCollection })));
      const additionalTags = [...tags];
      const expectedCollection: ITag[] = [...additionalTags, ...tagCollection];
      vi.spyOn(tagService, 'addTagToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ blogPost });
      comp.ngOnInit();

      expect(tagService.query).toHaveBeenCalled();
      expect(tagService.addTagToCollectionIfMissing).toHaveBeenCalledWith(
        tagCollection,
        ...additionalTags.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.tagsSharedCollection()).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const blogPost: IBlogPost = { id: 15145 };
      const category: IBlogCategory = { id: 27812 };
      blogPost.categories = [category];
      const tag: ITag = { id: 19931 };
      blogPost.tags = [tag];

      activatedRoute.data = of({ blogPost });
      comp.ngOnInit();

      expect(comp.blogCategoriesSharedCollection()).toContainEqual(category);
      expect(comp.tagsSharedCollection()).toContainEqual(tag);
      expect(comp.blogPost).toEqual(blogPost);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<IBlogPost>();
      const blogPost = { id: 11641 };
      vi.spyOn(blogPostFormService, 'getBlogPost').mockReturnValue(blogPost);
      vi.spyOn(blogPostService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ blogPost });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(blogPost);
      saveSubject.complete();

      // THEN
      expect(blogPostFormService.getBlogPost).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(blogPostService.update).toHaveBeenCalledWith(expect.objectContaining(blogPost));
      expect(comp.isSaving()).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<IBlogPost>();
      const blogPost = { id: 11641 };
      vi.spyOn(blogPostFormService, 'getBlogPost').mockReturnValue({ id: null });
      vi.spyOn(blogPostService, 'create').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ blogPost: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(blogPost);
      saveSubject.complete();

      // THEN
      expect(blogPostFormService.getBlogPost).toHaveBeenCalled();
      expect(blogPostService.create).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<IBlogPost>();
      const blogPost = { id: 11641 };
      vi.spyOn(blogPostService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ blogPost });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(blogPostService.update).toHaveBeenCalled();
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

    describe('compareTag', () => {
      it('should forward to tagService', () => {
        const entity = { id: 19931 };
        const entity2 = { id: 16779 };
        vi.spyOn(tagService, 'compareTag');
        comp.compareTag(entity, entity2);
        expect(tagService.compareTag).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
