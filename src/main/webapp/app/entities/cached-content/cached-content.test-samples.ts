import dayjs from 'dayjs/esm';

import { ICachedContent, NewCachedContent } from './cached-content.model';

export const sampleWithRequiredData: ICachedContent = {
  id: 24480,
  termSlug: 'noon accomplished',
};

export const sampleWithPartialData: ICachedContent = {
  id: 20483,
  termSlug: 'mallard',
  expired: dayjs('2026-09-29T17:53'),
  content: '../fake-data/blob/hipster.txt',
};

export const sampleWithFullData: ICachedContent = {
  id: 18399,
  termSlug: 'finally',
  expired: dayjs('2026-09-30T00:10'),
  content: '../fake-data/blob/hipster.txt',
};

export const sampleWithNewData: NewCachedContent = {
  termSlug: 'airport tenderly',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
