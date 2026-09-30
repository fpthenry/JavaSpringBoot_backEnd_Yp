import dayjs from 'dayjs/esm';

import { IArticle, NewArticle } from './article.model';

export const sampleWithRequiredData: IArticle = {
  id: 17774,
  title: 'however draft weight',
  slug: 'decriminalize pants ornate',
};

export const sampleWithPartialData: IArticle = {
  id: 4142,
  title: 'lively lecture',
  slug: 'respray rebuff solace',
  content: '../fake-data/blob/hipster.txt',
  status: 'curse metabolise sea',
  updatedAt: dayjs('2026-09-29T19:22'),
};

export const sampleWithFullData: IArticle = {
  id: 28493,
  title: 'athwart',
  slug: 'yearly nicely rise',
  excerpt: '../fake-data/blob/hipster.txt',
  content: '../fake-data/blob/hipster.txt',
  status: 'during season vice',
  featuredImageUrl: '../fake-data/blob/hipster.txt',
  createdAt: dayjs('2026-09-30T06:48'),
  updatedAt: dayjs('2026-09-30T08:34'),
};

export const sampleWithNewData: NewArticle = {
  title: 'yum',
  slug: 'rule',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
