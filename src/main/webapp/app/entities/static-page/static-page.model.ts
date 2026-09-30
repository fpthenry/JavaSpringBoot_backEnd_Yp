import dayjs from 'dayjs/esm';

import { IUser } from 'app/entities/user/user.model';

export interface IStaticPage {
  id: number;
  title?: string | null;
  slug?: string | null;
  content?: string | null;
  status?: string | null;
  template?: string | null;
  menuOrder?: number | null;
  createdAt?: dayjs.Dayjs | null;
  updatedAt?: dayjs.Dayjs | null;
  author?: Pick<IUser, 'id' | 'login'> | null;
  parent?: Pick<IStaticPage, 'id' | 'title'> | null;
}

export type NewStaticPage = Omit<IStaticPage, 'id'> & { id: null };
