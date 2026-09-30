import dayjs from 'dayjs/esm';

import { IGallery, NewGallery } from './gallery.model';

export const sampleWithRequiredData: IGallery = {
  id: 8426,
  title: 'skeletal',
};

export const sampleWithPartialData: IGallery = {
  id: 32185,
  title: 'white seldom warming',
  slug: 'sheepishly briskly',
  status: 'underneath well now',
};

export const sampleWithFullData: IGallery = {
  id: 31182,
  title: 'kowtow openly whose',
  slug: 'diligently',
  content: '../fake-data/blob/hipster.txt',
  status: 'whoa till',
  createdAt: dayjs('2026-09-29T14:06'),
  updatedAt: dayjs('2026-09-29T18:04'),
};

export const sampleWithNewData: NewGallery = {
  title: 'uh-huh although overconfidently',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
