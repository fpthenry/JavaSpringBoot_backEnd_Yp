import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpResponse } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { Subject, from, of } from 'rxjs';

import { IBlogPost } from 'app/entities/blog-post/blog-post.model';
import { BlogPostService } from 'app/entities/blog-post/service/blog-post.service';
import { TagService } from '../service/tag.service';
import { ITag } from '../tag.model';

import { TagFormService } from './tag-form.service';
import { TagUpdate } from './tag-update';

describe('Tag Management Update Component', () => {
  let comp: TagUpdate;
  let fixture: ComponentFixture<TagUpdate>;
  let activatedRoute: ActivatedRoute;
  let tagFormService: TagFormService;
  let tagService: TagService;
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

    fixture = TestBed.createComponent(TagUpdate);
    activatedRoute = TestBed.inject(ActivatedRoute);
    tagFormService = TestBed.inject(TagFormService);
    tagService = TestBed.inject(TagService);
    blogPostService = TestBed.inject(BlogPostService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call BlogPost query and add missing value', () => {
      const tag: ITag = { id: 16779 };
      const blogPosts: IBlogPost[] = [{ id: 11641 }];
      tag.blogPosts = blogPosts;

      const blogPostCollection: IBlogPost[] = [{ id: 11641 }];
      vi.spyOn(blogPostService, 'query').mockReturnValue(of(new HttpResponse({ body: blogPostCollection })));
      const additionalBlogPosts = [...blogPosts];
      const expectedCollection: IBlogPost[] = [...additionalBlogPosts, ...blogPostCollection];
      vi.spyOn(blogPostService, 'addBlogPostToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ tag });
      comp.ngOnInit();

      expect(blogPostService.query).toHaveBeenCalled();
      expect(blogPostService.addBlogPostToCollectionIfMissing).toHaveBeenCalledWith(
        blogPostCollection,
        ...additionalBlogPosts.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.blogPostsSharedCollection()).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const tag: ITag = { id: 16779 };
      const blogPost: IBlogPost = { id: 11641 };
      tag.blogPosts = [blogPost];

      activatedRoute.data = of({ tag });
      comp.ngOnInit();

      expect(comp.blogPostsSharedCollection()).toContainEqual(blogPost);
      expect(comp.tag).toEqual(tag);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<ITag>();
      const tag = { id: 19931 };
      vi.spyOn(tagFormService, 'getTag').mockReturnValue(tag);
      vi.spyOn(tagService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ tag });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(tag);
      saveSubject.complete();

      // THEN
      expect(tagFormService.getTag).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(tagService.update).toHaveBeenCalledWith(expect.objectContaining(tag));
      expect(comp.isSaving()).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<ITag>();
      const tag = { id: 19931 };
      vi.spyOn(tagFormService, 'getTag').mockReturnValue({ id: null });
      vi.spyOn(tagService, 'create').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ tag: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(tag);
      saveSubject.complete();

      // THEN
      expect(tagFormService.getTag).toHaveBeenCalled();
      expect(tagService.create).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<ITag>();
      const tag = { id: 19931 };
      vi.spyOn(tagService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ tag });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(tagService.update).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
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
