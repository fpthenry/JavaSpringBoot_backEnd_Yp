import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Observable, finalize } from 'rxjs';

import { DataUtils, EventManager, EventWithContent, FileLoadError } from 'app/core/util';
import { AlertError, AlertErrorModel } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { ICachedContent } from '../cached-content.model';
import { CachedContentService } from '../service/cached-content.service';

import { CachedContentFormGroup, CachedContentFormService } from './cached-content-form.service';

@Component({
  selector: 'jhi-cached-content-update',
  templateUrl: './cached-content-update.html',
  imports: [TranslateDirective, FontAwesomeModule, AlertError, ReactiveFormsModule],
})
export class CachedContentUpdate implements OnInit {
  readonly isSaving = signal(false);
  cachedContent: ICachedContent | null = null;

  protected dataUtils = inject(DataUtils);
  protected eventManager = inject(EventManager);
  protected cachedContentService = inject(CachedContentService);
  protected cachedContentFormService = inject(CachedContentFormService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: CachedContentFormGroup = this.cachedContentFormService.createCachedContentFormGroup();

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ cachedContent }) => {
      this.cachedContent = cachedContent;
      if (cachedContent) {
        this.updateForm(cachedContent);
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
    const cachedContent = this.cachedContentFormService.getCachedContent(this.editForm);
    if (cachedContent.id === null) {
      this.subscribeToSaveResponse(this.cachedContentService.create(cachedContent));
    } else {
      this.subscribeToSaveResponse(this.cachedContentService.update(cachedContent));
    }
  }

  protected subscribeToSaveResponse(result: Observable<ICachedContent | null>): void {
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

  protected updateForm(cachedContent: ICachedContent): void {
    this.cachedContent = cachedContent;
    this.cachedContentFormService.resetForm(this.editForm, cachedContent);
  }
}
