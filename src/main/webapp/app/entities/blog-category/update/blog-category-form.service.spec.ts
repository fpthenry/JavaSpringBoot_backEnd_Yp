import { beforeEach, describe, expect, it } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../blog-category.test-samples';

import { BlogCategoryFormService } from './blog-category-form.service';

describe('BlogCategory Form Service', () => {
  let service: BlogCategoryFormService;

  beforeEach(() => {
    service = TestBed.inject(BlogCategoryFormService);
  });

  describe('Service methods', () => {
    describe('createBlogCategoryFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createBlogCategoryFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            wpTermId: expect.any(Object),
            name: expect.any(Object),
            slug: expect.any(Object),
            description: expect.any(Object),
            postCount: expect.any(Object),
            parent: expect.any(Object),
            blogPosts: expect.any(Object),
          }),
        );
      });

      it('passing IBlogCategory should create a new form with FormGroup', () => {
        const formGroup = service.createBlogCategoryFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            wpTermId: expect.any(Object),
            name: expect.any(Object),
            slug: expect.any(Object),
            description: expect.any(Object),
            postCount: expect.any(Object),
            parent: expect.any(Object),
            blogPosts: expect.any(Object),
          }),
        );
      });
    });

    describe('getBlogCategory', () => {
      it('should return NewBlogCategory for default BlogCategory initial value', () => {
        const formGroup = service.createBlogCategoryFormGroup(sampleWithNewData);

        const blogCategory = service.getBlogCategory(formGroup);

        expect(blogCategory).toMatchObject(sampleWithNewData);
      });

      it('should return NewBlogCategory for empty BlogCategory initial value', () => {
        const formGroup = service.createBlogCategoryFormGroup();

        const blogCategory = service.getBlogCategory(formGroup);

        expect(blogCategory).toMatchObject({});
      });

      it('should return IBlogCategory', () => {
        const formGroup = service.createBlogCategoryFormGroup(sampleWithRequiredData);

        const blogCategory = service.getBlogCategory(formGroup);

        expect(blogCategory).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IBlogCategory should not enable id FormControl', () => {
        const formGroup = service.createBlogCategoryFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewBlogCategory should disable id FormControl', () => {
        const formGroup = service.createBlogCategoryFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
