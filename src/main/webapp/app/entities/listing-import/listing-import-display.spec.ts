import { describe, expect, it } from 'vitest';

import {
  compareLines,
  durationText,
  filledList,
  messageList,
  parsePairs,
  scorePercent,
  sourceCode,
  sourceKind,
  statusCount,
  statusLabelKey,
} from './listing-import-display';
import { IImportBatch } from './listing-import.model';

const batch: IImportBatch = {
  id: 1,
  source: 'file',
  dryRun: true,
  status: 'DONE',
  totalRows: 10,
  createdCount: 4,
  mergedCount: 2,
  unchangedCount: 1,
  suspectCount: 2,
  duplicateInBatchCount: 0,
  invalidCount: 1,
};

describe('listing-import-display', () => {
  it('statusLabelKey: dry run uses future tense for created/merged only', () => {
    expect(statusLabelKey('CREATED', true)).toBe('javaSpringBootBackEndApp.listingImport.status.CREATED_DRY');
    expect(statusLabelKey('CREATED', false)).toBe('javaSpringBootBackEndApp.listingImport.status.CREATED');
    expect(statusLabelKey('SUSPECT', true)).toBe('javaSpringBootBackEndApp.listingImport.status.SUSPECT');
  });

  it('statusCount maps batch counters; SKIPPED has none', () => {
    expect(statusCount(batch, 'CREATED')).toBe(4);
    expect(statusCount(batch, 'SUSPECT')).toBe(2);
    expect(statusCount(batch, 'SKIPPED')).toBeNull();
  });

  it('source kind and code', () => {
    expect(sourceKind('file')).toBe('file');
    expect(sourceKind('api:masothue')).toBe('api');
    expect(sourceKind('json:doi-tac')).toBe('json');
    expect(sourceCode('api:masothue')).toBe('masothue');
    expect(sourceCode('file')).toBeNull();
  });

  it('splits filled fields and notes', () => {
    expect(filledList('addressNew, images,')).toEqual(['addressNew', 'images']);
    expect(filledList(null)).toEqual([]);
    expect(messageList('Không nhận ra tỉnh: X; Logo không phải URL http(s), bỏ qua')).toEqual([
      'Không nhận ra tỉnh: X',
      'Logo không phải URL http(s), bỏ qua',
    ]);
  });

  it('scorePercent and durationText', () => {
    expect(scorePercent(0.7)).toBe('70%');
    expect(scorePercent(null)).toBe('');
    expect(durationText({ startedAt: '2026-10-27T03:00:00Z', finishedAt: '2026-10-27T03:00:12Z' })).toBe('12 giây');
    expect(durationText({ startedAt: '2026-10-27T03:00:00Z', finishedAt: '2026-10-27T03:03:05Z' })).toBe('3 phút 05 giây');
    expect(durationText({ startedAt: null, finishedAt: null })).toBe('');
  });

  it('compareLines marks same (ignoring accents, spaces, punctuation), different and fill', () => {
    const lines = compareLines({
      row: { id: 1, batchId: 1, rowNo: 2, status: 'SUSPECT' },
      data: { name: 'Công ty Tân Minh', phone: '(024) 3577-3348', addressNew: 'Số 4 Hoàng Ngân', email: 'a@x.vn' },
      matched: { id: 9, name: 'CÔNG TY TÂN MINH', phone: '024 35773348', email: 'b@x.vn', address: 'Thanh Xuân' },
    });
    const byField = Object.fromEntries(lines.map(l => [l.field, l.state]));
    expect(byField).toEqual({ name: 'same', phone: 'same', email: 'diff', address: 'none', addressNew: 'fill' });
    // field trống cả hai bên không hiện
    expect(lines.some(l => l.field === 'website')).toBe(false);
  });

  it('parsePairs reads "key: value" and "field = path" lines, skips bad lines', () => {
    expect(parsePairs('X-API-Key: abc:def\nAuthorization: Bearer t\nsai dòng', ':')).toEqual({
      'X-API-Key': 'abc:def',
      Authorization: 'Bearer t',
    });
    expect(parsePairs('taxCode = mst\n name = data.ten \n= x', '=')).toEqual({ taxCode: 'mst', name: 'data.ten' });
  });
});
