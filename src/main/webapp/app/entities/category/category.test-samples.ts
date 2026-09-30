import dayjs from 'dayjs/esm';

import { ICategory, NewCategory } from './category.model';

export const sampleWithRequiredData: ICategory = {
  id: 8109,
  name: 'midst croon cautiously',
  slug: 'fooey preheat',
};

export const sampleWithPartialData: ICategory = {
  id: 28713,
  name: 'technologist whenever',
  slug: 'midst bah within',
  description: '../fake-data/blob/hipster.txt',
  count: 16294,
  industryCode: 'beard lest',
};

export const sampleWithFullData: ICategory = {
  id: 28780,
  name: 'ouch',
  slug: 'self-confidence',
  description: '../fake-data/blob/hipster.txt',
  count: 11192,
  industryCode: 'unless bleakly',
  displayOrder: 22110,
  createdAt: dayjs('2026-09-30T04:52'),
  updatedAt: dayjs('2026-09-30T03:43'),
};

export const sampleWithNewData: NewCategory = {
  name: 'where pfft regularly',
  slug: 'rarely regulate phooey',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
