import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpErrorResponse, HttpHeaders, HttpResponse, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { FaIconLibrary } from '@fortawesome/angular-fontawesome';
import { provideTranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';

import { fontAwesomeIcons } from 'app/config/font-awesome-icons';
import { IImportBatch } from '../listing-import.model';
import { ListingImportService } from '../service/listing-import.service';

import { ListingImportList } from './listing-import-list';

const BATCH: IImportBatch = {
  id: 7,
  source: 'file',
  sourceName: 'doanh-nghiep.xlsx',
  dryRun: true,
  status: 'DONE',
  totalRows: 120,
  createdCount: 80,
  mergedCount: 20,
  unchangedCount: 5,
  suspectCount: 10,
  duplicateInBatchCount: 2,
  invalidCount: 3,
  createdBy: 'admin',
  startedAt: '2026-10-27T03:00:00Z',
  finishedAt: '2026-10-27T03:00:30Z',
};

describe('ListingImportList', () => {
  let comp: ListingImportList;
  let fixture: ComponentFixture<ListingImportList>;
  let service: ListingImportService;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideTranslateService(), provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    TestBed.inject(FaIconLibrary).addIcons(...fontAwesomeIcons);
    service = TestBed.inject(ListingImportService);
    router = TestBed.inject(Router);
    vi.spyOn(service, 'batches').mockReturnValue(
      of(new HttpResponse({ body: [BATCH], headers: new HttpHeaders({ 'X-Total-Count': '1' }) })),
    );
    vi.spyOn(service, 'running').mockReturnValue(of(null));
    vi.spyOn(service, 'pullSummary').mockReturnValue(of({ stored: 100, pending: 20 }));
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(ListingImportList);
    comp = fixture.componentInstance;
  });

  const el = (): HTMLElement => fixture.nativeElement as HTMLElement;

  const directusCheck: IImportBatch = { ...BATCH, source: 'api:yp-directus', sourceName: 'listing_source_record' };

  it('shows the three steps and no batch table', () => {
    fixture.detectChanges();
    comp['pull'].set({ stored: 100, pending: 20 }); // watchPull chỉ cập nhật ở lượt timer sau
    fixture.detectChanges();
    expect(el().querySelector('[data-cy="importSteps"]')).not.toBeNull();
    expect(el().querySelector('[data-cy="batchRow"]')).toBeNull();
    expect(el().querySelector('[data-cy="checkButton"]')).not.toBeNull();
  });

  it('gets only new and changed records by default, everything on request', () => {
    vi.spyOn(service, 'startPull').mockReturnValue(of({ stored: 100, pending: 20 }));
    fixture.detectChanges();
    (el().querySelector('[data-cy="pullButton"]') as HTMLButtonElement).click();
    expect(service.startPull).toHaveBeenLastCalledWith('NEW');
    (el().querySelector('[data-cy="pullFullButton"]') as HTMLButtonElement).click();
    expect(service.startPull).toHaveBeenLastCalledWith('FULL');
  });

  it('keeps showing the last pull and offers to continue an interrupted full pull', () => {
    vi.mocked(service.pullSummary).mockReturnValue(
      of({
        stored: 100,
        pending: 0,
        pull: { id: 3, state: 'INTERRUPTED', read: 74000, changed: 12, lastId: 83096, mode: 'FULL', startedAt: '2026-11-09T02:50:53Z' },
      }),
    );
    vi.spyOn(service, 'startPull').mockReturnValue(of({ stored: 100, pending: 0 }));
    fixture.detectChanges();
    const interrupted = {
      id: 3,
      state: 'INTERRUPTED',
      read: 74000,
      changed: 12,
      lastId: 83096,
      mode: 'FULL',
      startedAt: '2026-11-09T02:50:53Z',
    } as const;
    comp['pull'].set({ stored: 100, pending: 0, pull: interrupted, unfinishedFull: interrupted });
    fixture.detectChanges();
    expect(el().querySelector('[data-cy="pullProgress"]')).not.toBeNull();
    (el().querySelector('[data-cy="resumePull"]') as HTMLButtonElement).click();
    expect(service.startPull).toHaveBeenCalledWith('RESUME');
  });

  it('still offers to continue the interrupted full pull after a later quick pull', () => {
    vi.spyOn(service, 'startPull').mockReturnValue(of({ stored: 100, pending: 0 }));
    fixture.detectChanges();
    comp['pull'].set({
      stored: 100,
      pending: 0,
      pull: { id: 4, state: 'DONE', read: 0, changed: 0, lastId: 0, mode: 'NEW' },
      unfinishedFull: { id: 1, state: 'INTERRUPTED', read: 74000, changed: 0, lastId: 83096, mode: 'FULL' },
    });
    fixture.detectChanges();
    expect(el().querySelector('[data-cy="unfinishedFull"]')).not.toBeNull();
    (el().querySelector('[data-cy="resumePull"]') as HTMLButtonElement).click();
    expect(service.startPull).toHaveBeenCalledWith('RESUME');
  });

  it('cancels a running pull', () => {
    vi.spyOn(service, 'cancelPull').mockReturnValue(of(undefined));
    fixture.detectChanges();
    comp['pull'].set({ stored: 100, pending: 0, pull: { id: 4, state: 'RUNNING', read: 2000, changed: 0, lastId: 9000, mode: 'NEW' } });
    fixture.detectChanges();
    (el().querySelector('[data-cy="cancelPull"]') as HTMLButtonElement).click();
    expect(service.cancelPull).toHaveBeenCalled();
  });

  it('runs a check and stays on the page', () => {
    vi.spyOn(service, 'importDirectus').mockReturnValue(of({ ...directusCheck, status: 'RUNNING' }));
    fixture.detectChanges();
    comp.runDirectus(true);
    expect(service.importDirectus).toHaveBeenCalledWith(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('shows added, updated and rejected counts, then imports after confirming', () => {
    vi.mocked(service.batches).mockReturnValue(of(new HttpResponse({ body: [directusCheck] })));
    vi.spyOn(service, 'importDirectus').mockReturnValue(of({ ...directusCheck, dryRun: false, status: 'RUNNING' }));
    fixture.detectChanges();
    comp['pull'].set({ stored: 100, pending: 20 });
    fixture.detectChanges();
    expect(el().querySelector('[data-cy="checkResult"]')?.textContent).toContain('80');
    (el().querySelector('[data-cy="importButton"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(service.importDirectus).not.toHaveBeenCalled();
    (el().querySelector('[data-cy="importConfirm"]') as HTMLButtonElement).click();
    expect(service.importDirectus).toHaveBeenCalledWith(false);
  });

  it('says nothing is left to import once everything is in', () => {
    vi.mocked(service.pullSummary).mockReturnValue(of({ stored: 100, pending: 0 }));
    vi.mocked(service.batches).mockReturnValue(of(new HttpResponse({ body: [{ ...directusCheck, dryRun: false }] })));
    fixture.detectChanges();
    expect(el().querySelector('[data-cy="upToDate"]')).not.toBeNull();
    expect(el().querySelector('[data-cy="lastImport"]')).not.toBeNull();
    expect(el().querySelector('[data-cy="checkResult"]')).toBeNull();
  });

  it('accepts only .xlsx / .csv up to 100 MB', () => {
    comp.pickFile(new File(['x'], 'cu.xls'));
    expect(comp['file']()).toBeNull();
    expect(comp['fileError']()).toBe('javaSpringBootBackEndApp.listingImport.new.badType');

    const big = new File(['x'], 'lon.csv');
    Object.defineProperty(big, 'size', { value: 101 * 1024 * 1024 });
    comp.pickFile(big);
    expect(comp['fileError']()).toBe('javaSpringBootBackEndApp.listingImport.new.tooBig');

    const ok = new File(['a,b'], 'dn.xlsx');
    comp.pickFile(ok);
    expect(comp['file']()).toBe(ok);
    expect(comp['fileError']()).toBeNull();
  });

  it('submits the file as a dry run by default and opens the new batch', () => {
    vi.spyOn(service, 'importFile').mockReturnValue(of({ ...BATCH, id: 8, status: 'RUNNING' }));
    comp['tab'].set('file');
    const file = new File(['a,b'], 'dn.csv');
    comp.pickFile(file);
    expect(comp.canSubmit()).toBe(true);
    comp.submit();
    expect(service.importFile).toHaveBeenCalledWith(file, true);
    expect(router.navigate).toHaveBeenCalledWith(['/listing-import', 8]);
  });

  it('shows the backend message when another import is running (409)', () => {
    vi.spyOn(service, 'importFile').mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 409, error: { error: 'Đang có đợt nhập #5 chạy' } })),
    );
    comp['tab'].set('file');
    comp.pickFile(new File(['a'], 'dn.csv'));
    comp.submit();
    expect(comp['submitError']()).toBe('Đang có đợt nhập #5 chạy');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('cannot start while a batch is running', () => {
    comp['tab'].set('file');
    comp.pickFile(new File(['a'], 'dn.csv'));
    comp['running'].set({ ...BATCH, status: 'RUNNING' });
    expect(comp.canSubmit()).toBe(false);
  });

  it('builds the API request from the form (headers, field mapping, dry run)', () => {
    comp['tab'].set('api');
    comp['api'] = {
      source: 'masothue',
      url: 'https://api.vidu.vn/companies',
      itemsPath: 'data.items',
      headers: 'X-API-Key: abc',
      pageParam: 'page',
      firstPage: 1,
      maxPages: 5,
      delayMs: 300,
      fieldMapping: 'taxCode = mst',
    };
    expect(comp.canSubmit()).toBe(true);
    expect(comp.apiRequest()).toEqual({
      source: 'masothue',
      url: 'https://api.vidu.vn/companies',
      itemsPath: 'data.items',
      pageParam: 'page',
      firstPage: 1,
      maxPages: 5,
      delayMs: 300,
      headers: { 'X-API-Key': 'abc' },
      fieldMapping: { taxCode: 'mst' },
      dryRun: true,
    });
    comp['api'].source = 'Mã Sai';
    expect(comp.canSubmit()).toBe(false);
  });
});
