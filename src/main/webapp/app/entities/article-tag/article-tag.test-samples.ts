import { IArticleTag, NewArticleTag } from './article-tag.model';

export const sampleWithRequiredData: IArticleTag = {
  id: 24098,
  name: 'inasmuch',
  slug: 'quick-witted',
};

export const sampleWithPartialData: IArticleTag = {
  id: 4382,
  name: 'apud yippee as',
  slug: 'uh-huh',
};

export const sampleWithFullData: IArticleTag = {
  id: 20046,
  name: 'deer fishery',
  slug: 'frightened astride who',
  count: 19055,
};

export const sampleWithNewData: NewArticleTag = {
  name: 'approximate opposite',
  slug: 'especially yowza',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
