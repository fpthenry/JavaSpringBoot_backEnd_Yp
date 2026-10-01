import { beforeEach, describe, expect, it, vi } from 'vitest';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { Subject, from, of } from 'rxjs';

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

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should update editForm', () => {
      const blogPost: IBlogPost = { id: 15145 };

      activatedRoute.data = of({ blogPost });
      comp.ngOnInit();

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
});
