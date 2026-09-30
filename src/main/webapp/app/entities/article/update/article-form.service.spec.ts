import { beforeEach, describe, expect, it } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../article.test-samples';

import { ArticleFormService } from './article-form.service';

describe('Article Form Service', () => {
  let service: ArticleFormService;

  beforeEach(() => {
    service = TestBed.inject(ArticleFormService);
  });

  describe('Service methods', () => {
    describe('createArticleFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createArticleFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            title: expect.any(Object),
            slug: expect.any(Object),
            excerpt: expect.any(Object),
            content: expect.any(Object),
            status: expect.any(Object),
            featuredImageUrl: expect.any(Object),
            createdAt: expect.any(Object),
            updatedAt: expect.any(Object),
            author: expect.any(Object),
            categorieses: expect.any(Object),
            tagses: expect.any(Object),
          }),
        );
      });

      it('passing IArticle should create a new form with FormGroup', () => {
        const formGroup = service.createArticleFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            title: expect.any(Object),
            slug: expect.any(Object),
            excerpt: expect.any(Object),
            content: expect.any(Object),
            status: expect.any(Object),
            featuredImageUrl: expect.any(Object),
            createdAt: expect.any(Object),
            updatedAt: expect.any(Object),
            author: expect.any(Object),
            categorieses: expect.any(Object),
            tagses: expect.any(Object),
          }),
        );
      });
    });

    describe('getArticle', () => {
      it('should return NewArticle for default Article initial value', () => {
        const formGroup = service.createArticleFormGroup(sampleWithNewData);

        const article = service.getArticle(formGroup);

        expect(article).toMatchObject(sampleWithNewData);
      });

      it('should return NewArticle for empty Article initial value', () => {
        const formGroup = service.createArticleFormGroup();

        const article = service.getArticle(formGroup);

        expect(article).toMatchObject({});
      });

      it('should return IArticle', () => {
        const formGroup = service.createArticleFormGroup(sampleWithRequiredData);

        const article = service.getArticle(formGroup);

        expect(article).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IArticle should not enable id FormControl', () => {
        const formGroup = service.createArticleFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewArticle should disable id FormControl', () => {
        const formGroup = service.createArticleFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
