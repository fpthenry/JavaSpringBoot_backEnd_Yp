import dayjs from 'dayjs/esm';

import { ILocation, NewLocation } from './location.model';

export const sampleWithRequiredData: ILocation = {
  id: 12482,
  name: 'and',
};

export const sampleWithPartialData: ILocation = {
  id: 4133,
  name: 'likewise',
  slug: 'mob',
  type: 'hungrily geez',
  longitude: 29838.11,
  createdAt: dayjs('2026-09-30T21:15'),
};

export const sampleWithFullData: ILocation = {
  id: 22007,
  wpTermId: 12210,
  name: 'object ack',
  slug: 'fluctuate',
  type: 'once when viciously',
  code: 'outrun adrenalin where',
  latitude: 16814.5,
  longitude: 20862.95,
  createdAt: dayjs('2026-09-30T17:42'),
};

export const sampleWithNewData: NewLocation = {
  name: 'cake',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
