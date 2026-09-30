import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { IRedirectRule } from '../redirect-rule.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../redirect-rule.test-samples';

import { RedirectRuleService } from './redirect-rule.service';

const requireRestSample: IRedirectRule = {
  ...sampleWithRequiredData,
};

describe('RedirectRule Service', () => {
  let service: RedirectRuleService;
  let httpMock: HttpTestingController;
  let expectedResult: IRedirectRule | IRedirectRule[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(RedirectRuleService);
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

    it('should create a RedirectRule', () => {
      const redirectRule = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(redirectRule).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a RedirectRule', () => {
      const redirectRule = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(redirectRule).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a RedirectRule', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of RedirectRule', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a RedirectRule', () => {
      service.delete(123).subscribe();

      const requests = httpMock.match({ method: 'DELETE' });
      expect(requests).toHaveLength(1);
    });

    it('should handle exceptions for searching a RedirectRule', () => {
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

    describe('addRedirectRuleToCollectionIfMissing', () => {
      it('should add a RedirectRule to an empty array', () => {
        const redirectRule: IRedirectRule = sampleWithRequiredData;
        expectedResult = service.addRedirectRuleToCollectionIfMissing([], redirectRule);
        expect(expectedResult).toEqual([redirectRule]);
      });

      it('should not add a RedirectRule to an array that contains it', () => {
        const redirectRule: IRedirectRule = sampleWithRequiredData;
        const redirectRuleCollection: IRedirectRule[] = [
          {
            ...redirectRule,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addRedirectRuleToCollectionIfMissing(redirectRuleCollection, redirectRule);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a RedirectRule to an array that doesn't contain it", () => {
        const redirectRule: IRedirectRule = sampleWithRequiredData;
        const redirectRuleCollection: IRedirectRule[] = [sampleWithPartialData];
        expectedResult = service.addRedirectRuleToCollectionIfMissing(redirectRuleCollection, redirectRule);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(redirectRule);
      });

      it('should add only unique RedirectRule to an array', () => {
        const redirectRuleArray: IRedirectRule[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const redirectRuleCollection: IRedirectRule[] = [sampleWithRequiredData];
        expectedResult = service.addRedirectRuleToCollectionIfMissing(redirectRuleCollection, ...redirectRuleArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const redirectRule: IRedirectRule = sampleWithRequiredData;
        const redirectRule2: IRedirectRule = sampleWithPartialData;
        expectedResult = service.addRedirectRuleToCollectionIfMissing([], redirectRule, redirectRule2);
        expect(expectedResult).toEqual([redirectRule, redirectRule2]);
      });

      it('should accept null and undefined values', () => {
        const redirectRule: IRedirectRule = sampleWithRequiredData;
        expectedResult = service.addRedirectRuleToCollectionIfMissing([], null, redirectRule, undefined);
        expect(expectedResult).toEqual([redirectRule]);
      });

      it('should return initial array if no RedirectRule is added', () => {
        const redirectRuleCollection: IRedirectRule[] = [sampleWithRequiredData];
        expectedResult = service.addRedirectRuleToCollectionIfMissing(redirectRuleCollection, undefined, null);
        expect(expectedResult).toEqual(redirectRuleCollection);
      });
    });

    describe('compareRedirectRule', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareRedirectRule(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 31397 };
        const entity2 = null;

        const compareResult1 = service.compareRedirectRule(entity1, entity2);
        const compareResult2 = service.compareRedirectRule(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 31397 };
        const entity2 = { id: 31360 };

        const compareResult1 = service.compareRedirectRule(entity1, entity2);
        const compareResult2 = service.compareRedirectRule(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return true if primaryKey matches', () => {
        const entity1 = { id: 31397 };
        const entity2 = { id: 31397 };

        const compareResult1 = service.compareRedirectRule(entity1, entity2);
        const compareResult2 = service.compareRedirectRule(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
