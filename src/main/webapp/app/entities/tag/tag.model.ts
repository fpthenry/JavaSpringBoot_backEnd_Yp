export interface ITag {
  id: number;
  wpTermId?: number | null;
  name?: string | null;
  slug?: string | null;
}

export type NewTag = Omit<ITag, 'id'> & { id: null };
