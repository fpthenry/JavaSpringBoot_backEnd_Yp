import dayjs from 'dayjs/esm';

import { IListing, NewListing } from './listing.model';

export const sampleWithRequiredData: IListing = {
  id: 21194,
  wpId: 29134,
  name: 'aching plus hmph',
};

export const sampleWithPartialData: IListing = {
  id: 32639,
  wpId: 27665,
  name: 'timely heavily',
  nameEn: 'solicit wring',
  slug: 'fervently',
  phone: '672-800-7149 x48301',
  email: 'Maurice33@gmail.com',
  address: '../fake-data/blob/hipster.txt',
  capital: 'ha ha',
  foundedYear: 'stylish',
  businessType: 'opposite',
  businessStatus: 'role ack since',
  industryCode: 'sensitize cheerfully',
  thumbnail: 'specific meanwhile',
  images: '../fake-data/blob/hipster.txt',
  viewCount: 3152,
  publishedAt: dayjs('2026-09-30T03:55'),
  updatedAt: dayjs('2026-09-30T19:38'),
  esIndexed: false,
};

export const sampleWithFullData: IListing = {
  id: 28905,
  wpId: 16718,
  apiId: 'sadly um',
  name: 'unlike compromise',
  nameEn: 'boohoo under',
  nameAlias: 'since',
  slug: 'indeed inside',
  description: '../fake-data/blob/hipster.txt',
  phone: '(568) 490-9396',
  mobile: 'courageous next',
  email: 'Morton54@yahoo.com',
  website: 'amend um',
  address: '../fake-data/blob/hipster.txt',
  locationJson: '../fake-data/blob/hipster.txt',
  taxCode: 'inasmuch',
  representative: 'within hence',
  capital: 'outset gah',
  foundedYear: 'those',
  businessType: 'excluding',
  businessStatus: 'gadzooks hunger scarper',
  industryCode: 'with following',
  managedBy: 'atrium',
  thumbnail: 'than festival gah',
  images: '../fake-data/blob/hipster.txt',
  viewCount: 31704,
  isFeatured: false,
  status: 'request asset',
  publishedAt: dayjs('2026-09-30T04:22'),
  modifiedAt: dayjs('2026-10-01T03:24'),
  createdAt: dayjs('2026-09-30T23:40'),
  updatedAt: dayjs('2026-09-30T13:28'),
  esIndexed: false,
};

export const sampleWithNewData: NewListing = {
  wpId: 23616,
  name: 'gadzooks knavishly',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
