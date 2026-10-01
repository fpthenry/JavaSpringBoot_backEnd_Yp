import { beforeEach, describe, expect, it } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../blog-post.test-samples';

import { BlogPostFormService } from './blog-post-form.service';

describe('BlogPost Form Service', () => {
  let service: BlogPostFormService;

  beforeEach(() => {
    service = TestBed.inject(BlogPostFormService);
  });

  describe('Service methods', () => {
    describe('createBlogPostFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createBlogPostFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            wpId: expect.any(Object),
            title: expect.any(Object),
            slug: expect.any(Object),
            content: expect.any(Object),
            excerpt: expect.any(Object),
            thumbnail: expect.any(Object),
            status: expect.any(Object),
            viewCount: expect.any(Object),
            publishedAt: expect.any(Object),
            createdAt: expect.any(Object),
            updatedAt: expect.any(Object),
          }),
        );
      });

      it('passing IBlogPost should create a new form with FormGroup', () => {
        const formGroup = service.createBlogPostFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            wpId: expect.any(Object),
            title: expect.any(Object),
            slug: expect.any(Object),
            content: expect.any(Object),
            excerpt: expect.any(Object),
            thumbnail: expect.any(Object),
            status: expect.any(Object),
            viewCount: expect.any(Object),
            publishedAt: expect.any(Object),
            createdAt: expect.any(Object),
            updatedAt: expect.any(Object),
          }),
        );
      });
    });

    describe('getBlogPost', () => {
      it('should return NewBlogPost for default BlogPost initial value', () => {
        const formGroup = service.createBlogPostFormGroup(sampleWithNewData);

        const blogPost = service.getBlogPost(formGroup);

        expect(blogPost).toMatchObject(sampleWithNewData);
      });

      it('should return NewBlogPost for empty BlogPost initial value', () => {
        const formGroup = service.createBlogPostFormGroup();

        const blogPost = service.getBlogPost(formGroup);

        expect(blogPost).toMatchObject({});
      });

      it('should return IBlogPost', () => {
        const formGroup = service.createBlogPostFormGroup(sampleWithRequiredData);

        const blogPost = service.getBlogPost(formGroup);

        expect(blogPost).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IBlogPost should not enable id FormControl', () => {
        const formGroup = service.createBlogPostFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewBlogPost should disable id FormControl', () => {
        const formGroup = service.createBlogPostFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
