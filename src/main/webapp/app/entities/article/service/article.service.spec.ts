import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { IArticle } from '../article.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../article.test-samples';

import { ArticleService, RestArticle } from './article.service';

const requireRestSample: RestArticle = {
  ...sampleWithRequiredData,
  createdAt: sampleWithRequiredData.createdAt?.toJSON(),
  updatedAt: sampleWithRequiredData.updatedAt?.toJSON(),
};

describe('Article Service', () => {
  let service: ArticleService;
  let httpMock: HttpTestingController;
  let expectedResult: IArticle | IArticle[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(ArticleService);
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

    it('should create a Article', () => {
      const article = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(article).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a Article', () => {
      const article = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(article).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a Article', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of Article', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a Article', () => {
      service.delete(123).subscribe();

      const requests = httpMock.match({ method: 'DELETE' });
      expect(requests).toHaveLength(1);
    });

    it('should handle exceptions for searching a Article', () => {
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

    describe('addArticleToCollectionIfMissing', () => {
      it('should add a Article to an empty array', () => {
        const article: IArticle = sampleWithRequiredData;
        expectedResult = service.addArticleToCollectionIfMissing([], article);
        expect(expectedResult).toEqual([article]);
      });

      it('should not add a Article to an array that contains it', () => {
        const article: IArticle = sampleWithRequiredData;
        const articleCollection: IArticle[] = [
          {
            ...article,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addArticleToCollectionIfMissing(articleCollection, article);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a Article to an array that doesn't contain it", () => {
        const article: IArticle = sampleWithRequiredData;
        const articleCollection: IArticle[] = [sampleWithPartialData];
        expectedResult = service.addArticleToCollectionIfMissing(articleCollection, article);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(article);
      });

      it('should add only unique Article to an array', () => {
        const articleArray: IArticle[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const articleCollection: IArticle[] = [sampleWithRequiredData];
        expectedResult = service.addArticleToCollectionIfMissing(articleCollection, ...articleArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const article: IArticle = sampleWithRequiredData;
        const article2: IArticle = sampleWithPartialData;
        expectedResult = service.addArticleToCollectionIfMissing([], article, article2);
        expect(expectedResult).toEqual([article, article2]);
      });

      it('should accept null and undefined values', () => {
        const article: IArticle = sampleWithRequiredData;
        expectedResult = service.addArticleToCollectionIfMissing([], null, article, undefined);
        expect(expectedResult).toEqual([article]);
      });

      it('should return initial array if no Article is added', () => {
        const articleCollection: IArticle[] = [sampleWithRequiredData];
        expectedResult = service.addArticleToCollectionIfMissing(articleCollection, undefined, null);
        expect(expectedResult).toEqual(articleCollection);
      });
    });

    describe('compareArticle', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareArticle(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 24128 };
        const entity2 = null;

        const compareResult1 = service.compareArticle(entity1, entity2);
        const compareResult2 = service.compareArticle(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 24128 };
        const entity2 = { id: 30377 };

        const compareResult1 = service.compareArticle(entity1, entity2);
        const compareResult2 = service.compareArticle(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return true if primaryKey matches', () => {
        const entity1 = { id: 24128 };
        const entity2 = { id: 24128 };

        const compareResult1 = service.compareArticle(entity1, entity2);
        const compareResult2 = service.compareArticle(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
