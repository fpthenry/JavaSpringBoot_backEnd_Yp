import { beforeEach, describe, expect, it } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { GalleryImageFormService } from './gallery-image-form.service';

// Sửa tay: ảnh mới không được tự đặt startAt/endAt = giờ hiện tại (làm ảnh hết hạn ngay khi tạo)
describe('GalleryImage Form Service - ngày giờ hiển thị', () => {
  let service: GalleryImageFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(GalleryImageFormService);
  });

  it('ảnh mới để trống startAt/endAt và lưu null', () => {
    const formGroup = service.createGalleryImageFormGroup();
    expect(formGroup.controls.startAt.value).toBeFalsy();
    expect(formGroup.controls.endAt.value).toBeFalsy();

    const galleryImage = service.getGalleryImage(formGroup);
    expect(galleryImage.startAt).toBeNull();
    expect(galleryImage.endAt).toBeNull();
  });

  it('giữ nguyên ngày giờ khi có nhập', () => {
    const formGroup = service.createGalleryImageFormGroup();
    formGroup.controls.startAt.setValue('2026-10-05T14:30');
    const galleryImage = service.getGalleryImage(formGroup);
    expect(galleryImage.startAt?.format('YYYY-MM-DDTHH:mm')).toBe('2026-10-05T14:30');
    expect(galleryImage.endAt).toBeNull();
  });
});
