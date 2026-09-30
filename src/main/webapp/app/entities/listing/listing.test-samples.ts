import dayjs from 'dayjs/esm';

import { IListing, NewListing } from './listing.model';

export const sampleWithRequiredData: IListing = {
  id: 21194,
  title: 'ruddy yum why',
  slug: 'worthwhile speedy blissfully',
};

export const sampleWithPartialData: IListing = {
  id: 15531,
  title: 'hourly recent recklessly',
  slug: 'pigsty below',
  status: 'elegantly regarding ',
  telephone: '375-743-0438 x5772',
  website: 'linear',
  taxCode: 'duh honestly nun',
  nameEn: 'construe',
  businessType: 'jeopardise',
  statusYp: 'scuffle',
  foundedDate: dayjs('2026-09-29T10:15'),
  licenseModifiedDate: dayjs('2026-09-30T00:44'),
  address: '../fake-data/blob/hipster.txt',
  latitude: 25788.66,
  longitude: 32152.46,
  apiId: 16990,
};

export const sampleWithFullData: IListing = {
  id: 28905,
  title: 'infatuated so',
  slug: 'oh gee settle',
  content: '../fake-data/blob/hipster.txt',
  status: 'giant but clearly',
  email: 'Juan_Schuppe@gmail.com',
  telephone: '(568) 490-9396',
  mobile: 'courageous next',
  website: 'worst qua after',
  fax: 'instead rise',
  taxCode: 'which christen awkwardly',
  nameAlias: 'unexpectedly majestically',
  nameEn: 'finding',
  representative: 'apud weary',
  mainIndustry: 'fervently even coop',
  managedBy: 'birdbath',
  businessType: 'custom why mysterious',
  statusYp: 'fray antelope birdbath',
  foundedDate: dayjs('2026-09-29T12:31'),
  licenseModifiedDate: dayjs('2026-09-30T09:29'),
  address: '../fake-data/blob/hipster.txt',
  addressAlternative: '../fake-data/blob/hipster.txt',
  latitude: 28746.2,
  longitude: 3135.06,
  apiId: 23333,
  createdAt: dayjs('2026-09-29T20:02'),
  updatedAt: dayjs('2026-09-30T03:21'),
};

export const sampleWithNewData: NewListing = {
  title: 'youthfully joyfully oh',
  slug: 'emerge',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
