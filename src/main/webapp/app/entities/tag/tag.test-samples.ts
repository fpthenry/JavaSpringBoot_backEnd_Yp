import { ITag, NewTag } from './tag.model';

export const sampleWithRequiredData: ITag = {
  id: 17364,
  name: 'perp loosely into',
};

export const sampleWithPartialData: ITag = {
  id: 25623,
  name: 'velvety mosh incidentally',
};

export const sampleWithFullData: ITag = {
  id: 28385,
  wpTermId: 28912,
  name: 'voluminous unaware once',
  slug: 'that why',
};

export const sampleWithNewData: NewTag = {
  name: 'silk eek so',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
