import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { IStaticPage } from '../static-page.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../static-page.test-samples';

import { RestStaticPage, StaticPageService } from './static-page.service';

const requireRestSample: RestStaticPage = {
  ...sampleWithRequiredData,
  createdAt: sampleWithRequiredData.createdAt?.toJSON(),
  updatedAt: sampleWithRequiredData.updatedAt?.toJSON(),
};

describe('StaticPage Service', () => {
  let service: StaticPageService;
  let httpMock: HttpTestingController;
  let expectedResult: IStaticPage | IStaticPage[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(StaticPageService);
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

    it('should create a StaticPage', () => {
      const staticPage = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(staticPage).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a StaticPage', () => {
      const staticPage = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(staticPage).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a StaticPage', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of StaticPage', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a StaticPage', () => {
      service.delete(123).subscribe();

      const requests = httpMock.match({ method: 'DELETE' });
      expect(requests).toHaveLength(1);
    });

    it('should handle exceptions for searching a StaticPage', () => {
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

    describe('addStaticPageToCollectionIfMissing', () => {
      it('should add a StaticPage to an empty array', () => {
        const staticPage: IStaticPage = sampleWithRequiredData;
        expectedResult = service.addStaticPageToCollectionIfMissing([], staticPage);
        expect(expectedResult).toEqual([staticPage]);
      });

      it('should not add a StaticPage to an array that contains it', () => {
        const staticPage: IStaticPage = sampleWithRequiredData;
        const staticPageCollection: IStaticPage[] = [
          {
            ...staticPage,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addStaticPageToCollectionIfMissing(staticPageCollection, staticPage);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a StaticPage to an array that doesn't contain it", () => {
        const staticPage: IStaticPage = sampleWithRequiredData;
        const staticPageCollection: IStaticPage[] = [sampleWithPartialData];
        expectedResult = service.addStaticPageToCollectionIfMissing(staticPageCollection, staticPage);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(staticPage);
      });

      it('should add only unique StaticPage to an array', () => {
        const staticPageArray: IStaticPage[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const staticPageCollection: IStaticPage[] = [sampleWithRequiredData];
        expectedResult = service.addStaticPageToCollectionIfMissing(staticPageCollection, ...staticPageArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const staticPage: IStaticPage = sampleWithRequiredData;
        const staticPage2: IStaticPage = sampleWithPartialData;
        expectedResult = service.addStaticPageToCollectionIfMissing([], staticPage, staticPage2);
        expect(expectedResult).toEqual([staticPage, staticPage2]);
      });

      it('should accept null and undefined values', () => {
        const staticPage: IStaticPage = sampleWithRequiredData;
        expectedResult = service.addStaticPageToCollectionIfMissing([], null, staticPage, undefined);
        expect(expectedResult).toEqual([staticPage]);
      });

      it('should return initial array if no StaticPage is added', () => {
        const staticPageCollection: IStaticPage[] = [sampleWithRequiredData];
        expectedResult = service.addStaticPageToCollectionIfMissing(staticPageCollection, undefined, null);
        expect(expectedResult).toEqual(staticPageCollection);
      });
    });

    describe('compareStaticPage', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareStaticPage(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 24576 };
        const entity2 = null;

        const compareResult1 = service.compareStaticPage(entity1, entity2);
        const compareResult2 = service.compareStaticPage(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 24576 };
        const entity2 = { id: 24334 };

        const compareResult1 = service.compareStaticPage(entity1, entity2);
        const compareResult2 = service.compareStaticPage(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return true if primaryKey matches', () => {
        const entity1 = { id: 24576 };
        const entity2 = { id: 24576 };

        const compareResult1 = service.compareStaticPage(entity1, entity2);
        const compareResult2 = service.compareStaticPage(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
