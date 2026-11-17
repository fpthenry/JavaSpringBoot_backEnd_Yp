import { beforeEach, describe, expect, it, vi } from 'vitest';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';

import { FaIconLibrary } from '@fortawesome/angular-fontawesome';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap/modal';
import { provideTranslateService } from '@ngx-translate/core';
import dayjs from 'dayjs/esm';
import { BehaviorSubject, Subject, of } from 'rxjs';

import { ITEM_DELETED_EVENT } from 'app/config';
import { fontAwesomeIcons } from 'app/config/font-awesome-icons';
import { IGalleryBanner, IGalleryWithImages } from 'app/entities/gallery/gallery.model';
import { GalleryService } from 'app/entities/gallery/service/gallery.service';
import { IGalleryImage } from '../gallery-image.model';
import { GalleryImageService } from '../service/gallery-image.service';

import { GalleryImage, bannerStatus } from './gallery-image';

const now = dayjs('2026-11-17T10:00');
const banner = (patch: Partial<IGalleryBanner>): IGalleryBanner => ({
  id: 1,
  uploaded: false,
  previewUrl: 'https://cdn.yp.vn/a.png',
  active: true,
  showingNow: true,
  ...patch,
});

const POSITIONS: IGalleryWithImages[] = [
  {
    id: 1,
    name: 'Đầu trang',
    active: true,
    imageCount: 3,
    images: [
      banner({ id: 1, title: 'Tết', displayOrder: 1 }),
      banner({ id: 2, title: 'Hè', displayOrder: 0, active: false }),
      banner({ id: 3, title: 'Năm sau', startAt: dayjs().add(30, 'day').toISOString() }),
    ],
  },
  { id: 2, name: 'Chân trang', active: true, imageCount: 1, images: [banner({ id: 4, title: 'Cũ', endAt: '2020-01-01T00:00:00Z' })] },
];

describe('bannerStatus', () => {
  it('tells live, scheduled, expired, off and missing image apart', () => {
    expect(bannerStatus(banner({}), true, now)).toBe('showing');
    expect(bannerStatus(banner({ active: false }), true, now)).toBe('off');
    expect(bannerStatus(banner({}), false, now)).toBe('off');
    expect(bannerStatus(banner({ previewUrl: null }), true, now)).toBe('noImage');
    expect(bannerStatus(banner({ startAt: '2026-12-01T00:00:00Z' }), true, now)).toBe('scheduled');
    expect(bannerStatus(banner({ endAt: '2026-11-01T00:00:00Z' }), true, now)).toBe('expired');
  });
});

describe('GalleryImage list (trang quản lý banner)', () => {
  let comp: GalleryImage;
  let fixture: ComponentFixture<GalleryImage>;
  let queryParamMap: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let galleryImageService: GalleryImageService;

  beforeEach(() => {
    queryParamMap = new BehaviorSubject(convertToParamMap({}));
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { queryParamMap } },
      ],
    });
    TestBed.inject(FaIconLibrary).addIcons(...fontAwesomeIcons);
    vi.spyOn(TestBed.inject(GalleryService), 'withImages').mockReturnValue(of(POSITIONS));
    galleryImageService = TestBed.inject(GalleryImageService);
    fixture = TestBed.createComponent(GalleryImage);
    comp = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads every banner of every position, ordered by display order', () => {
    expect(comp.banners().map(item => item.id)).toEqual([2, 1, 3, 4]);
    expect(comp.showingTotal()).toBe(1);
    expect(comp.showingByPosition().get(1)).toBe(1);
  });

  it('filters by position and status from the URL', () => {
    queryParamMap.next(convertToParamMap({ position: '1' }));
    expect(comp.visible().map(item => item.id)).toEqual([2, 1, 3]);
    expect(comp.statusCounts()).toEqual({ showing: 1, scheduled: 1, expired: 0, off: 1, noImage: 0 });

    queryParamMap.next(convertToParamMap({ position: '1', status: 'scheduled' }));
    expect(comp.visible().map(item => item.id)).toEqual([3]);

    queryParamMap.next(convertToParamMap({ status: 'expired' }));
    expect(comp.visible().map(item => item.id)).toEqual([4]);
  });

  it('searches by name', () => {
    queryParamMap.next(convertToParamMap({ q: 'tết' }));
    expect(comp.visible().map(item => item.id)).toEqual([1]);
  });

  it('writes the filters to the URL', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    comp.setPosition(2);
    expect(navigate).toHaveBeenCalledWith([], expect.objectContaining({ queryParams: { position: 2 }, queryParamsHandling: 'merge' }));
  });

  it('turns a banner on/off and reloads', () => {
    const patch = vi.spyOn(galleryImageService, 'partialUpdate').mockReturnValue(of({ id: 2 } as IGalleryImage));
    comp.toggleActive(comp.banners()[0]);
    expect(patch).toHaveBeenCalledWith({ id: 2, active: true });
    expect(comp.toggling()).toBeNull();
  });

  it('reloads after a confirmed delete', () => {
    const closed = new Subject<string>();
    vi.spyOn(TestBed.inject(NgbModal), 'open').mockReturnValue({ componentInstance: {}, closed } as unknown as NgbModalRef);
    const withImages = vi.spyOn(TestBed.inject(GalleryService), 'withImages');
    withImages.mockClear();
    comp.delete(comp.banners()[1]);
    closed.next(ITEM_DELETED_EVENT);
    expect(withImages).toHaveBeenCalledTimes(1);
  });
});
