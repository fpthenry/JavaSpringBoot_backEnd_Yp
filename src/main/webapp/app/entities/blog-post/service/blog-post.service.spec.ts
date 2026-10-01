import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { IBlogPost } from '../blog-post.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../blog-post.test-samples';

import { BlogPostService, RestBlogPost } from './blog-post.service';

const requireRestSample: RestBlogPost = {
  ...sampleWithRequiredData,
  publishedAt: sampleWithRequiredData.publishedAt?.toJSON(),
  createdAt: sampleWithRequiredData.createdAt?.toJSON(),
  updatedAt: sampleWithRequiredData.updatedAt?.toJSON(),
};

describe('BlogPost Service', () => {
  let service: BlogPostService;
  let httpMock: HttpTestingController;
  let expectedResult: IBlogPost | IBlogPost[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(BlogPostService);
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

    it('should create a BlogPost', () => {
      const blogPost = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(blogPost).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a BlogPost', () => {
      const blogPost = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(blogPost).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a BlogPost', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of BlogPost', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a BlogPost', () => {
      service.delete(123).subscribe();

      const requests = httpMock.match({ method: 'DELETE' });
      expect(requests).toHaveLength(1);
    });

    it('should handle exceptions for searching a BlogPost', () => {
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

    describe('addBlogPostToCollectionIfMissing', () => {
      it('should add a BlogPost to an empty array', () => {
        const blogPost: IBlogPost = sampleWithRequiredData;
        expectedResult = service.addBlogPostToCollectionIfMissing([], blogPost);
        expect(expectedResult).toEqual([blogPost]);
      });

      it('should not add a BlogPost to an array that contains it', () => {
        const blogPost: IBlogPost = sampleWithRequiredData;
        const blogPostCollection: IBlogPost[] = [
          {
            ...blogPost,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addBlogPostToCollectionIfMissing(blogPostCollection, blogPost);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a BlogPost to an array that doesn't contain it", () => {
        const blogPost: IBlogPost = sampleWithRequiredData;
        const blogPostCollection: IBlogPost[] = [sampleWithPartialData];
        expectedResult = service.addBlogPostToCollectionIfMissing(blogPostCollection, blogPost);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(blogPost);
      });

      it('should add only unique BlogPost to an array', () => {
        const blogPostArray: IBlogPost[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const blogPostCollection: IBlogPost[] = [sampleWithRequiredData];
        expectedResult = service.addBlogPostToCollectionIfMissing(blogPostCollection, ...blogPostArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const blogPost: IBlogPost = sampleWithRequiredData;
        const blogPost2: IBlogPost = sampleWithPartialData;
        expectedResult = service.addBlogPostToCollectionIfMissing([], blogPost, blogPost2);
        expect(expectedResult).toEqual([blogPost, blogPost2]);
      });

      it('should accept null and undefined values', () => {
        const blogPost: IBlogPost = sampleWithRequiredData;
        expectedResult = service.addBlogPostToCollectionIfMissing([], null, blogPost, undefined);
        expect(expectedResult).toEqual([blogPost]);
      });

      it('should return initial array if no BlogPost is added', () => {
        const blogPostCollection: IBlogPost[] = [sampleWithRequiredData];
        expectedResult = service.addBlogPostToCollectionIfMissing(blogPostCollection, undefined, null);
        expect(expectedResult).toEqual(blogPostCollection);
      });
    });

    describe('compareBlogPost', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareBlogPost(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 11641 };
        const entity2 = null;

        const compareResult1 = service.compareBlogPost(entity1, entity2);
        const compareResult2 = service.compareBlogPost(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 11641 };
        const entity2 = { id: 15145 };

        const compareResult1 = service.compareBlogPost(entity1, entity2);
        const compareResult2 = service.compareBlogPost(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return true if primaryKey matches', () => {
        const entity1 = { id: 11641 };
        const entity2 = { id: 11641 };

        const compareResult1 = service.compareBlogPost(entity1, entity2);
        const compareResult2 = service.compareBlogPost(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
