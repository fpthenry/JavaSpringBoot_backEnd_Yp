import dayjs from 'dayjs/esm';

import { IGalleryImage, NewGalleryImage } from './gallery-image.model';

export const sampleWithRequiredData: IGalleryImage = {
  id: 29073,
  displayOrder: 27702,
  active: false,
};

export const sampleWithPartialData: IGalleryImage = {
  id: 20505,
  title: 'phooey coast',
  imageUrl: 'drive',
  displayOrder: 9108,
  active: true,
  startAt: dayjs('2026-10-04T12:31'),
  openInNewTab: true,
};

export const sampleWithFullData: IGalleryImage = {
  id: 32491,
  title: 'enrage potentially below',
  image: '../fake-data/blob/hipster.png',
  imageContentType: 'unknown',
  imageUrl: 'than',
  linkUrl: 'athwart likewise gee',
  altText: 'noxious likewise',
  displayOrder: 12917,
  active: false,
  startAt: dayjs('2026-10-04T07:38'),
  endAt: dayjs('2026-10-04T23:04'),
  openInNewTab: false,
};

export const sampleWithNewData: NewGalleryImage = {
  displayOrder: 13123,
  active: false,
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
