import { IRedirectRule, NewRedirectRule } from './redirect-rule.model';

export const sampleWithRequiredData: IRedirectRule = {
  id: 15830,
  sourceSlug: 'matter',
  destinationSlug: 'whether diligently vainly',
  objectType: 'given yum',
};

export const sampleWithPartialData: IRedirectRule = {
  id: 1898,
  sourceId: 8272,
  sourceSlug: 'winged',
  destinationId: 7851,
  destinationSlug: 'ouch lucky concentration',
  objectType: 'after',
};

export const sampleWithFullData: IRedirectRule = {
  id: 30260,
  sourceId: 4759,
  sourceSlug: 'absentmindedly that',
  destinationId: 31741,
  destinationSlug: 'liberalize swanling',
  objectType: 'pfft',
};

export const sampleWithNewData: NewRedirectRule = {
  sourceSlug: 'fill',
  destinationSlug: 'and',
  objectType: 'once segregate incline',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
