import { beforeEach, describe, expect, it, vi } from 'vitest';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { Subject, from, of } from 'rxjs';

import { UserManagementService } from '../service/user-management.service';
import { IUserManagement } from '../user-management.model';

import { UserManagementFormService } from './user-management-form.service';
import { UserManagementUpdate } from './user-management-update';

describe('UserManagement Management Update Component', () => {
  let comp: UserManagementUpdate;
  let fixture: ComponentFixture<UserManagementUpdate>;
  let activatedRoute: ActivatedRoute;
  let userManagementFormService: UserManagementFormService;
  let userManagementService: UserManagementService;

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

    fixture = TestBed.createComponent(UserManagementUpdate);
    activatedRoute = TestBed.inject(ActivatedRoute);
    userManagementFormService = TestBed.inject(UserManagementFormService);
    userManagementService = TestBed.inject(UserManagementService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should update editForm', () => {
      const userManagement: IUserManagement = { login: 'Anya.Schiller33' };

      activatedRoute.data = of({ userManagement });
      comp.ngOnInit();

      expect(comp.userManagement).toEqual(userManagement);
    });
  });

  /** Form mới bắt buộc họ tên, email và (khi tạo) mật khẩu. */
  const fillValidForm = (): void => {
    comp.editForm.patchValue({ firstName: 'Nguyễn Văn An', email: 'An@YP.vn' });
    comp.accessForm.controls.password.setValue('matkhau123');
  };

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<IUserManagement>();
      const userManagement = { login: 'Ozella.Kertzmann' };
      vi.spyOn(userManagementFormService, 'getUserManagement').mockReturnValue(userManagement);
      vi.spyOn(userManagementService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ userManagement });
      comp.ngOnInit();
      fillValidForm();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(userManagement);
      saveSubject.complete();

      // THEN
      expect(userManagementFormService.getUserManagement).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(userManagementService.update).toHaveBeenCalledWith(expect.objectContaining(userManagement));
      expect(comp.isSaving()).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<IUserManagement>();
      const userManagement = { login: 'Ozella.Kertzmann' };
      vi.spyOn(userManagementFormService, 'getUserManagement').mockReturnValue({ id: null, login: null });
      vi.spyOn(userManagementService, 'create').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ userManagement: null });
      comp.ngOnInit();
      fillValidForm();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(userManagement);
      saveSubject.complete();

      // THEN
      expect(userManagementFormService.getUserManagement).toHaveBeenCalled();
      expect(userManagementService.create).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<IUserManagement>();
      const userManagement = { id: 123, login: 'Ozella.Kertzmann' };
      vi.spyOn(userManagementService, 'update').mockReturnValue(saveSubject);
      vi.spyOn(comp, 'previousState');
      activatedRoute.data = of({ userManagement });
      comp.ngOnInit();
      fillValidForm();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(userManagementService.update).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('permissions', () => {
    it('should not save without a full name', () => {
      vi.spyOn(userManagementService, 'create');
      activatedRoute.data = of({ userManagement: null });
      comp.ngOnInit();
      comp.editForm.patchValue({ email: 'an@yp.vn' });
      comp.accessForm.controls.password.setValue('matkhau123');

      comp.save();

      expect(comp.editForm.controls.firstName.hasError('required')).toBe(true);
      expect(userManagementService.create).not.toHaveBeenCalled();
    });

    it('should not save when required fields are missing', () => {
      vi.spyOn(userManagementService, 'create');
      activatedRoute.data = of({ userManagement: null });
      comp.ngOnInit();

      comp.save();

      expect(userManagementService.create).not.toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
    });

    it('should create with login from email, password and authorities from the permission table', () => {
      vi.spyOn(userManagementService, 'create').mockReturnValue(new Subject<IUserManagement>());
      activatedRoute.data = of({ userManagement: null });
      comp.ngOnInit();
      fillValidForm();
      comp.accessForm.controls.levels.patchValue({ post: 'edit', listing: 'view' });

      comp.save();

      expect(userManagementService.create).toHaveBeenCalledWith(
        expect.objectContaining({
          login: 'an@yp.vn',
          email: 'an@yp.vn',
          password: 'matkhau123',
          authorities: ['ROLE_USER', 'ROLE_LISTING_VIEW', 'ROLE_POST_EDIT'],
        }),
      );
    });

    it('should read the permission table from existing authorities', () => {
      activatedRoute.data = of({ userManagement: { id: 1, login: 'an', authorities: ['ROLE_USER', 'ROLE_BANNER_VIEW'] } });
      comp.ngOnInit();

      expect(comp.accessForm.controls.levels.getRawValue()).toEqual({ listing: 'none', catalog: 'none', post: 'none', banner: 'view' });
      expect(comp.accessForm.controls.isAdmin.value).toBe(false);
    });
  });
});
