import { IBlogCategory, NewBlogCategory } from './blog-category.model';

export const sampleWithRequiredData: IBlogCategory = {
  id: 16743,
  name: 'ah hateful',
};

export const sampleWithPartialData: IBlogCategory = {
  id: 29620,
  name: 'stormy',
  slug: 'whenever brr',
  postCount: 5903,
};

export const sampleWithFullData: IBlogCategory = {
  id: 2396,
  wpTermId: 11248,
  name: 'phew than astride',
  slug: 'amid ouch woot',
  description: '../fake-data/blob/hipster.txt',
  postCount: 16267,
};

export const sampleWithNewData: NewBlogCategory = {
  name: 'anneal',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
