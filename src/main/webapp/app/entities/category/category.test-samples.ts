import dayjs from 'dayjs/esm';

import { ICategory, NewCategory } from './category.model';

export const sampleWithRequiredData: ICategory = {
  id: 8109,
  name: 'midst croon cautiously',
};

export const sampleWithPartialData: ICategory = {
  id: 13566,
  wpTermId: 8343,
  name: 'mothball cornet',
  slug: 'consequently',
  description: '../fake-data/blob/hipster.txt',
};

export const sampleWithFullData: ICategory = {
  id: 28780,
  wpTermId: 6868,
  name: 'filthy',
  slug: 'flight',
  description: '../fake-data/blob/hipster.txt',
  icon: 'unless bleakly',
  listingCount: 22110,
  createdAt: dayjs('2026-09-30T22:37'),
  updatedAt: dayjs('2026-09-30T21:27'),
};

export const sampleWithNewData: NewCategory = {
  name: 'where pfft regularly',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
