import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ICachedContent } from '../cached-content.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../cached-content.test-samples';

import { CachedContentService, RestCachedContent } from './cached-content.service';

const requireRestSample: RestCachedContent = {
  ...sampleWithRequiredData,
  expired: sampleWithRequiredData.expired?.toJSON(),
};

describe('CachedContent Service', () => {
  let service: CachedContentService;
  let httpMock: HttpTestingController;
  let expectedResult: ICachedContent | ICachedContent[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(CachedContentService);
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

    it('should create a CachedContent', () => {
      const cachedContent = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(cachedContent).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a CachedContent', () => {
      const cachedContent = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(cachedContent).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a CachedContent', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of CachedContent', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a CachedContent', () => {
      service.delete(123).subscribe();

      const requests = httpMock.match({ method: 'DELETE' });
      expect(requests).toHaveLength(1);
    });

    it('should handle exceptions for searching a CachedContent', () => {
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

    describe('addCachedContentToCollectionIfMissing', () => {
      it('should add a CachedContent to an empty array', () => {
        const cachedContent: ICachedContent = sampleWithRequiredData;
        expectedResult = service.addCachedContentToCollectionIfMissing([], cachedContent);
        expect(expectedResult).toEqual([cachedContent]);
      });

      it('should not add a CachedContent to an array that contains it', () => {
        const cachedContent: ICachedContent = sampleWithRequiredData;
        const cachedContentCollection: ICachedContent[] = [
          {
            ...cachedContent,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addCachedContentToCollectionIfMissing(cachedContentCollection, cachedContent);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a CachedContent to an array that doesn't contain it", () => {
        const cachedContent: ICachedContent = sampleWithRequiredData;
        const cachedContentCollection: ICachedContent[] = [sampleWithPartialData];
        expectedResult = service.addCachedContentToCollectionIfMissing(cachedContentCollection, cachedContent);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(cachedContent);
      });

      it('should add only unique CachedContent to an array', () => {
        const cachedContentArray: ICachedContent[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const cachedContentCollection: ICachedContent[] = [sampleWithRequiredData];
        expectedResult = service.addCachedContentToCollectionIfMissing(cachedContentCollection, ...cachedContentArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const cachedContent: ICachedContent = sampleWithRequiredData;
        const cachedContent2: ICachedContent = sampleWithPartialData;
        expectedResult = service.addCachedContentToCollectionIfMissing([], cachedContent, cachedContent2);
        expect(expectedResult).toEqual([cachedContent, cachedContent2]);
      });

      it('should accept null and undefined values', () => {
        const cachedContent: ICachedContent = sampleWithRequiredData;
        expectedResult = service.addCachedContentToCollectionIfMissing([], null, cachedContent, undefined);
        expect(expectedResult).toEqual([cachedContent]);
      });

      it('should return initial array if no CachedContent is added', () => {
        const cachedContentCollection: ICachedContent[] = [sampleWithRequiredData];
        expectedResult = service.addCachedContentToCollectionIfMissing(cachedContentCollection, undefined, null);
        expect(expectedResult).toEqual(cachedContentCollection);
      });
    });

    describe('compareCachedContent', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareCachedContent(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 5503 };
        const entity2 = null;

        const compareResult1 = service.compareCachedContent(entity1, entity2);
        const compareResult2 = service.compareCachedContent(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 5503 };
        const entity2 = { id: 31525 };

        const compareResult1 = service.compareCachedContent(entity1, entity2);
        const compareResult2 = service.compareCachedContent(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return true if primaryKey matches', () => {
        const entity1 = { id: 5503 };
        const entity2 = { id: 5503 };

        const compareResult1 = service.compareCachedContent(entity1, entity2);
        const compareResult2 = service.compareCachedContent(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
