import dayjs from 'dayjs/esm';

import { ILocation, NewLocation } from './location.model';

export const sampleWithRequiredData: ILocation = {
  id: 12482,
  name: 'and',
  slug: 'drowse unto considering',
};

export const sampleWithPartialData: ILocation = {
  id: 2046,
  name: 'boldly mob oh',
  slug: 'geez nor',
  provinceCode: 'upright al',
  districtCode: 'good vivid',
  displayOrder: 29168,
  createdAt: dayjs('2026-09-30T00:49'),
  updatedAt: dayjs('2026-09-29T20:55'),
};

export const sampleWithFullData: ILocation = {
  id: 22007,
  name: 'abseil given',
  slug: 'underneath emerge excluding',
  type: 'fatal',
  provinceCode: 'duh agains',
  districtCode: 'per conseq',
  wardCode: 'strict kin',
  count: 8301,
  displayOrder: 16166,
  createdAt: dayjs('2026-09-30T04:47'),
  updatedAt: dayjs('2026-09-30T03:50'),
};

export const sampleWithNewData: NewLocation = {
  name: 'cake',
  slug: 'plus train ornate',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
