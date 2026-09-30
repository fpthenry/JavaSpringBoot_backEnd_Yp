import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { IListing } from '../listing.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../listing.test-samples';

import { ListingService, RestListing } from './listing.service';

const requireRestSample: RestListing = {
  ...sampleWithRequiredData,
  foundedDate: sampleWithRequiredData.foundedDate?.toJSON(),
  licenseModifiedDate: sampleWithRequiredData.licenseModifiedDate?.toJSON(),
  createdAt: sampleWithRequiredData.createdAt?.toJSON(),
  updatedAt: sampleWithRequiredData.updatedAt?.toJSON(),
};

describe('Listing Service', () => {
  let service: ListingService;
  let httpMock: HttpTestingController;
  let expectedResult: IListing | IListing[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(ListingService);
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

    it('should create a Listing', () => {
      const listing = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(listing).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a Listing', () => {
      const listing = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(listing).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a Listing', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of Listing', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a Listing', () => {
      service.delete(123).subscribe();

      const requests = httpMock.match({ method: 'DELETE' });
      expect(requests).toHaveLength(1);
    });

    it('should handle exceptions for searching a Listing', () => {
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

    describe('addListingToCollectionIfMissing', () => {
      it('should add a Listing to an empty array', () => {
        const listing: IListing = sampleWithRequiredData;
        expectedResult = service.addListingToCollectionIfMissing([], listing);
        expect(expectedResult).toEqual([listing]);
      });

      it('should not add a Listing to an array that contains it', () => {
        const listing: IListing = sampleWithRequiredData;
        const listingCollection: IListing[] = [
          {
            ...listing,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addListingToCollectionIfMissing(listingCollection, listing);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a Listing to an array that doesn't contain it", () => {
        const listing: IListing = sampleWithRequiredData;
        const listingCollection: IListing[] = [sampleWithPartialData];
        expectedResult = service.addListingToCollectionIfMissing(listingCollection, listing);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(listing);
      });

      it('should add only unique Listing to an array', () => {
        const listingArray: IListing[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const listingCollection: IListing[] = [sampleWithRequiredData];
        expectedResult = service.addListingToCollectionIfMissing(listingCollection, ...listingArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const listing: IListing = sampleWithRequiredData;
        const listing2: IListing = sampleWithPartialData;
        expectedResult = service.addListingToCollectionIfMissing([], listing, listing2);
        expect(expectedResult).toEqual([listing, listing2]);
      });

      it('should accept null and undefined values', () => {
        const listing: IListing = sampleWithRequiredData;
        expectedResult = service.addListingToCollectionIfMissing([], null, listing, undefined);
        expect(expectedResult).toEqual([listing]);
      });

      it('should return initial array if no Listing is added', () => {
        const listingCollection: IListing[] = [sampleWithRequiredData];
        expectedResult = service.addListingToCollectionIfMissing(listingCollection, undefined, null);
        expect(expectedResult).toEqual(listingCollection);
      });
    });

    describe('compareListing', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareListing(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 14276 };
        const entity2 = null;

        const compareResult1 = service.compareListing(entity1, entity2);
        const compareResult2 = service.compareListing(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 14276 };
        const entity2 = { id: 86 };

        const compareResult1 = service.compareListing(entity1, entity2);
        const compareResult2 = service.compareListing(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return true if primaryKey matches', () => {
        const entity1 = { id: 14276 };
        const entity2 = { id: 14276 };

        const compareResult1 = service.compareListing(entity1, entity2);
        const compareResult2 = service.compareListing(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
