import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpHeaders, HttpResponse, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

import { IImportBatch } from '../listing-import.model';
import { ListingImportService } from '../service/listing-import.service';

import { ListingImportLog } from './listing-import-log';

const BATCH: IImportBatch = {
  id: 54,
  source: 'api:yp-directus',
  dryRun: false,
  status: 'DONE',
  totalRows: 73047,
  createdCount: 827,
  mergedCount: 72215,
  unchangedCount: 0,
  suspectCount: 0,
  duplicateInBatchCount: 2,
  invalidCount: 5,
  startedAt: '2026-11-06T06:40:00Z',
};

describe('ListingImportLog', () => {
  let service: ListingImportService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideTranslateService(), provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    service = TestBed.inject(ListingImportService);
  });

  it('asks only for finished real imports and shows added, updated and rejected counts', () => {
    vi.spyOn(service, 'batches').mockReturnValue(
      of(new HttpResponse({ body: [BATCH], headers: new HttpHeaders({ 'X-Total-Count': '1' }) })),
    );
    const fixture = TestBed.createComponent(ListingImportLog);
    fixture.detectChanges();

    expect(service.batches).toHaveBeenCalledWith(0, 20, false);
    const row = (fixture.nativeElement as HTMLElement).querySelector('[data-cy="logRow"]') as HTMLElement;
    const text = row.textContent.replace(/\s+/g, ' ');
    expect(text).toContain((827).toLocaleString('vi-VN'));
    expect(text).toContain((72215).toLocaleString('vi-VN'));
    expect(text).toContain('7');
  });
});
