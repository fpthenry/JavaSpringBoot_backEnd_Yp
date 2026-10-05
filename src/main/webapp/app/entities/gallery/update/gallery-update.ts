import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap/tooltip';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable, finalize } from 'rxjs';

import { DataUtils, EventManager, EventWithContent, FileLoadError } from 'app/core/util';
import { AlertError, AlertErrorModel } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { IGallery } from '../gallery.model';
import { GalleryService } from '../service/gallery.service';

import { GalleryFormGroup, GalleryFormService } from './gallery-form.service';

@Component({
  selector: 'jhi-gallery-update',
  templateUrl: './gallery-update.html',
  imports: [TranslateDirective, TranslatePipe, FontAwesomeModule, AlertError, ReactiveFormsModule, NgbTooltip],
})
export class GalleryUpdate implements OnInit {
  readonly isSaving = signal(false);
  gallery: IGallery | null = null;

  protected dataUtils = inject(DataUtils);
  protected eventManager = inject(EventManager);
  protected galleryService = inject(GalleryService);
  protected galleryFormService = inject(GalleryFormService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: GalleryFormGroup = this.galleryFormService.createGalleryFormGroup();

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ gallery }) => {
      this.gallery = gallery;
      if (gallery) {
        this.updateForm(gallery);
      }
    });
  }

  byteSize(base64String: string): string {
    return this.dataUtils.byteSize(base64String);
  }

  openFile(base64String: string, contentType: string | null | undefined): void {
    this.dataUtils.openFile(base64String, contentType);
  }

  setFileData(event: Event, field: string, isImage: boolean): void {
    this.dataUtils.loadFileToForm(event, this.editForm, field, isImage).subscribe({
      error: (err: FileLoadError) =>
        this.eventManager.broadcast(
          new EventWithContent<AlertErrorModel>('javaSpringBootBackEndApp.error', { ...err, key: `error.file.${err.key}` }),
        ),
    });
  }

  previousState(): void {
    globalThis.history.back();
  }

  save(): void {
    this.isSaving.set(true);
    const gallery = this.galleryFormService.getGallery(this.editForm);
    if (gallery.id === null) {
      this.subscribeToSaveResponse(this.galleryService.create(gallery));
    } else {
      this.subscribeToSaveResponse(this.galleryService.update(gallery));
    }
  }

  protected subscribeToSaveResponse(result: Observable<IGallery | null>): void {
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

  protected updateForm(gallery: IGallery): void {
    this.gallery = gallery;
    this.galleryFormService.resetForm(this.editForm, gallery);
  }
}
