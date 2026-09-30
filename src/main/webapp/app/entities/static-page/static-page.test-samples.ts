import dayjs from 'dayjs/esm';

import { IStaticPage, NewStaticPage } from './static-page.model';

export const sampleWithRequiredData: IStaticPage = {
  id: 24645,
  title: 'paralyse substantial evenly',
  slug: 'carelessly',
};

export const sampleWithPartialData: IStaticPage = {
  id: 25099,
  title: 'valuable phew ouch',
  slug: 'suspension',
  content: '../fake-data/blob/hipster.txt',
  template: 'monthly whoa interestingly',
  createdAt: dayjs('2026-09-30T04:13'),
};

export const sampleWithFullData: IStaticPage = {
  id: 21259,
  title: 'powerfully focalise humble',
  slug: 'adolescent reorganisation',
  content: '../fake-data/blob/hipster.txt',
  status: 'unlearn openly amids',
  template: 'sweetly',
  menuOrder: 13038,
  createdAt: dayjs('2026-09-29T17:09'),
  updatedAt: dayjs('2026-09-29T12:47'),
};

export const sampleWithNewData: NewStaticPage = {
  title: 'sunder concerning',
  slug: 'dismal triumphantly premise',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
