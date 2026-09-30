import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { IArticleCategory } from '../article-category.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../article-category.test-samples';

import { ArticleCategoryService } from './article-category.service';

const requireRestSample: IArticleCategory = {
  ...sampleWithRequiredData,
};

describe('ArticleCategory Service', () => {
  let service: ArticleCategoryService;
  let httpMock: HttpTestingController;
  let expectedResult: IArticleCategory | IArticleCategory[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(ArticleCategoryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  describe('Service methods', () => {
    it('should find an element', () => {
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.find(123).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should create a ArticleCategory', () => {
      const articleCategory = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(articleCategory).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a ArticleCategory', () => {
      const articleCategory = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(articleCategory).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a ArticleCategory', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of ArticleCategory', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a ArticleCategory', () => {
      service.delete(123).subscribe();

      const requests = httpMock.match({ method: 'DELETE' });
      expect(requests).toHaveLength(1);
    });

    it('should handle exceptions for searching a ArticleCategory', () => {
      const queryObject: any = {
        page: 0,
        size: 20,
        query: '',
        sort: [],
      };
      service.search(queryObject).subscribe(() => expectedResult);

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush(null, { status: 500, statusText: 'Internal Server Error' });
      expect(expectedResult).toBeNull();
    });

    describe('addArticleCategoryToCollectionIfMissing', () => {
      it('should add a ArticleCategory to an empty array', () => {
        const articleCategory: IArticleCategory = sampleWithRequiredData;
        expectedResult = service.addArticleCategoryToCollectionIfMissing([], articleCategory);
        expect(expectedResult).toEqual([articleCategory]);
      });

      it('should not add a ArticleCategory to an array that contains it', () => {
        const articleCategory: IArticleCategory = sampleWithRequiredData;
        const articleCategoryCollection: IArticleCategory[] = [
          {
            ...articleCategory,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addArticleCategoryToCollectionIfMissing(articleCategoryCollection, articleCategory);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a ArticleCategory to an array that doesn't contain it", () => {
        const articleCategory: IArticleCategory = sampleWithRequiredData;
        const articleCategoryCollection: IArticleCategory[] = [sampleWithPartialData];
        expectedResult = service.addArticleCategoryToCollectionIfMissing(articleCategoryCollection, articleCategory);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(articleCategory);
      });

      it('should add only unique ArticleCategory to an array', () => {
        const articleCategoryArray: IArticleCategory[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const articleCategoryCollection: IArticleCategory[] = [sampleWithRequiredData];
        expectedResult = service.addArticleCategoryToCollectionIfMissing(articleCategoryCollection, ...articleCategoryArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const articleCategory: IArticleCategory = sampleWithRequiredData;
        const articleCategory2: IArticleCategory = sampleWithPartialData;
        expectedResult = service.addArticleCategoryToCollectionIfMissing([], articleCategory, articleCategory2);
        expect(expectedResult).toEqual([articleCategory, articleCategory2]);
      });

      it('should accept null and undefined values', () => {
        const articleCategory: IArticleCategory = sampleWithRequiredData;
        expectedResult = service.addArticleCategoryToCollectionIfMissing([], null, articleCategory, undefined);
        expect(expectedResult).toEqual([articleCategory]);
      });

      it('should return initial array if no ArticleCategory is added', () => {
        const articleCategoryCollection: IArticleCategory[] = [sampleWithRequiredData];
        expectedResult = service.addArticleCategoryToCollectionIfMissing(articleCategoryCollection, undefined, null);
        expect(expectedResult).toEqual(articleCategoryCollection);
      });
    });

    describe('compareArticleCategory', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareArticleCategory(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 11547 };
        const entity2 = null;

        const compareResult1 = service.compareArticleCategory(entity1, entity2);
        const compareResult2 = service.compareArticleCategory(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 11547 };
        const entity2 = { id: 17997 };

        const compareResult1 = service.compareArticleCategory(entity1, entity2);
        const compareResult2 = service.compareArticleCategory(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return true if primaryKey matches', () => {
        const entity1 = { id: 11547 };
        const entity2 = { id: 11547 };

        const compareResult1 = service.compareArticleCategory(entity1, entity2);
        const compareResult2 = service.compareArticleCategory(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
