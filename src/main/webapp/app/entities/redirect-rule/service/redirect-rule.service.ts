import { HttpClient, HttpResponse, httpResource } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';

import { Observable, asapScheduler, catchError, scheduled } from 'rxjs';

import { serverApiUrl } from 'app/config';
import { SearchWithPagination, createRequestOption } from 'app/core/request';
import { IRedirectRule, NewRedirectRule } from '../redirect-rule.model';

export type PartialUpdateRedirectRule = Partial<IRedirectRule> & Pick<IRedirectRule, 'id'>;

@Service()
export class RedirectRulesService {
  readonly redirectRulesParams = signal<Record<string, string | number | boolean | readonly (string | number | boolean)[]> | undefined>(
    undefined,
  );
  readonly redirectRulesResource = httpResource<IRedirectRule[]>(() => {
    const params = this.redirectRulesParams();
    if (!params) {
      return undefined;
    }
    return { url: params.query ? this.resourceSearchUrl : this.resourceUrl, params };
  });
  /**
   * This signal holds the list of redirectRule that have been fetched. It is updated when the redirectRulesResource emits a new value.
   * In case of error while fetching the redirectRules, the signal is set to an empty array.
   */
  readonly redirectRules = computed(() => (this.redirectRulesResource.hasValue() ? this.redirectRulesResource.value() : []));
  protected readonly resourceUrl = `${serverApiUrl}api/redirect-rules`;
  protected readonly resourceSearchUrl = `${serverApiUrl}api/redirect-rules/_search`;
}

@Service()
export class RedirectRuleService extends RedirectRulesService {
  protected readonly http = inject(HttpClient);

  create(redirectRule: NewRedirectRule): Observable<IRedirectRule> {
    return this.http.post<IRedirectRule>(this.resourceUrl, redirectRule);
  }

  update(redirectRule: IRedirectRule): Observable<IRedirectRule> {
    return this.http.put<IRedirectRule>(
      `${this.resourceUrl}/${encodeURIComponent(this.getRedirectRuleIdentifier(redirectRule))}`,
      redirectRule,
    );
  }

  partialUpdate(redirectRule: PartialUpdateRedirectRule): Observable<IRedirectRule> {
    return this.http.patch<IRedirectRule>(
      `${this.resourceUrl}/${encodeURIComponent(this.getRedirectRuleIdentifier(redirectRule))}`,
      redirectRule,
    );
  }

  find(id: number): Observable<IRedirectRule> {
    return this.http.get<IRedirectRule>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  query(req?: any): Observable<HttpResponse<IRedirectRule[]>> {
    const options = createRequestOption(req);
    return this.http.get<IRedirectRule[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<undefined> {
    return this.http.delete<undefined>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  search(req: SearchWithPagination): Observable<IRedirectRule[]> {
    const options = createRequestOption(req);
    return this.http.get<IRedirectRule[]>(this.resourceSearchUrl, { params: options }).pipe(catchError(() => scheduled([], asapScheduler)));
  }

  getRedirectRuleIdentifier(redirectRule: Pick<IRedirectRule, 'id'>): number {
    return redirectRule.id;
  }

  compareRedirectRule(o1: Pick<IRedirectRule, 'id'> | null, o2: Pick<IRedirectRule, 'id'> | null): boolean {
    return o1 && o2 ? this.getRedirectRuleIdentifier(o1) === this.getRedirectRuleIdentifier(o2) : o1 === o2;
  }

  addRedirectRuleToCollectionIfMissing<Type extends Pick<IRedirectRule, 'id'>>(
    redirectRuleCollection: Type[],
    ...redirectRulesToCheck: (Type | null | undefined)[]
  ): Type[] {
    const redirectRules: Type[] = redirectRulesToCheck.filter(
      redirectRuleItem => redirectRuleItem !== null && redirectRuleItem !== undefined,
    );
    if (redirectRules.length > 0) {
      const redirectRuleCollectionIdentifiers = redirectRuleCollection.map(redirectRuleItem =>
        this.getRedirectRuleIdentifier(redirectRuleItem),
      );
      const redirectRulesToAdd = redirectRules.filter(redirectRuleItem => {
        const redirectRuleIdentifier = this.getRedirectRuleIdentifier(redirectRuleItem);
        if (redirectRuleCollectionIdentifiers.includes(redirectRuleIdentifier)) {
          return false;
        }
        redirectRuleCollectionIdentifiers.push(redirectRuleIdentifier);
        return true;
      });
      return [...redirectRulesToAdd, ...redirectRuleCollection];
    }
    return redirectRuleCollection;
  }
}
