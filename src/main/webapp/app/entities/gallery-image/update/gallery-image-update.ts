import { HttpResponse } from '@angular/common/http';
import { Component, ElementRef, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap/tooltip';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable, finalize, map } from 'rxjs';

import { DataUtils, EventManager, EventWithContent, FileLoadError } from 'app/core/util';
import { IGallery } from 'app/entities/gallery/gallery.model';
import { GalleryService } from 'app/entities/gallery/service/gallery.service';
import { AlertError, AlertErrorModel } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { IGalleryImage } from '../gallery-image.model';
import { GalleryImageService } from '../service/gallery-image.service';

import { GalleryImageFormGroup, GalleryImageFormService } from './gallery-image-form.service';

@Component({
  selector: 'jhi-gallery-image-update',
  templateUrl: './gallery-image-update.html',
  imports: [TranslateDirective, TranslatePipe, FontAwesomeModule, AlertError, ReactiveFormsModule, NgbTooltip],
})
export class GalleryImageUpdate implements OnInit {
  readonly isSaving = signal(false);
  galleryImage: IGalleryImage | null = null;

  galleriesSharedCollection = signal<IGallery[]>([]);

  protected dataUtils = inject(DataUtils);
  protected eventManager = inject(EventManager);
  protected galleryImageService = inject(GalleryImageService);
  protected galleryImageFormService = inject(GalleryImageFormService);
  protected galleryService = inject(GalleryService);
  protected elementRef = inject(ElementRef);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: GalleryImageFormGroup = this.galleryImageFormService.createGalleryImageFormGroup();

  compareGallery = (o1: IGallery | null, o2: IGallery | null): boolean => this.galleryService.compareGallery(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ galleryImage }) => {
      this.galleryImage = galleryImage;
      if (galleryImage) {
        this.updateForm(galleryImage);
      }

      this.loadRelationshipsOptions();
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

  clearInputImage(field: string, fieldContentType: string, idInput: string): void {
    this.editForm.patchValue({
      [field]: null,
      [fieldContentType]: null,
    });
    if (idInput && this.elementRef.nativeElement.querySelector(`#${idInput}`)) {
      this.elementRef.nativeElement.querySelector(`#${idInput}`).value = null;
    }
  }

  previousState(): void {
    globalThis.history.back();
  }

  save(): void {
    this.isSaving.set(true);
    const galleryImage = this.galleryImageFormService.getGalleryImage(this.editForm);
    if (galleryImage.id === null) {
      this.subscribeToSaveResponse(this.galleryImageService.create(galleryImage));
    } else {
      this.subscribeToSaveResponse(this.galleryImageService.update(galleryImage));
    }
  }

  protected subscribeToSaveResponse(result: Observable<IGalleryImage | null>): void {
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

  protected updateForm(galleryImage: IGalleryImage): void {
    this.galleryImage = galleryImage;
    this.galleryImageFormService.resetForm(this.editForm, galleryImage);

    this.galleriesSharedCollection.update(galleries =>
      this.galleryService.addGalleryToCollectionIfMissing<IGallery>(galleries, galleryImage.gallery),
    );
  }

  protected loadRelationshipsOptions(): void {
    this.galleryService
      .query()
      .pipe(map((res: HttpResponse<IGallery[]>) => res.body ?? []))
      .pipe(
        map((galleries: IGallery[]) =>
          this.galleryService.addGalleryToCollectionIfMissing<IGallery>(galleries, this.galleryImage?.gallery),
        ),
      )
      .subscribe((galleries: IGallery[]) => this.galleriesSharedCollection.set(galleries));
  }
}
