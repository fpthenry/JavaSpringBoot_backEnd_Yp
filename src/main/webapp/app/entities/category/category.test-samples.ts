import dayjs from 'dayjs/esm';

import { ICategory, NewCategory } from './category.model';

export const sampleWithRequiredData: ICategory = {
  id: 8109,
  code: '4632',
  name: 'Bán buôn thực phẩm',
};

export const sampleWithPartialData: ICategory = {
  id: 13566,
  code: '463',
  level: 3,
  name: 'Bán buôn lương thực, thực phẩm, đồ uống và sản phẩm thuốc lá, thuốc lào',
  slug: 'ban-buon-luong-thuc-thuc-pham-do-uong-va-san-pham-thuoc-la-thuoc-lao-463',
  description: '../fake-data/blob/hipster.txt',
};

export const sampleWithFullData: ICategory = {
  id: 28780,
  code: '46321',
  level: 5,
  name: 'Bán buôn thịt và các sản phẩm từ thịt',
  slug: 'ban-buon-thit-va-cac-san-pham-tu-thit-46321',
  description: '../fake-data/blob/hipster.txt',
  listingCount: 22110,
  createdAt: dayjs('2026-09-30T22:37'),
  updatedAt: dayjs('2026-09-30T21:27'),
};

export const sampleWithNewData: NewCategory = {
  code: 'G',
  name: 'Bán buôn và bán lẻ',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
