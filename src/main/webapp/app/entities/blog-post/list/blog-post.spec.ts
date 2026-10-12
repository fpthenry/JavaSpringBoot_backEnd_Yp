import { MockInstance, afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import { FaIconLibrary } from '@fortawesome/angular-fontawesome';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap/modal';
import { provideTranslateService } from '@ngx-translate/core';
import { Subject, of } from 'rxjs';

import { fontAwesomeIcons } from 'app/config/font-awesome-icons';
import { sampleWithRequiredData } from '../blog-post.test-samples';
import { BlogPostService } from '../service/blog-post.service';

import { BlogPost } from './blog-post';

vi.useFakeTimers();

describe('BlogPost Management Component', () => {
  let httpMock: HttpTestingController;
  let comp: BlogPost;
  let fixture: ComponentFixture<BlogPost>;
  let service: BlogPostService;
  let routerNavigateSpy: MockInstance;
  let loadFilterOptions: MockInstance;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            data: of({
              defaultSort: 'id,asc',
            }),
            queryParamMap: of(
              convertToParamMap({
                page: '1',
                size: '1',
                sort: 'id,desc',
                'filter[someId.in]': 'dc4279ea-cfb9-11ec-9d64-0242ac120002',
              }),
            ),
            snapshot: {
              data: { defaultSort: 'id,asc' },
              queryParams: {},
              queryParamMap: convertToParamMap({
                page: '1',
                size: '1',
                sort: 'id,desc',
                'filter[someId.in]': 'dc4279ea-cfb9-11ec-9d64-0242ac120002',
              }),
            },
          },
        },
      ],
    });

    // Sửa tay: danh mục, thẻ, số bài theo trạng thái được đọc riêng khi mở trang; các test gốc chỉ đếm request danh sách
    loadFilterOptions = vi
      .spyOn(BlogPost.prototype as unknown as { loadFilterOptions(): void }, 'loadFilterOptions')
      .mockImplementation(() => undefined);
    fixture = TestBed.createComponent(BlogPost);
    comp = fixture.componentInstance;
    service = TestBed.inject(BlogPostService);
    routerNavigateSpy = vi.spyOn(comp.router, 'navigate');

    const library = TestBed.inject(FaIconLibrary);
    library.addIcons(...fontAwesomeIcons);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    httpMock.verify();
  });

  it('should call load all on init', async () => {
    // WHEN
    TestBed.tick();
    const req = httpMock.expectOne({ method: 'GET' });
    req.flush([{ id: 11641 }], { headers: { link: '<http://localhost/api/foo?page=1&size=20>; rel="next"' } });
    await vi.runAllTimersAsync();

    // THEN
    expect(comp.isLoading()).toEqual(false);
    expect(comp.blogPosts()[0]).toEqual(expect.objectContaining({ id: 11641 }));
  });

  it('should cancel previous requests when loading a new page', async () => {
    // WHEN
    TestBed.tick();
    const req = httpMock.expectOne({ method: 'GET' });
    await vi.runAllTimersAsync();

    comp.page.set(3);
    comp.load();
    await vi.runAllTimersAsync();
    const req2 = httpMock.expectOne({ method: 'GET' });
    req2.flush([{ id: 11641 }], { headers: { link: '<http://localhost/api/foo?page=1&size=20>; rel="next"' } });
    await vi.runAllTimersAsync();

    // THEN
    expect(req.cancelled).toBeTruthy();
    expect(comp.isLoading()).toEqual(false);
    expect(comp.blogPosts()[0]).toEqual(expect.objectContaining({ id: 11641 }));
  });

  it('should not fail on resource error state', async () => {
    // GIVEN - first load triggers an HTTP error
    TestBed.tick();
    const errorReq = httpMock.expectOne({ method: 'GET' });
    errorReq.flush('error', { status: 500, statusText: 'Server Error' });
    await vi.runAllTimersAsync();

    // THEN - loading state was reset and list is empty
    expect(comp.isLoading()).toBe(false);
    expect(comp.blogPosts()).toEqual([]);

    // WHEN - second load should still work
    comp.load();
    TestBed.tick();
    const successReq = httpMock.expectOne({ method: 'GET' });
    successReq.flush([{ id: 11641 }], { headers: { link: '<http://localhost/api/foo?page=1&size=20>; rel="next"' } });
    await vi.runAllTimersAsync();

    // THEN - subscription is still alive and second load succeeds
    expect(comp.blogPosts()[0]).toEqual(expect.objectContaining({ id: 11641 }));
  });

  it('tìm kiếm: bỏ sắp xếp để xếp theo độ liên quan; xóa từ khóa thì về sắp xếp mặc định', () => {
    TestBed.tick();
    httpMock.expectOne({ method: 'GET' });

    comp.search('khuyen mai');
    expect(routerNavigateSpy).toHaveBeenLastCalledWith(
      ['./'],
      expect.objectContaining({ queryParams: expect.objectContaining({ search: 'khuyen mai', page: 1, sort: [] }) }),
    );

    comp.search('');
    expect(routerNavigateSpy).toHaveBeenLastCalledWith(
      ['./'],
      expect.objectContaining({ queryParams: expect.objectContaining({ search: '', sort: ['id,asc'] }) }),
    );
  });

  describe('trackId', () => {
    it('should forward to blogPostService', () => {
      const entity = { id: 11641 };
      vi.spyOn(service, 'getBlogPostIdentifier');
      const id = comp.trackId(entity);
      expect(service.getBlogPostIdentifier).toHaveBeenCalledWith(entity);
      expect(id).toBe(entity.id);
    });
  });

  it('should calculate the sort attribute for a non-id attribute', () => {
    // WHEN
    comp.navigateToWithComponentValues({ predicate: 'non-existing-column', order: 'asc' });

    // THEN
    expect(routerNavigateSpy).toHaveBeenLastCalledWith(
      expect.anything(),
      expect.objectContaining({
        queryParams: expect.objectContaining({
          sort: ['non-existing-column,asc'],
        }),
      }),
    );
  });

  it('should load a page', () => {
    // WHEN
    comp.navigateToPage(1);

    // THEN
    expect(routerNavigateSpy).toHaveBeenCalled();
  });

  it('should calculate the sort attribute for an id', () => {
    // WHEN
    TestBed.tick();
    httpMock.expectOne({ method: 'GET' });

    // THEN
    expect(service.blogPostsParams()).toMatchObject({ sort: ['id,desc'] });
  });

  it('should calculate the filter attribute', () => {
    // WHEN
    TestBed.tick();
    httpMock.expectOne({ method: 'GET' });

    // THEN
    expect(service.blogPostsParams()).toMatchObject({ 'someId.in': ['dc4279ea-cfb9-11ec-9d64-0242ac120002'] });
  });

  describe('delete', () => {
    let ngbModal: NgbModal;
    let deleteModalMock: any;

    beforeEach(() => {
      deleteModalMock = { componentInstance: {}, closed: new Subject() };
      // NgbModal is not a singleton using TestBed.inject.
      // ngbModal = TestBed.inject(NgbModal);
      ngbModal = (comp as unknown as { modalService: NgbModal }).modalService;
      vi.spyOn(ngbModal, 'open').mockReturnValue(deleteModalMock);
    });

    it('on confirm should call load', () => {
      // GIVEN
      vi.spyOn(comp, 'load');

      // WHEN
      comp.delete(sampleWithRequiredData);
      deleteModalMock.closed.next('deleted');

      // THEN
      expect(ngbModal.open).toHaveBeenCalled();
      expect(comp.load).toHaveBeenCalled();
    });

    it('on dismiss should call load', () => {
      // GIVEN
      vi.spyOn(comp, 'load');

      // WHEN
      comp.delete(sampleWithRequiredData);
      deleteModalMock.closed.next();

      // THEN
      expect(ngbModal.open).toHaveBeenCalled();
      expect(comp.load).not.toHaveBeenCalled();
    });
  });
  describe('Sửa tay: lọc trạng thái, danh mục, thẻ', () => {
    it('filters go to the Elasticsearch search API and to the URL', () => {
      TestBed.tick();
      httpMock.expectOne({ method: 'GET' }).flush([]);

      comp.statusFilter.set('draft');
      comp.categoryFilter.set(1500);
      comp.tagFilter.set(7);
      comp.load();
      TestBed.tick();
      const req = httpMock.expectOne(request => request.url.endsWith('api/blog-posts/_search'));
      expect(req.request.params.get('status')).toBe('draft');
      expect(req.request.params.get('categoryId')).toBe('1500');
      expect(req.request.params.get('tagId')).toBe('7');
      req.flush([]);

      comp.setStatus('publish');
      expect(routerNavigateSpy).toHaveBeenLastCalledWith(
        ['./'],
        expect.objectContaining({ queryParams: expect.objectContaining({ page: 1, status: 'publish', categoryId: 1500, tagId: 7 }) }),
      );
    });

    it('while filtering only date and views are sortable; text sort is dropped', () => {
      expect(comp.sortable('title')).toBe(true);
      comp.sortState.set({ predicate: 'title', order: 'asc' });
      comp.setCategory(1500);
      expect(comp.searching()).toBe(true);
      expect(comp.sortable('title')).toBe(false);
      expect(comp.sortable('publishedAt')).toBe(true);
      expect(routerNavigateSpy).toHaveBeenLastCalledWith(
        ['./'],
        expect.objectContaining({ queryParams: expect.objectContaining({ sort: [] }) }),
      );
    });

    it('clearFilters removes every filter and the keyword', () => {
      comp.statusFilter.set('draft');
      comp.tagFilter.set(7);
      comp.currentSearch.set('khuyen mai');
      comp.clearFilters();
      expect(comp.hasFilters()).toBe(false);
      expect(comp.currentSearch()).toBe('');
    });

    it('loads category tree, tags and status counts once', () => {
      loadFilterOptions.mockRestore();
      TestBed.tick();
      httpMock.match(() => true).forEach(request => request.flush([]));
      (comp as unknown as { loadFilterOptions(): void }).loadFilterOptions();
      httpMock
        .expectOne(request => request.url.endsWith('api/blog-categories'))
        .flush([
          { id: 2, name: 'Khuyến mại', parent: { id: 1 } },
          { id: 1, name: 'Tin tức', parent: null },
        ]);
      httpMock.expectOne(request => request.url.endsWith('api/tags')).flush([{ id: 7, name: 'B&amp;N' }]);
      const counts = httpMock.match(request => request.url.endsWith('api/blog-posts/count'));
      expect(counts.length).toBe(3);
      counts[0].flush(2435);
      counts[1].flush(2433);
      counts[2].flush(2);
      expect(comp.categoryOptions().map(option => option.label)).toEqual(['Tin tức', '— Khuyến mại']);
      expect(comp.tagOptions()).toEqual([{ id: 7, name: 'B&N' }]);
      expect(comp.statusCounts()).toEqual({ all: 2435, publish: 2433, draft: 2 });
    });
  });
});
