import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpResponse, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { FaIconLibrary } from '@fortawesome/angular-fontawesome';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap/modal';
import { provideTranslateService } from '@ngx-translate/core';
import { Subject, of } from 'rxjs';

import { ITEM_DELETED_EVENT } from 'app/config';
import { fontAwesomeIcons } from 'app/config/font-awesome-icons';
import { ILocation } from 'app/entities/location/location.model';
import { LocationService } from 'app/entities/location/service/location.service';
import { IListing } from '../listing.model';

import { ListingDetail } from './listing-detail';

const LISTING: IListing = {
  id: 181746,
  wpId: 4872937,
  name: 'TRƯỜNG THPT YÊN LÃNG',
  status: 'publish',
  phone: '(024)38164331',
  email: 'a@x.com;b@y.com',
  website: 'www.yenlang.edu.vn',
  addressNew: 'THÔN BỒNG MẠC, Xã Yên Lãng, Thành phố Hà Nội',
  locationJson: '{"province":"Thành phố Hà Nội","ward":"Xã Yên Lãng","coordinate":"21.1721:105.662"}',
  categories: [{ id: 329, name: 'TRƯỜNG TIỂU HỌC - THCS &amp; THPT' }],
  locations: [
    { id: 2, name: 'Thành phố Hà Nội' },
    { id: 12112, name: 'Xã Yên Lãng' },
  ],
};

const LOCATION_DETAILS: ILocation[] = [
  { id: 2, name: 'Thành phố Hà Nội', slug: 'thanh-pho-ha-noi-01', parent: null },
  { id: 12112, name: 'Xã Yên Lãng', slug: 'xa-yen-lang-08980-2025', parent: { id: 11960, name: 'Thành phố Hà Nội' } },
];

describe('Listing Management Detail Component', () => {
  let comp: ListingDetail;
  let fixture: ComponentFixture<ListingDetail>;
  let locationService: LocationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter(
          [
            {
              path: '**',
              loadComponent: () => import('./listing-detail').then(m => m.ListingDetail),
              resolve: { listing: () => of({ id: 14276 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    });
    TestBed.inject(FaIconLibrary).addIcons(...fontAwesomeIcons);
    locationService = TestBed.inject(LocationService);
    vi.spyOn(locationService, 'query').mockReturnValue(of(new HttpResponse({ body: LOCATION_DETAILS })));
    fixture = TestBed.createComponent(ListingDetail);
    comp = fixture.componentInstance;
  });

  afterEach(() => vi.useRealTimers());

  const show = (listing: IListing): void => {
    fixture.componentRef.setInput('listing', listing);
    fixture.detectChanges();
  };

  it('should load listing on init', async () => {
    const harness = await RouterTestingHarness.create();
    const instance = await harness.navigateByUrl('/', ListingDetail);
    expect(instance.listing()).toEqual(expect.objectContaining({ id: 14276 }));
  });

  it('turns contacts into links: several emails, phone tel:, website with https', () => {
    show(LISTING);
    expect(comp.phones()).toEqual(['(024)38164331']);
    expect(comp.telHref('(024)38164331')).toBe('tel:02438164331');
    expect(comp.emails()).toEqual(['a@x.com', 'b@y.com']);
    expect(comp.website()).toBe('https://www.yenlang.edu.vn');
    const links = [...(fixture.nativeElement as HTMLElement).querySelectorAll('a')].map(a => a.getAttribute('href'));
    expect(links).toContain('tel:02438164331');
    expect(links).toContain('mailto:b@y.com');
  });

  it('map link uses the stored coordinate text as is', () => {
    show(LISTING);
    expect(comp.place()?.coordinate).toBe('21.1721:105.662');
    expect(comp.mapUrl()).toBe('https://www.google.com/maps?q=21.1721,105.662');
  });

  it('loads location details in one request: parent and new/old unit', () => {
    show(LISTING);
    expect(locationService.query).toHaveBeenCalledWith({ 'id.in': '2,12112', size: 2 });
    expect(comp.linkedLocations()).toEqual([
      { id: 2, name: 'Thành phố Hà Nội', parentName: null, isNew: false },
      { id: 12112, name: 'Xã Yên Lãng', parentName: 'Thành phố Hà Nội', isNew: true },
    ]);
    expect(comp.hasNewUnit()).toBe(true);
  });

  it('completeness lists what is missing, with the edit field to jump to', () => {
    show(LISTING);
    expect(comp.missing().map(check => check.field)).toEqual(['taxCode', 'description']);
    expect(comp.percent()).toBe(75);
    expect(comp.editQueryParams('taxCode')).toEqual({ focus: 'taxCode' });
    const text = (fixture.nativeElement as HTMLElement).querySelector('[data-cy=completeness]')?.textContent ?? '';
    expect(text).toContain('75%');
  });

  it('decodes &amp; in industry names and links chips to the filtered list', () => {
    show(LISTING);
    const chips = [...(fixture.nativeElement as HTMLElement).querySelectorAll('a.detail-chip')].map(a => a.textContent.trim());
    expect(chips).toContain('TRƯỜNG TIỂU HỌC - THCS & THPT');
    expect(comp.categoryQueryParams(329)).toEqual({ 'filter[categoryTreeId.equals]': 329 });
    expect(comp.locationQueryParams(12112)).toEqual({ 'filter[locationTreeId.equals]': 12112 });
  });

  it('hero: initials skip company-type words; only filled business fields are shown', () => {
    show({ ...LISTING, name: 'CTY TNHH TM ĐẦU TƯ HÓA DẦU VIỆT NAM', representative: 'Nguyễn Văn A', capital: ' ' });
    expect(comp.initials()).toBe('ĐT');
    expect(comp.businessFilled()).toEqual(['representative']);
    expect(comp.businessEmpty()).toContain('capital');
    const text = (fixture.nativeElement as HTMLElement).querySelector('#section-business')?.textContent ?? '';
    expect(text).toContain('Nguyễn Văn A');
  });

  it('description with only empty HTML counts as missing', () => {
    show({ ...LISTING, description: '<p> </p>' });
    expect(comp.description()).toBeNull();
    show({ ...LISTING, description: '<ul><li>DẦU NHỜN</li></ul>' });
    expect(comp.description()).toContain('DẦU NHỜN');
  });

  it('E key opens the edit form, but not while typing or with Ctrl', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    show(LISTING);
    comp.onKeydown(new KeyboardEvent('keydown', { key: 'e', ctrlKey: true }));
    const input = document.createElement('input');
    document.body.appendChild(input);
    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'e', bubbles: true }));
    input.remove();
    expect(navigate).not.toHaveBeenCalled();
    comp.onKeydown(new KeyboardEvent('keydown', { key: 'E' }));
    expect(navigate).toHaveBeenCalledWith(['/listing', 181746, 'edit']);
  });

  it('renders the section nav and quick actions', () => {
    show(LISTING);
    const root = fixture.nativeElement as HTMLElement;
    expect([...root.querySelectorAll('.section-nav-link')].map(a => a.getAttribute('href'))).toEqual([
      '#section-overview',
      '#section-contact',
      '#section-address',
      '#section-categories',
      '#section-business',
    ]);
    expect(root.querySelector('[data-cy=quickCall]')?.getAttribute('href')).toBe('tel:02438164331');
    expect(root.querySelector('[data-cy=quickMap]')?.getAttribute('href')).toBe('https://www.google.com/maps?q=21.1721,105.662');
  });

  it('after delete goes back to the list', () => {
    const closed = new Subject<string>();
    vi.spyOn(TestBed.inject(NgbModal), 'open').mockReturnValue({ componentInstance: {}, closed } as unknown as NgbModalRef);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    comp.delete(LISTING);
    closed.next(ITEM_DELETED_EVENT);
    expect(navigate).toHaveBeenCalledWith(['/listing']);
  });

  it('previousState: back inside the app, otherwise to the list', () => {
    const back = vi.spyOn(globalThis.history, 'back').mockImplementation(() => undefined);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    vi.spyOn(globalThis.history, 'state', 'get').mockReturnValue({ navigationId: 3 });
    comp.previousState();
    expect(back).toHaveBeenCalled();
    vi.spyOn(globalThis.history, 'state', 'get').mockReturnValue(null);
    comp.previousState();
    expect(navigate).toHaveBeenCalledWith(['/listing']);
  });
});
