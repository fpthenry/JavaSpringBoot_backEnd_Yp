import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { IListingImage } from '../listing-image.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../listing-image.test-samples';

import { ListingImageService, RestListingImage } from './listing-image.service';

const requireRestSample: RestListingImage = {
  ...sampleWithRequiredData,
  createdAt: sampleWithRequiredData.createdAt?.toJSON(),
  updatedAt: sampleWithRequiredData.updatedAt?.toJSON(),
};

describe('ListingImage Service', () => {
  let service: ListingImageService;
  let httpMock: HttpTestingController;
  let expectedResult: IListingImage | IListingImage[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(ListingImageService);
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

    it('should create a ListingImage', () => {
      const listingImage = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(listingImage).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a ListingImage', () => {
      const listingImage = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(listingImage).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a ListingImage', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of ListingImage', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a ListingImage', () => {
      service.delete(123).subscribe();

      const requests = httpMock.match({ method: 'DELETE' });
      expect(requests).toHaveLength(1);
    });

    it('should handle exceptions for searching a ListingImage', () => {
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

    describe('addListingImageToCollectionIfMissing', () => {
      it('should add a ListingImage to an empty array', () => {
        const listingImage: IListingImage = sampleWithRequiredData;
        expectedResult = service.addListingImageToCollectionIfMissing([], listingImage);
        expect(expectedResult).toEqual([listingImage]);
      });

      it('should not add a ListingImage to an array that contains it', () => {
        const listingImage: IListingImage = sampleWithRequiredData;
        const listingImageCollection: IListingImage[] = [
          {
            ...listingImage,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addListingImageToCollectionIfMissing(listingImageCollection, listingImage);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a ListingImage to an array that doesn't contain it", () => {
        const listingImage: IListingImage = sampleWithRequiredData;
        const listingImageCollection: IListingImage[] = [sampleWithPartialData];
        expectedResult = service.addListingImageToCollectionIfMissing(listingImageCollection, listingImage);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(listingImage);
      });

      it('should add only unique ListingImage to an array', () => {
        const listingImageArray: IListingImage[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const listingImageCollection: IListingImage[] = [sampleWithRequiredData];
        expectedResult = service.addListingImageToCollectionIfMissing(listingImageCollection, ...listingImageArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const listingImage: IListingImage = sampleWithRequiredData;
        const listingImage2: IListingImage = sampleWithPartialData;
        expectedResult = service.addListingImageToCollectionIfMissing([], listingImage, listingImage2);
        expect(expectedResult).toEqual([listingImage, listingImage2]);
      });

      it('should accept null and undefined values', () => {
        const listingImage: IListingImage = sampleWithRequiredData;
        expectedResult = service.addListingImageToCollectionIfMissing([], null, listingImage, undefined);
        expect(expectedResult).toEqual([listingImage]);
      });

      it('should return initial array if no ListingImage is added', () => {
        const listingImageCollection: IListingImage[] = [sampleWithRequiredData];
        expectedResult = service.addListingImageToCollectionIfMissing(listingImageCollection, undefined, null);
        expect(expectedResult).toEqual(listingImageCollection);
      });
    });

    describe('compareListingImage', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareListingImage(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 8129 };
        const entity2 = null;

        const compareResult1 = service.compareListingImage(entity1, entity2);
        const compareResult2 = service.compareListingImage(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 8129 };
        const entity2 = { id: 30153 };

        const compareResult1 = service.compareListingImage(entity1, entity2);
        const compareResult2 = service.compareListingImage(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return true if primaryKey matches', () => {
        const entity1 = { id: 8129 };
        const entity2 = { id: 8129 };

        const compareResult1 = service.compareListingImage(entity1, entity2);
        const compareResult2 = service.compareListingImage(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
