import { IGallery, NewGallery } from './gallery.model';

export const sampleWithRequiredData: IGallery = {
  id: 8426,
  name: 'skeletal',
  code: 'fzec-',
  active: true,
};

export const sampleWithPartialData: IGallery = {
  id: 13478,
  name: 'massage energetically near',
  code: '6',
  description: '../fake-data/blob/hipster.txt',
  active: false,
};

export const sampleWithFullData: IGallery = {
  id: 31182,
  name: 'kowtow openly whose',
  code: '9d',
  description: '../fake-data/blob/hipster.txt',
  active: true,
  wpId: 18530,
};

export const sampleWithNewData: NewGallery = {
  name: 'uh-huh although overconfidently',
  code: 'c6',
  active: false,
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
