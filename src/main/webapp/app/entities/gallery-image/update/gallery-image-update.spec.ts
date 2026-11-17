import { beforeEach, describe, expect, it, vi } from 'vitest';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import dayjs from 'dayjs/esm';
import { Subject, of } from 'rxjs';

import { IGalleryWithImages } from 'app/entities/gallery/gallery.model';
import { GalleryService } from 'app/entities/gallery/service/gallery.service';
import { IGalleryImage } from '../gallery-image.model';
import { GalleryImageService } from '../service/gallery-image.service';

import { GalleryImageUpdate } from './gallery-image-update';

const POSITIONS: IGalleryWithImages[] = [
  {
    id: 1,
    name: 'Đầu trang',
    code: 'top',
    active: true,
    imageCount: 2,
    images: [
      { id: 10, uploaded: false, displayOrder: 0, showingNow: true },
      { id: 11, uploaded: false, displayOrder: 4, showingNow: true },
    ],
  },
  { id: 2, name: 'Chân trang', code: 'footer', active: true, imageCount: 0, images: [] },
];

describe('GalleryImage Update Component (form banner tinh gọn)', () => {
  let comp: GalleryImageUpdate;
  let fixture: ComponentFixture<GalleryImageUpdate>;
  let activatedRoute: { data: unknown; snapshot: { queryParamMap: ReturnType<typeof convertToParamMap> } };
  let galleryImageService: GalleryImageService;

  beforeEach(() => {
    activatedRoute = { data: of({ galleryImage: null }), snapshot: { queryParamMap: convertToParamMap({}) } };
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: activatedRoute },
      ],
    });
    vi.spyOn(TestBed.inject(GalleryService), 'withImages').mockReturnValue(of(POSITIONS));
    galleryImageService = TestBed.inject(GalleryImageService);
    fixture = TestBed.createComponent(GalleryImageUpdate);
    comp = fixture.componentInstance;
  });

  const fill = (): void => {
    comp.editForm.patchValue({ title: '  Khuyến mãi Tết  ', imageUrl: 'https://cdn.yp.vn/tet.png', gallery: { id: 1, name: 'Đầu trang' } });
  };

  it('new banner: active, start date today, no end date', () => {
    comp.ngOnInit();
    expect(comp.isNew).toBe(true);
    expect(comp.editForm.controls.active.value).toBe(true);
    expect(comp.startDate.value).toBe(dayjs().format('YYYY-MM-DD'));
    expect(comp.endDate.value).toBe('');
  });

  it('preselects the position from ?position=', () => {
    activatedRoute.snapshot.queryParamMap = convertToParamMap({ position: '2' });
    comp.ngOnInit();
    expect(comp.editForm.controls.gallery.value).toEqual({ id: 2, name: 'Chân trang' });
  });

  it('does not save without name, image and position', () => {
    const create = vi.spyOn(galleryImageService, 'create');
    comp.ngOnInit();
    comp.save();
    expect(create).not.toHaveBeenCalled();
    expect(comp.missingImage).toBe(true);
  });

  it('rejects an end date before the start date', () => {
    comp.ngOnInit();
    comp.startDate.setValue('2026-12-10');
    comp.endDate.setValue('2026-12-01');
    expect(comp.endDate.hasError('endBeforeStart')).toBe(true);
    comp.endDate.setValue('2026-12-31');
    expect(comp.endDate.valid).toBe(true);
  });

  it('saves a new banner with day bounds, next order and alt from the name', () => {
    const create = vi.spyOn(galleryImageService, 'create').mockReturnValue(new Subject<IGalleryImage>());
    comp.ngOnInit();
    fill();
    comp.startDate.setValue('2026-12-01');
    comp.endDate.setValue('2026-12-31');

    comp.save();

    expect(create).toHaveBeenCalledTimes(1);
    const payload = create.mock.calls[0][0];
    expect(payload.title).toBe('Khuyến mãi Tết');
    expect(payload.altText).toBe('Khuyến mãi Tết');
    expect(payload.displayOrder).toBe(5);
    expect(payload.active).toBe(true);
    expect(payload.startAt?.format('YYYY-MM-DD HH:mm')).toBe('2026-12-01 00:00');
    expect(payload.endAt?.format('YYYY-MM-DD HH:mm:ss')).toBe('2026-12-31 23:59:59');
  });

  it('empty end date saves no end, empty start date saves "show now"', () => {
    const create = vi.spyOn(galleryImageService, 'create').mockReturnValue(new Subject<IGalleryImage>());
    comp.ngOnInit();
    fill();
    comp.editForm.patchValue({ gallery: { id: 2, name: 'Chân trang' } });
    comp.startDate.setValue('');

    comp.save();

    const payload = create.mock.calls[0][0];
    expect(payload.startAt).toBeNull();
    expect(payload.endAt).toBeNull();
    expect(payload.displayOrder).toBe(0);
  });

  it('edit: loads dates and keeps the existing order', () => {
    const update = vi.spyOn(galleryImageService, 'update').mockReturnValue(new Subject<IGalleryImage>());
    activatedRoute.data = of({
      galleryImage: {
        id: 7,
        title: 'Cũ',
        imageUrl: 'https://cdn.yp.vn/a.png',
        displayOrder: 3,
        active: false,
        startAt: dayjs('2026-11-01T00:00'),
        endAt: null,
        gallery: { id: 1, name: 'Đầu trang' },
      },
    });
    comp.ngOnInit();
    expect(comp.isNew).toBe(false);
    expect(comp.startDate.value).toBe('2026-11-01');
    expect(comp.showMore()).toBe(true);

    comp.save();

    expect(update).toHaveBeenCalledWith(expect.objectContaining({ id: 7, displayOrder: 3, active: false, endAt: null }));
  });
});
