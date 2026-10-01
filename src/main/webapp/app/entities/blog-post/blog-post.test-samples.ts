import dayjs from 'dayjs/esm';

import { IBlogPost, NewBlogPost } from './blog-post.model';

export const sampleWithRequiredData: IBlogPost = {
  id: 13142,
  wpId: 29233,
  title: 'calmly jubilantly',
};

export const sampleWithPartialData: IBlogPost = {
  id: 3625,
  wpId: 1632,
  title: 'braid',
  content: '../fake-data/blob/hipster.txt',
  thumbnail: 'er waterspout',
  status: 'failing vice tank',
  viewCount: 30032,
  publishedAt: dayjs('2026-09-30T17:18'),
  createdAt: dayjs('2026-09-30T05:37'),
  updatedAt: dayjs('2026-10-01T01:34'),
};

export const sampleWithFullData: IBlogPost = {
  id: 10904,
  wpId: 24786,
  title: 'blushing whopping beside',
  slug: 'angrily',
  content: '../fake-data/blob/hipster.txt',
  excerpt: '../fake-data/blob/hipster.txt',
  thumbnail: 'beneath',
  status: 'mad',
  viewCount: 1843,
  publishedAt: dayjs('2026-09-30T12:15'),
  createdAt: dayjs('2026-10-01T03:06'),
  updatedAt: dayjs('2026-09-30T08:40'),
};

export const sampleWithNewData: NewBlogPost = {
  wpId: 12633,
  title: 'poppy',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
