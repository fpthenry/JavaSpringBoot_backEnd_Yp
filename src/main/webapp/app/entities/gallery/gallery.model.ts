export interface IGallery {
  id: number;
  name?: string | null;
  code?: string | null;
  description?: string | null;
  active?: boolean | null;
  wpId?: number | null;
}

export type NewGallery = Omit<IGallery, 'id'> & { id: null };
