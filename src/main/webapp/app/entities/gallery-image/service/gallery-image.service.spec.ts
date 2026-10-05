import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { IGalleryImage } from '../gallery-image.model';
import { sampleWithFullData, sampleWithNewData, sampleWithPartialData, sampleWithRequiredData } from '../gallery-image.test-samples';

import { GalleryImageService, RestGalleryImage } from './gallery-image.service';

const requireRestSample: RestGalleryImage = {
  ...sampleWithRequiredData,
  startAt: sampleWithRequiredData.startAt?.toJSON(),
  endAt: sampleWithRequiredData.endAt?.toJSON(),
};

describe('GalleryImage Service', () => {
  let service: GalleryImageService;
  let httpMock: HttpTestingController;
  let expectedResult: IGalleryImage | IGalleryImage[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(GalleryImageService);
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

    it('should create a GalleryImage', () => {
      const galleryImage = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(galleryImage).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a GalleryImage', () => {
      const galleryImage = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(galleryImage).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a GalleryImage', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of GalleryImage', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a GalleryImage', () => {
      service.delete(123).subscribe();

      const requests = httpMock.match({ method: 'DELETE' });
      expect(requests).toHaveLength(1);
    });

    describe('addGalleryImageToCollectionIfMissing', () => {
      it('should add a GalleryImage to an empty array', () => {
        const galleryImage: IGalleryImage = sampleWithRequiredData;
        expectedResult = service.addGalleryImageToCollectionIfMissing([], galleryImage);
        expect(expectedResult).toEqual([galleryImage]);
      });

      it('should not add a GalleryImage to an array that contains it', () => {
        const galleryImage: IGalleryImage = sampleWithRequiredData;
        const galleryImageCollection: IGalleryImage[] = [
          {
            ...galleryImage,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addGalleryImageToCollectionIfMissing(galleryImageCollection, galleryImage);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a GalleryImage to an array that doesn't contain it", () => {
        const galleryImage: IGalleryImage = sampleWithRequiredData;
        const galleryImageCollection: IGalleryImage[] = [sampleWithPartialData];
        expectedResult = service.addGalleryImageToCollectionIfMissing(galleryImageCollection, galleryImage);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(galleryImage);
      });

      it('should add only unique GalleryImage to an array', () => {
        const galleryImageArray: IGalleryImage[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const galleryImageCollection: IGalleryImage[] = [sampleWithRequiredData];
        expectedResult = service.addGalleryImageToCollectionIfMissing(galleryImageCollection, ...galleryImageArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const galleryImage: IGalleryImage = sampleWithRequiredData;
        const galleryImage2: IGalleryImage = sampleWithPartialData;
        expectedResult = service.addGalleryImageToCollectionIfMissing([], galleryImage, galleryImage2);
        expect(expectedResult).toEqual([galleryImage, galleryImage2]);
      });

      it('should accept null and undefined values', () => {
        const galleryImage: IGalleryImage = sampleWithRequiredData;
        expectedResult = service.addGalleryImageToCollectionIfMissing([], null, galleryImage, undefined);
        expect(expectedResult).toEqual([galleryImage]);
      });

      it('should return initial array if no GalleryImage is added', () => {
        const galleryImageCollection: IGalleryImage[] = [sampleWithRequiredData];
        expectedResult = service.addGalleryImageToCollectionIfMissing(galleryImageCollection, undefined, null);
        expect(expectedResult).toEqual(galleryImageCollection);
      });
    });

    describe('compareGalleryImage', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareGalleryImage(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 21370 };
        const entity2 = null;

        const compareResult1 = service.compareGalleryImage(entity1, entity2);
        const compareResult2 = service.compareGalleryImage(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 21370 };
        const entity2 = { id: 4556 };

        const compareResult1 = service.compareGalleryImage(entity1, entity2);
        const compareResult2 = service.compareGalleryImage(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return true if primaryKey matches', () => {
        const entity1 = { id: 21370 };
        const entity2 = { id: 21370 };

        const compareResult1 = service.compareGalleryImage(entity1, entity2);
        const compareResult2 = service.compareGalleryImage(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
