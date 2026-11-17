import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable, finalize } from 'rxjs';

import { AlertError } from 'app/shared/alert';
import {
  PERMISSION_LEVELS,
  PERMISSION_MODULES,
  PermissionLevel,
  PermissionModuleKey,
  authoritiesFrom,
  levelOf,
} from 'app/shared/auth/permissions';
import { Authority } from 'app/shared/jhipster/constants';
import { TranslateDirective } from 'app/shared/language';
import { UserManagementService } from '../service/user-management.service';
import { IUserManagement, NewUserManagement } from '../user-management.model';

import { UserManagementFormGroup, UserManagementFormService } from './user-management-form.service';

/** Mật khẩu admin đặt cho tài khoản: tối thiểu 8 ký tự (khớp UserResource.ADMIN_PASSWORD_MIN_LENGTH). */
export const PASSWORD_MIN_LENGTH = 8;

/** Mẫu quyền hay dùng: bấm một lần là điền sẵn bảng quyền. */
export interface PermissionPreset {
  label: string;
  levels: Record<PermissionModuleKey, PermissionLevel>;
}

export const PERMISSION_PRESETS: PermissionPreset[] = [
  {
    label: 'userManagement.permissions.presets.editor',
    levels: { listing: 'none', catalog: 'none', post: 'edit', banner: 'edit' },
  },
  {
    label: 'userManagement.permissions.presets.dataEntry',
    levels: { listing: 'edit', catalog: 'view', post: 'none', banner: 'none' },
  },
  {
    label: 'userManagement.permissions.presets.viewer',
    levels: { listing: 'view', catalog: 'view', post: 'view', banner: 'view' },
  },
];

/** Tên đăng nhập sinh từ email (người dùng đăng nhập bằng email, không phải nhớ tên đăng nhập). */
export function loginFromEmail(email: string): string {
  const value = email.trim().toLowerCase();
  if (value.length <= 50) {
    return value;
  }
  return value
    .split('@')[0]
    .replace(/[^_.a-z0-9-]/g, '')
    .slice(0, 50);
}

/** Mật khẩu ngẫu nhiên 12 ký tự, bỏ các ký tự dễ nhầm (0/O, 1/l/I). */
export function randomPassword(length = 12): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%';
  const values = new Uint32Array(length);
  crypto.getRandomValues(values);
  return Array.from(values, value => chars[value % chars.length]).join('');
}

/**
 * Sửa tay: form tạo/sửa tài khoản gọn cho người quản trị: Họ và tên, Email (dùng để đăng nhập), Mật khẩu, Trạng thái và
 * bảng quyền theo từng mục (Không / Chỉ xem / Quản lý). Tên đăng nhập tự sinh từ email, không hiện trên form.
 */
@Component({
  selector: 'jhi-user-management-update',
  templateUrl: './user-management-update.html',
  styleUrl: './user-management-update.scss',
  imports: [TranslateDirective, TranslatePipe, FontAwesomeModule, AlertError, ReactiveFormsModule],
})
export class UserManagementUpdate implements OnInit {
  readonly isSaving = signal(false);
  readonly showPassword = signal(false);
  readonly copied = signal(false);
  userManagement: IUserManagement | null = null;

  readonly modules = PERMISSION_MODULES;
  readonly levels = PERMISSION_LEVELS;
  readonly presets = PERMISSION_PRESETS;
  readonly passwordMinLength = PASSWORD_MIN_LENGTH;

  protected userManagementService = inject(UserManagementService);
  protected userManagementFormService = inject(UserManagementFormService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: UserManagementFormGroup = this.userManagementFormService.createUserManagementFormGroup();

  /** Các ô riêng của form mới, không thuộc IUserManagement: mật khẩu và quyền dạng bảng. */
  // eslint-disable-next-line @typescript-eslint/member-ordering
  readonly accessForm = new FormGroup({
    password: new FormControl<string>('', { nonNullable: true, validators: [Validators.minLength(PASSWORD_MIN_LENGTH)] }),
    isAdmin: new FormControl<boolean>(false, { nonNullable: true }),
    levels: new FormGroup(
      Object.fromEntries(
        PERMISSION_MODULES.map(module => [module.key, new FormControl<PermissionLevel>('none', { nonNullable: true })]),
      ) as Record<PermissionModuleKey, FormControl<PermissionLevel>>,
    ),
  });

  ngOnInit(): void {
    const { login, firstName, email } = this.editForm.controls;
    // Tên đăng nhập không hiện trên form: tạo mới thì sinh từ email lúc lưu, sửa thì giữ nguyên.
    login.clearValidators();
    login.updateValueAndValidity();
    firstName.addValidators([Validators.required]);
    email.addValidators([Validators.required, Validators.email]);
    // Thêm luật xong phải tính lại, không thì ô trống vẫn được coi là hợp lệ.
    firstName.updateValueAndValidity();
    email.updateValueAndValidity();
    // Quản trị viên đã có toàn quyền: khóa bảng quyền theo mục cho khỏi nhầm.
    const { isAdmin, levels } = this.accessForm.controls;
    isAdmin.valueChanges.subscribe(admin => (admin ? levels.disable({ emitEvent: false }) : levels.enable({ emitEvent: false })));
    this.activatedRoute.data.subscribe(({ userManagement }) => {
      this.userManagement = userManagement;
      if (userManagement) {
        this.updateForm(userManagement);
      } else {
        // Tạo mới: bắt buộc có mật khẩu, mặc định đang hoạt động.
        this.accessForm.controls.password.addValidators([Validators.required]);
        this.accessForm.controls.password.updateValueAndValidity();
        this.editForm.controls.activated.setValue(true);
      }
    });
  }

  get isNew(): boolean {
    return !this.userManagement?.id;
  }

  /** Tên hiển thị của tài khoản đang sửa (họ tên, không có thì email). */
  get displayName(): string {
    const user = this.userManagement;
    const name = [user?.firstName, user?.lastName].filter(Boolean).join(' ').trim();
    return name !== '' ? name : (user?.email ?? user?.login ?? '');
  }

  isInvalid(control: FormControl<unknown>): boolean {
    return control.invalid && (control.dirty || control.touched);
  }

  togglePassword(): void {
    this.showPassword.update(show => !show);
  }

  generatePassword(): void {
    const password = this.accessForm.controls.password;
    password.setValue(randomPassword());
    password.markAsDirty();
    this.showPassword.set(true);
  }

  copyPassword(): void {
    const value = this.accessForm.controls.password.value;
    if (!value) {
      return;
    }
    try {
      // Trang mở qua http (không phải https) thì trình duyệt không cho dùng clipboard: bỏ qua, người dùng tự bôi đen để chép.
      navigator.clipboard
        .writeText(value)
        .then(() => {
          this.copied.set(true);
          setTimeout(() => this.copied.set(false), 1500);
        })
        .catch(() => undefined);
    } catch {
      // không có clipboard
    }
  }

  applyPreset(preset: PermissionPreset): void {
    this.accessForm.controls.isAdmin.setValue(false);
    this.accessForm.controls.levels.setValue(preset.levels);
    this.accessForm.markAsDirty();
  }

  previousState(): void {
    globalThis.history.back();
  }

  save(): void {
    if (this.editForm.invalid || this.accessForm.invalid) {
      this.editForm.markAllAsTouched();
      this.accessForm.markAllAsTouched();
      return;
    }
    this.isSaving.set(true);
    const userManagement = this.userManagementFormService.getUserManagement(this.editForm);
    const { password, isAdmin, levels } = this.accessForm.getRawValue();
    const email = (userManagement.email ?? '').trim().toLowerCase();
    const payload = {
      ...userManagement,
      firstName: userManagement.firstName?.trim() ?? null,
      email,
      authorities: authoritiesFrom(isAdmin, levels),
      password: password || null,
    };
    if (userManagement.id === null) {
      this.subscribeToSaveResponse(
        this.userManagementService.create({ ...payload, login: loginFromEmail(email) } as unknown as NewUserManagement),
      );
    } else {
      this.subscribeToSaveResponse(this.userManagementService.update(payload as IUserManagement));
    }
  }

  protected subscribeToSaveResponse(result: Observable<IUserManagement | null>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.previousState();
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }

  protected onSaveFinalize(): void {
    this.isSaving.set(false);
  }

  protected updateForm(userManagement: IUserManagement): void {
    this.userManagement = userManagement;
    this.userManagementFormService.resetForm(this.editForm, userManagement);
    const authorities = userManagement.authorities ?? [];
    this.accessForm.controls.isAdmin.setValue(authorities.includes(Authority.ADMIN));
    this.accessForm.controls.levels.setValue(
      Object.fromEntries(PERMISSION_MODULES.map(module => [module.key, levelOf(authorities, module)])) as Record<
        PermissionModuleKey,
        PermissionLevel
      >,
    );
  }
}
