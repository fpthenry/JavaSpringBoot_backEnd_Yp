import { Routes } from '@angular/router';

import { ASC } from 'app/config';
import { userRouteAccessService } from 'app/core/auth';

import RedirectRuleResolve from './route/redirect-rule-routing-resolve.service';

const redirectRuleRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/redirect-rule').then(m => m.RedirectRule),
    data: {
      defaultSort: `id,${ASC}`,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./detail/redirect-rule-detail').then(m => m.RedirectRuleDetail),
    resolve: {
      redirectRule: RedirectRuleResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./update/redirect-rule-update').then(m => m.RedirectRuleUpdate),
    resolve: {
      redirectRule: RedirectRuleResolve,
    },
    canActivate: [userRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./update/redirect-rule-update').then(m => m.RedirectRuleUpdate),
    resolve: {
      redirectRule: RedirectRuleResolve,
    },
    canActivate: [userRouteAccessService],
  },
];

export default redirectRuleRoute;
