import { beforeEach, describe, expect, it } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../article-tag.test-samples';

import { ArticleTagFormService } from './article-tag-form.service';

describe('ArticleTag Form Service', () => {
  let service: ArticleTagFormService;

  beforeEach(() => {
    service = TestBed.inject(ArticleTagFormService);
  });

  describe('Service methods', () => {
    describe('createArticleTagFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createArticleTagFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            name: expect.any(Object),
            slug: expect.any(Object),
            count: expect.any(Object),
            articleses: expect.any(Object),
          }),
        );
      });

      it('passing IArticleTag should create a new form with FormGroup', () => {
        const formGroup = service.createArticleTagFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            name: expect.any(Object),
            slug: expect.any(Object),
            count: expect.any(Object),
            articleses: expect.any(Object),
          }),
        );
      });
    });

    describe('getArticleTag', () => {
      it('should return NewArticleTag for default ArticleTag initial value', () => {
        const formGroup = service.createArticleTagFormGroup(sampleWithNewData);

        const articleTag = service.getArticleTag(formGroup);

        expect(articleTag).toMatchObject(sampleWithNewData);
      });

      it('should return NewArticleTag for empty ArticleTag initial value', () => {
        const formGroup = service.createArticleTagFormGroup();

        const articleTag = service.getArticleTag(formGroup);

        expect(articleTag).toMatchObject({});
      });

      it('should return IArticleTag', () => {
        const formGroup = service.createArticleTagFormGroup(sampleWithRequiredData);

        const articleTag = service.getArticleTag(formGroup);

        expect(articleTag).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IArticleTag should not enable id FormControl', () => {
        const formGroup = service.createArticleTagFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewArticleTag should disable id FormControl', () => {
        const formGroup = service.createArticleTagFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
