import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { IArticleTag } from '../article-tag.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../article-tag.test-samples';

import { ArticleTagService } from './article-tag.service';

const requireRestSample: IArticleTag = {
  ...sampleWithRequiredData,
};

describe('ArticleTag Service', () => {
  let service: ArticleTagService;
  let httpMock: HttpTestingController;
  let expectedResult: IArticleTag | IArticleTag[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(ArticleTagService);
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

    it('should create a ArticleTag', () => {
      const articleTag = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(articleTag).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a ArticleTag', () => {
      const articleTag = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(articleTag).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a ArticleTag', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of ArticleTag', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a ArticleTag', () => {
      service.delete(123).subscribe();

      const requests = httpMock.match({ method: 'DELETE' });
      expect(requests).toHaveLength(1);
    });

    it('should handle exceptions for searching a ArticleTag', () => {
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

    describe('addArticleTagToCollectionIfMissing', () => {
      it('should add a ArticleTag to an empty array', () => {
        const articleTag: IArticleTag = sampleWithRequiredData;
        expectedResult = service.addArticleTagToCollectionIfMissing([], articleTag);
        expect(expectedResult).toEqual([articleTag]);
      });

      it('should not add a ArticleTag to an array that contains it', () => {
        const articleTag: IArticleTag = sampleWithRequiredData;
        const articleTagCollection: IArticleTag[] = [
          {
            ...articleTag,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addArticleTagToCollectionIfMissing(articleTagCollection, articleTag);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a ArticleTag to an array that doesn't contain it", () => {
        const articleTag: IArticleTag = sampleWithRequiredData;
        const articleTagCollection: IArticleTag[] = [sampleWithPartialData];
        expectedResult = service.addArticleTagToCollectionIfMissing(articleTagCollection, articleTag);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(articleTag);
      });

      it('should add only unique ArticleTag to an array', () => {
        const articleTagArray: IArticleTag[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const articleTagCollection: IArticleTag[] = [sampleWithRequiredData];
        expectedResult = service.addArticleTagToCollectionIfMissing(articleTagCollection, ...articleTagArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const articleTag: IArticleTag = sampleWithRequiredData;
        const articleTag2: IArticleTag = sampleWithPartialData;
        expectedResult = service.addArticleTagToCollectionIfMissing([], articleTag, articleTag2);
        expect(expectedResult).toEqual([articleTag, articleTag2]);
      });

      it('should accept null and undefined values', () => {
        const articleTag: IArticleTag = sampleWithRequiredData;
        expectedResult = service.addArticleTagToCollectionIfMissing([], null, articleTag, undefined);
        expect(expectedResult).toEqual([articleTag]);
      });

      it('should return initial array if no ArticleTag is added', () => {
        const articleTagCollection: IArticleTag[] = [sampleWithRequiredData];
        expectedResult = service.addArticleTagToCollectionIfMissing(articleTagCollection, undefined, null);
        expect(expectedResult).toEqual(articleTagCollection);
      });
    });

    describe('compareArticleTag', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareArticleTag(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 22923 };
        const entity2 = null;

        const compareResult1 = service.compareArticleTag(entity1, entity2);
        const compareResult2 = service.compareArticleTag(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 22923 };
        const entity2 = { id: 25497 };

        const compareResult1 = service.compareArticleTag(entity1, entity2);
        const compareResult2 = service.compareArticleTag(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return true if primaryKey matches', () => {
        const entity1 = { id: 22923 };
        const entity2 = { id: 22923 };

        const compareResult1 = service.compareArticleTag(entity1, entity2);
        const compareResult2 = service.compareArticleTag(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
