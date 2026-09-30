import { beforeEach, describe, expect, it } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../article-category.test-samples';

import { ArticleCategoryFormService } from './article-category-form.service';

describe('ArticleCategory Form Service', () => {
  let service: ArticleCategoryFormService;

  beforeEach(() => {
    service = TestBed.inject(ArticleCategoryFormService);
  });

  describe('Service methods', () => {
    describe('createArticleCategoryFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createArticleCategoryFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            name: expect.any(Object),
            slug: expect.any(Object),
            description: expect.any(Object),
            count: expect.any(Object),
            parent: expect.any(Object),
            articleses: expect.any(Object),
          }),
        );
      });

      it('passing IArticleCategory should create a new form with FormGroup', () => {
        const formGroup = service.createArticleCategoryFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            name: expect.any(Object),
            slug: expect.any(Object),
            description: expect.any(Object),
            count: expect.any(Object),
            parent: expect.any(Object),
            articleses: expect.any(Object),
          }),
        );
      });
    });

    describe('getArticleCategory', () => {
      it('should return NewArticleCategory for default ArticleCategory initial value', () => {
        const formGroup = service.createArticleCategoryFormGroup(sampleWithNewData);

        const articleCategory = service.getArticleCategory(formGroup);

        expect(articleCategory).toMatchObject(sampleWithNewData);
      });

      it('should return NewArticleCategory for empty ArticleCategory initial value', () => {
        const formGroup = service.createArticleCategoryFormGroup();

        const articleCategory = service.getArticleCategory(formGroup);

        expect(articleCategory).toMatchObject({});
      });

      it('should return IArticleCategory', () => {
        const formGroup = service.createArticleCategoryFormGroup(sampleWithRequiredData);

        const articleCategory = service.getArticleCategory(formGroup);

        expect(articleCategory).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IArticleCategory should not enable id FormControl', () => {
        const formGroup = service.createArticleCategoryFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewArticleCategory should disable id FormControl', () => {
        const formGroup = service.createArticleCategoryFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
