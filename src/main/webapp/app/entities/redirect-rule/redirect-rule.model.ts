export interface IRedirectRule {
  id: number;
  sourceId?: number | null;
  sourceSlug?: string | null;
  destinationId?: number | null;
  destinationSlug?: string | null;
  objectType?: string | null;
}

export type NewRedirectRule = Omit<IRedirectRule, 'id'> & { id: null };
