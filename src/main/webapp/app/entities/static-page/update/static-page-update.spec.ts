import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HttpResponse } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { Subject, from, of } from 'rxjs';

import { UserService } from 'app/entities/user/service/user.service';
import { IUser } from 'app/entities/user/user.model';
import { StaticPageService } from '../service/static-page.service';
import { IStaticPage } from '../static-page.model';

import { StaticPageFormService } from './static-page-form.service';
import { StaticPageUpdate } from './static-page-update';

describe('StaticPage Management Update Component', () => {
  let comp: StaticPageUpdate;
  let fixture: ComponentFixture<StaticPageUpdate>;
  let activatedRoute: ActivatedRoute;
  let staticPageFormService: StaticPageFormService;
  let staticPageService: StaticPageService;
  let userService: UserService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            params: from([{}]),
          },
        },
      ],
    });

    fixture = TestBed.createComponent(StaticPageUpdate);
    activatedRoute = TestBed.inject(ActivatedRoute);
    staticPageFormService = TestBed.inject(StaticPageFormService);
    staticPageService = TestBed.inject(StaticPageService);
    userService = TestBed.inject(UserService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call StaticPage query and add missing value', () => {
      const staticPage: IStaticPage = { id: 24334 };
      const parent: IStaticPage = { id: 24576 };
      staticPage.parent = parent;

      const staticPageCollection: IStaticPage[] = [{ id: 24576 }];
      vi.spyOn(staticPageService, 'query').mockReturnValue(of(new HttpResponse({ body: staticPageCollection })));
      const additionalStaticPages = [parent];
      const expectedCollection: IStaticPage[] = [...additionalStaticPages, ...staticPageCollection];
      vi.spyOn(staticPageService, 'addStaticPageToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ staticPage });
      comp.ngOnInit();

      expect(staticPageService.query).toHaveBeenCalled();
      expect(staticPageService.addStaticPageToCollectionIfMissing).toHaveBeenCalledWith(
        staticPageCollection,
        ...additionalStaticPages.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.staticPagesSharedCollection()).toEqual(expectedCollection);
    });

    it('should call User query and add missing value', () => {
      const staticPage: IStaticPage = { id: 24334 };
      const author: IUser = { id: 3944 };
      staticPage.author = author;

      const userCollection: IUser[] = [{ id: 3944 }];
      vi.spyOn(userService, 'query').mockReturnValue(of(new HttpResponse({ body: userCollection })));
      const additionalUsers = [author];
      const expectedCollection: IUser[] = [...additionalUsers, ...userCollection];
      vi.spyOn(userService, 'addUserToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ staticPage });
      comp.ngOnInit();

      expect(userService.query).toHaveBeenCalled();
      expect(userService.addUserToCollectionIfMissing).toHaveBeenCalledWith(
        userCollection,
        ...additionalUsers.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.usersSharedCollection()).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const staticPage: IStaticPage = { id: 24334 };
      const parent: IStaticPage = { id: 24576 };
      staticPage.parent = parent;
      const author: IUser = { id: 3944 };
      staticPage.author = author;

      activatedRoute.data = of({ staticPage });
      comp.ngOnInit();

      expect(comp.staticPagesSharedCollection()).toContainEqual(parent);
      expect(comp.usersSharedCollection()).toContainEqual(author);
      expect(comp.staticPage).toEqual(staticPage);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<IStaticPage>();
      const staticPage = { id: 24576 };
      vi.spyOn(staticPageFormService, 'getStaticPage').mockReturnValue(staticPage);
      vi.spyOn(staticPageService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ staticPage });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(staticPage);
      saveSubject.complete();

      // THEN
      expect(staticPageFormService.getStaticPage).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(staticPageService.update).toHaveBeenCalledWith(expect.objectContaining(staticPage));
      expect(comp.isSaving()).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<IStaticPage>();
      const staticPage = { id: 24576 };
      vi.spyOn(staticPageFormService, 'getStaticPage').mockReturnValue({ id: null });
      vi.spyOn(staticPageService, 'create').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ staticPage: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(staticPage);
      saveSubject.complete();

      // THEN
      expect(staticPageFormService.getStaticPage).toHaveBeenCalled();
      expect(staticPageService.create).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<IStaticPage>();
      const staticPage = { id: 24576 };
      vi.spyOn(staticPageService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ staticPage });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(staticPageService.update).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareStaticPage', () => {
      it('should forward to staticPageService', () => {
        const entity = { id: 24576 };
        const entity2 = { id: 24334 };
        vi.spyOn(staticPageService, 'compareStaticPage');
        comp.compareStaticPage(entity, entity2);
        expect(staticPageService.compareStaticPage).toHaveBeenCalledWith(entity, entity2);
      });
    });

    describe('compareUser', () => {
      it('should forward to userService', () => {
        const entity = { id: 3944 };
        const entity2 = { id: 6275 };
        vi.spyOn(userService, 'compareUser');
        comp.compareUser(entity, entity2);
        expect(userService.compareUser).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
