import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { IBlogCategory } from '../blog-category.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../blog-category.test-samples';

import { BlogCategoryService } from './blog-category.service';

const requireRestSample: IBlogCategory = {
  ...sampleWithRequiredData,
};

describe('BlogCategory Service', () => {
  let service: BlogCategoryService;
  let httpMock: HttpTestingController;
  let expectedResult: IBlogCategory | IBlogCategory[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(BlogCategoryService);
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

    it('should create a BlogCategory', () => {
      const blogCategory = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(blogCategory).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a BlogCategory', () => {
      const blogCategory = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(blogCategory).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a BlogCategory', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of BlogCategory', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a BlogCategory', () => {
      service.delete(123).subscribe();

      const requests = httpMock.match({ method: 'DELETE' });
      expect(requests).toHaveLength(1);
    });

    it('should handle exceptions for searching a BlogCategory', () => {
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

    describe('addBlogCategoryToCollectionIfMissing', () => {
      it('should add a BlogCategory to an empty array', () => {
        const blogCategory: IBlogCategory = sampleWithRequiredData;
        expectedResult = service.addBlogCategoryToCollectionIfMissing([], blogCategory);
        expect(expectedResult).toEqual([blogCategory]);
      });

      it('should not add a BlogCategory to an array that contains it', () => {
        const blogCategory: IBlogCategory = sampleWithRequiredData;
        const blogCategoryCollection: IBlogCategory[] = [
          {
            ...blogCategory,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addBlogCategoryToCollectionIfMissing(blogCategoryCollection, blogCategory);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a BlogCategory to an array that doesn't contain it", () => {
        const blogCategory: IBlogCategory = sampleWithRequiredData;
        const blogCategoryCollection: IBlogCategory[] = [sampleWithPartialData];
        expectedResult = service.addBlogCategoryToCollectionIfMissing(blogCategoryCollection, blogCategory);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(blogCategory);
      });

      it('should add only unique BlogCategory to an array', () => {
        const blogCategoryArray: IBlogCategory[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const blogCategoryCollection: IBlogCategory[] = [sampleWithRequiredData];
        expectedResult = service.addBlogCategoryToCollectionIfMissing(blogCategoryCollection, ...blogCategoryArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const blogCategory: IBlogCategory = sampleWithRequiredData;
        const blogCategory2: IBlogCategory = sampleWithPartialData;
        expectedResult = service.addBlogCategoryToCollectionIfMissing([], blogCategory, blogCategory2);
        expect(expectedResult).toEqual([blogCategory, blogCategory2]);
      });

      it('should accept null and undefined values', () => {
        const blogCategory: IBlogCategory = sampleWithRequiredData;
        expectedResult = service.addBlogCategoryToCollectionIfMissing([], null, blogCategory, undefined);
        expect(expectedResult).toEqual([blogCategory]);
      });

      it('should return initial array if no BlogCategory is added', () => {
        const blogCategoryCollection: IBlogCategory[] = [sampleWithRequiredData];
        expectedResult = service.addBlogCategoryToCollectionIfMissing(blogCategoryCollection, undefined, null);
        expect(expectedResult).toEqual(blogCategoryCollection);
      });
    });

    describe('compareBlogCategory', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareBlogCategory(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 27812 };
        const entity2 = null;

        const compareResult1 = service.compareBlogCategory(entity1, entity2);
        const compareResult2 = service.compareBlogCategory(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 27812 };
        const entity2 = { id: 21032 };

        const compareResult1 = service.compareBlogCategory(entity1, entity2);
        const compareResult2 = service.compareBlogCategory(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return true if primaryKey matches', () => {
        const entity1 = { id: 27812 };
        const entity2 = { id: 27812 };

        const compareResult1 = service.compareBlogCategory(entity1, entity2);
        const compareResult2 = service.compareBlogCategory(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
