import dayjs from 'dayjs/esm';

import { IListingImage, NewListingImage } from './listing-image.model';

export const sampleWithRequiredData: IListingImage = {
  id: 20525,
  imageUrl: '../fake-data/blob/hipster.txt',
};

export const sampleWithPartialData: IListingImage = {
  id: 21812,
  imageUrl: '../fake-data/blob/hipster.txt',
  thumbnailUrl: '../fake-data/blob/hipster.txt',
  altText: 'waterspout',
  displayOrder: 7728,
};

export const sampleWithFullData: IListingImage = {
  id: 14433,
  imageUrl: '../fake-data/blob/hipster.txt',
  thumbnailUrl: '../fake-data/blob/hipster.txt',
  altText: 'amazing embarrassment',
  displayOrder: 31612,
  isFeatured: false,
  createdAt: dayjs('2026-09-30T06:09'),
  updatedAt: dayjs('2026-09-30T04:37'),
};

export const sampleWithNewData: NewListingImage = {
  imageUrl: '../fake-data/blob/hipster.txt',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
