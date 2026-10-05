import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpResponse } from '@angular/common/http';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import dayjs from 'dayjs/esm';
import { of } from 'rxjs';

import { BlogCategoryService } from 'app/entities/blog-category/service/blog-category.service';
import { TagService } from 'app/entities/tag/service/tag.service';
import { IBlogPost } from '../blog-post.model';
import { BlogPostService } from '../service/blog-post.service';

import { BlogPostEditor, slugify } from './blog-post-editor';

const CATEGORIES = [
  { id: 1, name: 'Tin tức', parent: null },
  { id: 2, name: 'Sự kiện', parent: null },
  { id: 3, name: 'Ngày hội khuyến mại', parent: { id: 2, name: 'Sự kiện' } },
  { id: 4, name: 'Tháng khuyến mại', parent: { id: 2, name: 'Sự kiện' } },
];

describe('slugify', () => {
  it('bỏ dấu tiếng Việt, đ -> d, nối bằng gạch ngang', () => {
    expect(slugify('Tâm lý học chăn nuôi gia cầm: Khi sự thoải mái')).toBe('tam-ly-hoc-chan-nuoi-gia-cam-khi-su-thoai-mai');
    expect(slugify('  Đồng Nai — Điện   máy!! ')).toBe('dong-nai-dien-may');
    expect(slugify(null)).toBe('');
  });
});

describe('BlogPostEditor', () => {
  let fixture: ComponentFixture<BlogPostEditor>;
  let comp: BlogPostEditor;
  let blogPostService: BlogPostService;
  let routeData: IBlogPost | null;

  const setup = (post: IBlogPost | null): void => {
    routeData = post;
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { data: of({ blogPost: routeData }) } },
      ],
    });
    vi.spyOn(TestBed.inject(BlogCategoryService), 'query').mockReturnValue(of(new HttpResponse({ body: CATEGORIES })));
    vi.spyOn(TestBed.inject(TagService), 'query').mockReturnValue(of(new HttpResponse({ body: [{ id: 9, name: 'khuyến mãi' }] })));
    blogPostService = TestBed.inject(BlogPostService);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(BlogPostEditor);
    comp = fixture.componentInstance;
  };

  beforeEach(() => TestBed.resetTestingModule());

  it('dựng cây danh mục: cha trước, con thụt lề, sắp theo tên', () => {
    setup(null);
    expect(comp.categoryRows().map(r => `${'-'.repeat(r.depth)}${r.category.name}`)).toEqual([
      'Sự kiện',
      '-Ngày hội khuyến mại',
      '-Tháng khuyến mại',
      'Tin tức',
    ]);
  });

  it('lọc danh mục không dấu, giữ cả danh mục cha', () => {
    setup(null);
    comp.categoryFilter.set('thang khuyen');
    expect(comp.categoryRows().map(r => r.category.name)).toEqual(['Sự kiện', 'Tháng khuyến mại']);
  });

  it('bài mới: tự tạo slug, xuất bản gửi đúng danh mục, thẻ, ảnh đại diện, ngày đăng', () => {
    setup(null);
    const create = vi.spyOn(blogPostService, 'create').mockReturnValue(of({ id: 100 }));
    comp.editForm.controls.title.setValue('Ngày hội khuyến mại tháng 11');
    comp.editForm.controls.content.setValue('<p>Nội dung <img src="https://yp.com.vn/a.jpg"></p>');
    comp.toggleCategory(3);
    comp.toggleCategory(1);
    comp.toggleCategory(1);
    comp.toggleTag(9);
    comp.useAsThumbnail(comp.contentImages()[0]);

    comp.publish();

    const sent = create.mock.calls[0][0];
    expect(sent.id).toBeNull();
    expect(sent.slug).toBe('ngay-hoi-khuyen-mai-thang-11');
    expect(sent.status).toBe('publish');
    expect(sent.categories).toEqual([{ id: 3 }]);
    expect(sent.tags).toEqual([{ id: 9 }]);
    expect(sent.thumbnail).toBe('https://yp.com.vn/a.jpg');
    expect(sent.publishedAt).toBeTruthy();
    expect(sent.wpId).toBeUndefined();
  });

  it('thiếu tiêu đề thì không lưu', () => {
    setup(null);
    const create = vi.spyOn(blogPostService, 'create');
    comp.saveDraft();
    expect(create).not.toHaveBeenCalled();
  });

  it('sửa bài từ WordPress: nạp sẵn danh mục đã tích, giữ wpId, slug và lưu nháp', () => {
    setup({
      id: 5,
      wpId: 14347989,
      title: 'Bài cũ',
      slug: 'bai-cu-wp',
      status: 'publish',
      publishedAt: dayjs('2026-10-05T14:09'),
      categories: [{ id: 4, name: 'Tháng khuyến mại' }],
      tags: [],
      viewCount: 12,
    });
    const update = vi.spyOn(blogPostService, 'update').mockReturnValue(of({ id: 5 }));
    expect(comp.isCategoryChecked(4)).toBe(true);
    comp.editForm.controls.title.setValue('Bài cũ (sửa)');

    comp.saveDraft();

    const sent = update.mock.calls[0][0];
    expect(sent.wpId).toBe(14347989);
    expect(sent.slug).toBe('bai-cu-wp');
    expect(sent.status).toBe('draft');
    expect(sent.viewCount).toBe(12);
    expect(sent.categories).toEqual([{ id: 4 }]);
  });
});
