import { IArticleCategory, NewArticleCategory } from './article-category.model';

export const sampleWithRequiredData: IArticleCategory = {
  id: 32440,
  name: 'forenenst',
  slug: 'roughly',
};

export const sampleWithPartialData: IArticleCategory = {
  id: 19047,
  name: 'gee spectate provided',
  slug: 'within deflect',
  description: '../fake-data/blob/hipster.txt',
  count: 21784,
};

export const sampleWithFullData: IArticleCategory = {
  id: 6943,
  name: 'ignorant councilman harp',
  slug: 'inside',
  description: '../fake-data/blob/hipster.txt',
  count: 11690,
};

export const sampleWithNewData: NewArticleCategory = {
  name: 'soliloquy yowza kindly',
  slug: 'provided incidentally yuck',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
