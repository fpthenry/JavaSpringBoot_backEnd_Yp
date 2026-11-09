import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpHeaders, HttpResponse, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { FaIconLibrary } from '@fortawesome/angular-fontawesome';
import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

import { fontAwesomeIcons } from 'app/config/font-awesome-icons';
import { IImportBatch, IImportRowView } from '../listing-import.model';
import { ListingImportService } from '../service/listing-import.service';

import { ListingImportDetail } from './listing-import-detail';

const BATCH: IImportBatch = {
  id: 7,
  source: 'file',
  sourceName: 'dn.xlsx',
  dryRun: false,
  status: 'DONE',
  totalRows: 3,
  createdCount: 1,
  mergedCount: 1,
  unchangedCount: 0,
  suspectCount: 1,
  duplicateInBatchCount: 0,
  invalidCount: 0,
  startedAt: '2026-10-27T03:00:00Z',
  finishedAt: '2026-10-27T03:00:05Z',
};

const SUSPECT: IImportRowView = {
  row: {
    id: 101,
    batchId: 7,
    rowNo: 3,
    name: 'Công ty Tân Minh Technology',
    phone: '02435773348',
    status: 'SUSPECT',
    matchReason: 'NAME',
    matchScore: 0.7,
    matchedListingId: 533428,
  },
  data: { name: 'Công ty Tân Minh Technology', phone: '(024) 3577 3348' },
  matched: { id: 533428, name: 'CÔNG TY TNHH MINH TÂN TECHNOLOGY', phone: '024 3577 3348' },
};
const MERGED: IImportRowView = {
  row: {
    id: 102,
    batchId: 7,
    rowNo: 2,
    name: 'Tân Minh',
    taxCode: '0108001922',
    status: 'MERGED',
    matchReason: 'TAX_CODE',
    matchedListingId: 413261,
    filledFields: 'addressNew,images',
    message: 'Không nhận ra tỉnh: X',
  },
  data: { name: 'Tân Minh' },
  matched: { id: 413261, name: 'CÔNG TY TNHH CÔNG NGHỆ VÀ DỊCH VỤ TÂN MINH' },
};

describe('ListingImportDetail', () => {
  let service: ListingImportService;

  const setup = async (batch: IImportBatch): Promise<{ harness: RouterTestingHarness; comp: ListingImportDetail; el: HTMLElement }> => {
    vi.spyOn(service, 'batch').mockReturnValue(of(batch));
    const harness = await RouterTestingHarness.create();
    const comp = await harness.navigateByUrl('/listing-import/7', ListingImportDetail);
    harness.detectChanges();
    return { harness, comp, el: harness.routeNativeElement as HTMLElement };
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'listing-import/:id', component: ListingImportDetail }]),
      ],
    });
    TestBed.inject(FaIconLibrary).addIcons(...fontAwesomeIcons);
    service = TestBed.inject(ListingImportService);
    vi.spyOn(service, 'rows').mockReturnValue(
      of(new HttpResponse({ body: [MERGED, SUSPECT], headers: new HttpHeaders({ 'X-Total-Count': '2' }) })),
    );
    vi.spyOn(service, 'drafts').mockReturnValue(of(1));
    vi.spyOn(service, 'fieldStats').mockReturnValue(of([{ field: 'address', filled: 1, updated: 2 }]));
  });

  it('shows rows with filled fields, notes and a review box for the suspect row', async () => {
    const { el } = await setup(BATCH);
    expect(service.rows).toHaveBeenCalledWith(7, null, 0, 50, '', null);
    expect(el.querySelectorAll('[data-cy="importRow"]').length).toBe(2);
    expect(el.textContent).toContain('Không nhận ra tỉnh: X');
    const review = el.querySelectorAll('[data-cy="reviewBox"]');
    expect(review.length).toBe(1);
    // SĐT giống nhau dù viết khác → dòng so sánh "giống"
    expect(review[0].querySelector('tr.cmp-same')).not.toBeNull();
    expect(review[0].querySelector('[data-cy="resolveMerge"]')).not.toBeNull();
  });

  it('merges a suspect row, then reloads the batch and rows', async () => {
    const { harness, el } = await setup(BATCH);
    vi.spyOn(service, 'resolve').mockReturnValue(of({ ...SUSPECT.row, status: 'MERGED' }));
    (el.querySelector('[data-cy="resolveMerge"]') as HTMLButtonElement).click();
    harness.detectChanges();
    expect(service.resolve).toHaveBeenCalledWith(101, 'MERGE', undefined);
    expect(service.batch).toHaveBeenCalledTimes(2);
    expect(service.rows).toHaveBeenCalledTimes(2);
  });

  it('dry run: no review buttons, explains why', async () => {
    const { el } = await setup({ ...BATCH, dryRun: true });
    expect(el.querySelector('[data-cy="resolveMerge"]')).toBeNull();
    expect(el.querySelector('[data-cy="publishBox"]')).toBeNull();
  });

  it('filter buttons reload rows with the status', async () => {
    const { harness, el } = await setup(BATCH);
    (el.querySelector('[data-cy="filter-SUSPECT"]') as HTMLButtonElement).click();
    harness.detectChanges();
    expect(service.rows).toHaveBeenLastCalledWith(7, 'SUSPECT', 0, 50, '', null);
  });

  it('publishes drafts only after confirming', async () => {
    const { harness, el } = await setup(BATCH);
    vi.spyOn(service, 'publish').mockReturnValue(of(1));
    (el.querySelector('[data-cy="publishButton"]') as HTMLButtonElement).click();
    harness.detectChanges();
    expect(service.publish).not.toHaveBeenCalled();
    (el.querySelector('[data-cy="publishConfirm"]') as HTMLButtonElement).click();
    harness.detectChanges();
    expect(service.publish).toHaveBeenCalledWith(7);
  });

  it('hides the publish button once no drafts are left, keeps the done notice', async () => {
    vi.mocked(service.drafts).mockReturnValue(of(0));
    const { el } = await setup(BATCH);
    expect(service.drafts).toHaveBeenCalledWith(7);
    expect(el.querySelector('[data-cy="publishBox"]')).toBeNull();
    expect(el.querySelector('[data-cy="doneNotice"]')).not.toBeNull();
  });

  it('offers the real import on the latest dry run of the Yellow Pages API, then opens the new batch', async () => {
    const dry = { ...BATCH, dryRun: true, source: 'api:yp-directus' };
    vi.spyOn(service, 'batches').mockReturnValue(of(new HttpResponse({ body: [dry] })));
    vi.spyOn(service, 'pullSummary').mockReturnValue(of({ stored: 5, pending: 2 }));
    vi.spyOn(service, 'importDirectus').mockReturnValue(of({ ...BATCH, id: 8 }));
    const { harness, el } = await setup(dry);
    (el.querySelector('[data-cy="importButton"]') as HTMLButtonElement).click();
    harness.detectChanges();
    expect(service.importDirectus).not.toHaveBeenCalled();
    (el.querySelector('[data-cy="importConfirm"]') as HTMLButtonElement).click();
    expect(service.importDirectus).toHaveBeenCalledWith(false);
  });

  it('hides the real import once nothing is left to import', async () => {
    const dry = { ...BATCH, dryRun: true, source: 'api:yp-directus' };
    vi.spyOn(service, 'batches').mockReturnValue(of(new HttpResponse({ body: [dry] })));
    vi.spyOn(service, 'pullSummary').mockReturnValue(of({ stored: 5, pending: 0 }));
    const { el } = await setup(dry);
    expect(el.querySelector('[data-cy="importBox"]')).toBeNull();
  });

  it('filters rows by changed field, then clears on the second click', async () => {
    const { harness, el } = await setup(BATCH);
    (el.querySelector('[data-cy="field-address"]') as HTMLButtonElement).click();
    harness.detectChanges();
    expect(service.rows).toHaveBeenLastCalledWith(7, null, 0, 50, '', 'address');
    (el.querySelector('[data-cy="field-address"]') as HTMLButtonElement).click();
    expect(service.rows).toHaveBeenLastCalledWith(7, null, 0, 50, '', null);
  });

  it('separates overwritten from filled fields and shows old and new values', async () => {
    vi.mocked(service.rows).mockReturnValue(
      of(
        new HttpResponse({
          body: [
            {
              ...MERGED,
              row: { ...MERGED.row, filledFields: 'address,phone', updatedFields: 'address' },
              changes: {
                fields: [
                  { field: 'address', oldValue: 'Địa chỉ cũ', newValue: 'Địa chỉ mới', updated: true },
                  { field: 'phone', oldValue: null, newValue: '0901', updated: false },
                  { field: 'locationJson', oldValue: '{}', newValue: '{"ward":"A"}', updated: true },
                ],
                categories: [],
                locations: [],
              },
            },
          ],
          headers: new HttpHeaders({ 'X-Total-Count': '1' }),
        }),
      ),
    );
    const { harness, el } = await setup(BATCH);
    expect(el.querySelector('[data-cy="updatedTags"]')).not.toBeNull();
    expect(el.querySelector('[data-cy="filledTags"]')).not.toBeNull();
    (el.querySelector('[data-cy="toggleChanges"]') as HTMLButtonElement).click();
    harness.detectChanges();
    const changes = el.querySelector('[data-cy="changesRow"]') as HTMLElement;
    expect(changes.textContent).toContain('Địa chỉ cũ');
    expect(changes.textContent).toContain('Địa chỉ mới');
    expect(changes.querySelectorAll('td.import-diff-new').length).toBe(2);
  });

  it('undoes a finished real import only after confirming', async () => {
    const { harness, el } = await setup(BATCH);
    vi.spyOn(service, 'undo').mockReturnValue(of({ ...BATCH, status: 'UNDOING' }));
    (el.querySelector('[data-cy="undoButton"]') as HTMLButtonElement).click();
    harness.detectChanges();
    expect(service.undo).not.toHaveBeenCalled();
    (el.querySelector('[data-cy="undoConfirm"]') as HTMLButtonElement).click();
    harness.detectChanges();
    expect(service.undo).toHaveBeenCalledWith(7);
    expect(el.querySelector('[data-cy="undoingNotice"]')).not.toBeNull();
  });

  it('cancels a running import', async () => {
    vi.spyOn(service, 'cancel').mockReturnValue(of(undefined));
    const { harness, el } = await setup({ ...BATCH, status: 'RUNNING' });
    (el.querySelector('[data-cy="cancelRun"]') as HTMLButtonElement).click();
    harness.detectChanges();
    expect(service.cancel).toHaveBeenCalledWith(7);
  });

  it('dry run: no done notice, does not ask for drafts', async () => {
    const { el } = await setup({ ...BATCH, dryRun: true });
    expect(service.drafts).not.toHaveBeenCalled();
    expect(el.querySelector('[data-cy="doneNotice"]')).toBeNull();
  });
});
