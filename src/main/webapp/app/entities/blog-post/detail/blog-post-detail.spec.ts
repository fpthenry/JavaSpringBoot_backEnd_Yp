import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { FaIconLibrary } from '@fortawesome/angular-fontawesome';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap/modal';
import { provideTranslateService } from '@ngx-translate/core';
import { Subject, of } from 'rxjs';

import { ITEM_DELETED_EVENT } from 'app/config';
import { fontAwesomeIcons } from 'app/config/font-awesome-icons';
import { IBlogPost } from '../blog-post.model';

import { BlogPostDetail } from './blog-post-detail';

const POST: IBlogPost = {
  id: 2442,
  title: 'Tâm lý học chăn nuôi gia cầm &amp; chất lượng',
  slug: 'tam-ly-hoc-chan-nuoi',
  status: 'publish',
  authorName: 'ADMIN HCM',
  viewCount: 1234,
  thumbnail: 'https://yp.com.vn/wp-content/uploads/2026/10/4d.jpg',
  excerpt:
    '<p>Trong nền nông nghiệp hiện đại&#8230; <a class="view-article" href="https://yp.com.vn/tam-ly-hoc-chan-nuoi/">Xem bài viết</a></p>',
  content:
    '[vc_row][vc_column_text]<p data-path-to-node="3">Đoạn một <b>quan trọng</b>.</p><img src="https://yp.com.vn/a.jpg" alt="Gà">[/vc_column_text][/vc_row]',
  categories: [{ id: 1550, name: 'Chăn nuôi - gia cầm gia súc' }],
  tags: [],
};

describe('BlogPost Management Detail Component', () => {
  let comp: BlogPostDetail;
  let fixture: ComponentFixture<BlogPostDetail>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        provideRouter(
          [
            {
              path: '**',
              loadComponent: () => import('./blog-post-detail').then(m => m.BlogPostDetail),
              resolve: { blogPost: () => of({ id: 2442 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    });
    TestBed.inject(FaIconLibrary).addIcons(...fontAwesomeIcons);
    fixture = TestBed.createComponent(BlogPostDetail);
    comp = fixture.componentInstance;
  });

  const show = (post: IBlogPost): HTMLElement => {
    fixture.componentRef.setInput('blogPost', post);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  };

  it('should load blogPost on init', async () => {
    const harness = await RouterTestingHarness.create();
    const instance = await harness.navigateByUrl('/', BlogPostDetail);
    expect(instance.blogPost()).toEqual(expect.objectContaining({ id: 2442 }));
  });

  it('shows the article: decoded title, lead without the WordPress link, body without shortcodes', () => {
    const root = show(POST);
    expect(comp.title()).toBe('Tâm lý học chăn nuôi gia cầm & chất lượng');
    expect(comp.lead()).toBe('Trong nền nông nghiệp hiện đại…');
    expect(comp.publicUrl()).toBe('https://yp.com.vn/tam-ly-hoc-chan-nuoi/');
    expect(root.querySelector('[data-cy=viewOnSite]')?.getAttribute('href')).toBe('https://yp.com.vn/tam-ly-hoc-chan-nuoi/');
    const body = root.querySelector('[data-cy=articleBody]')?.innerHTML ?? '';
    expect(body).not.toContain('[vc_');
    expect(body).not.toContain('data-path-to-node');
    expect(body).toContain('<b>quan trọng</b>');
    expect(comp.stats()).toEqual({ words: 4, minutes: 1 });
  });

  it('hides an auto-generated excerpt that only repeats the start of the article', () => {
    let root = show(POST);
    expect(comp.showLead()).toBe(true);
    expect(root.querySelector('[data-cy=lead]')).not.toBeNull();
    root = show({ ...POST, excerpt: '<p>Đoạn một quan&#8230; <a class="view-article" href="https://yp.com.vn/x/">Xem bài viết</a></p>' });
    expect(comp.showLead()).toBe(false);
    expect(root.querySelector('[data-cy=lead]')).toBeNull();
  });

  it('a cover image that fails to load removes the whole cover box', () => {
    const root = show(POST);
    expect(root.querySelector('[data-cy=cover]')).not.toBeNull();
    root.querySelector('[data-cy=cover] img')?.dispatchEvent(new Event('error'));
    fixture.detectChanges();
    expect(root.querySelector('[data-cy=cover]')).toBeNull();
  });

  it('suggests what is missing (tags here), and a too long title', () => {
    show(POST);
    expect(comp.suggestions().map(item => item.key)).toEqual(['tag']);
    show({ ...POST, title: 'x'.repeat(80), tags: [{ id: 1, name: 'bao bì' }], excerpt: null, thumbnail: null });
    expect(comp.suggestions().map(item => item.key)).toEqual(['excerpt', 'thumbnail', 'titleLength']);
  });

  it('categories and tags link to the filtered list', () => {
    const root = show({ ...POST, tags: [{ id: 7, name: 'bao bì' }] });
    expect(comp.categoryQueryParams(1550)).toEqual({ categoryId: 1550 });
    expect(comp.tagQueryParams(7)).toEqual({ tagId: 7 });
    expect(root.querySelector('[data-cy=tags]')?.textContent).toContain('#bao bì');
  });

  it('a broken or blocked image becomes a labelled box', () => {
    const root = show(POST);
    const image = document.createElement('img');
    image.alt = 'Gà';
    root.querySelector('[data-cy=articleBody]')?.appendChild(image);
    (comp as unknown as { replaceBrokenImage(img: HTMLImageElement): void }).replaceBrokenImage(image);
    expect(root.querySelector('.article-image-missing')?.textContent).toBe('Gà');
  });

  it('after delete goes back to the list; E opens the edit page', () => {
    const closed = new Subject<string>();
    vi.spyOn(TestBed.inject(NgbModal), 'open').mockReturnValue({ componentInstance: {}, closed } as unknown as NgbModalRef);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    show(POST);
    comp.delete(POST);
    closed.next(ITEM_DELETED_EVENT);
    expect(navigate).toHaveBeenCalledWith(['/blog-post']);
    comp.onKeydown(new KeyboardEvent('keydown', { key: 'e' }));
    expect(navigate).toHaveBeenCalledWith(['/blog-post', 2442, 'edit']);
  });
});
