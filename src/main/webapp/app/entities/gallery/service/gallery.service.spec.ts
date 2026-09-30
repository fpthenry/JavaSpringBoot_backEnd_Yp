import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { IGallery } from '../gallery.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../gallery.test-samples';

import { GalleryService, RestGallery } from './gallery.service';

const requireRestSample: RestGallery = {
  ...sampleWithRequiredData,
  createdAt: sampleWithRequiredData.createdAt?.toJSON(),
  updatedAt: sampleWithRequiredData.updatedAt?.toJSON(),
};

describe('Gallery Service', () => {
  let service: GalleryService;
  let httpMock: HttpTestingController;
  let expectedResult: IGallery | IGallery[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(GalleryService);
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

    it('should create a Gallery', () => {
      const gallery = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(gallery).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a Gallery', () => {
      const gallery = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(gallery).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a Gallery', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of Gallery', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a Gallery', () => {
      service.delete(123).subscribe();

      const requests = httpMock.match({ method: 'DELETE' });
      expect(requests).toHaveLength(1);
    });

    it('should handle exceptions for searching a Gallery', () => {
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

    describe('addGalleryToCollectionIfMissing', () => {
      it('should add a Gallery to an empty array', () => {
        const gallery: IGallery = sampleWithRequiredData;
        expectedResult = service.addGalleryToCollectionIfMissing([], gallery);
        expect(expectedResult).toEqual([gallery]);
      });

      it('should not add a Gallery to an array that contains it', () => {
        const gallery: IGallery = sampleWithRequiredData;
        const galleryCollection: IGallery[] = [
          {
            ...gallery,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addGalleryToCollectionIfMissing(galleryCollection, gallery);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a Gallery to an array that doesn't contain it", () => {
        const gallery: IGallery = sampleWithRequiredData;
        const galleryCollection: IGallery[] = [sampleWithPartialData];
        expectedResult = service.addGalleryToCollectionIfMissing(galleryCollection, gallery);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(gallery);
      });

      it('should add only unique Gallery to an array', () => {
        const galleryArray: IGallery[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const galleryCollection: IGallery[] = [sampleWithRequiredData];
        expectedResult = service.addGalleryToCollectionIfMissing(galleryCollection, ...galleryArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const gallery: IGallery = sampleWithRequiredData;
        const gallery2: IGallery = sampleWithPartialData;
        expectedResult = service.addGalleryToCollectionIfMissing([], gallery, gallery2);
        expect(expectedResult).toEqual([gallery, gallery2]);
      });

      it('should accept null and undefined values', () => {
        const gallery: IGallery = sampleWithRequiredData;
        expectedResult = service.addGalleryToCollectionIfMissing([], null, gallery, undefined);
        expect(expectedResult).toEqual([gallery]);
      });

      it('should return initial array if no Gallery is added', () => {
        const galleryCollection: IGallery[] = [sampleWithRequiredData];
        expectedResult = service.addGalleryToCollectionIfMissing(galleryCollection, undefined, null);
        expect(expectedResult).toEqual(galleryCollection);
      });
    });

    describe('compareGallery', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareGallery(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 16173 };
        const entity2 = null;

        const compareResult1 = service.compareGallery(entity1, entity2);
        const compareResult2 = service.compareGallery(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 16173 };
        const entity2 = { id: 7807 };

        const compareResult1 = service.compareGallery(entity1, entity2);
        const compareResult2 = service.compareGallery(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return true if primaryKey matches', () => {
        const entity1 = { id: 16173 };
        const entity2 = { id: 16173 };

        const compareResult1 = service.compareGallery(entity1, entity2);
        const compareResult2 = service.compareGallery(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
