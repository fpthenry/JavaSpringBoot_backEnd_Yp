import { HttpResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Observable, finalize, map } from 'rxjs';

import { DataUtils, EventManager, EventWithContent, FileLoadError } from 'app/core/util';
import { IListing } from 'app/entities/listing/listing.model';
import { ListingService } from 'app/entities/listing/service/listing.service';
import { UserService } from 'app/entities/user/service/user.service';
import { IUser } from 'app/entities/user/user.model';
import { AlertError, AlertErrorModel } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { IGallery } from '../gallery.model';
import { GalleryService } from '../service/gallery.service';

import { GalleryFormGroup, GalleryFormService } from './gallery-form.service';

@Component({
  selector: 'jhi-gallery-update',
  templateUrl: './gallery-update.html',
  imports: [TranslateDirective, FontAwesomeModule, AlertError, ReactiveFormsModule],
})
export class GalleryUpdate implements OnInit {
  readonly isSaving = signal(false);
  gallery: IGallery | null = null;

  usersSharedCollection = signal<IUser[]>([]);
  listingsSharedCollection = signal<IListing[]>([]);

  protected dataUtils = inject(DataUtils);
  protected eventManager = inject(EventManager);
  protected galleryService = inject(GalleryService);
  protected galleryFormService = inject(GalleryFormService);
  protected userService = inject(UserService);
  protected listingService = inject(ListingService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: GalleryFormGroup = this.galleryFormService.createGalleryFormGroup();

  compareUser = (o1: IUser | null, o2: IUser | null): boolean => this.userService.compareUser(o1, o2);

  compareListing = (o1: IListing | null, o2: IListing | null): boolean => this.listingService.compareListing(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ gallery }) => {
      this.gallery = gallery;
      if (gallery) {
        this.updateForm(gallery);
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

    this.usersSharedCollection.update(users => this.userService.addUserToCollectionIfMissing<IUser>(users, gallery.author));
    this.listingsSharedCollection.update(listings =>
      this.listingService.addListingToCollectionIfMissing<IListing>(listings, gallery.listing),
    );
  }

  protected loadRelationshipsOptions(): void {
    this.userService
      .query()
      .pipe(map((res: HttpResponse<IUser[]>) => res.body ?? []))
      .pipe(map((users: IUser[]) => this.userService.addUserToCollectionIfMissing<IUser>(users, this.gallery?.author)))
      .subscribe((users: IUser[]) => this.usersSharedCollection.set(users));

    this.listingService
      .query()
      .pipe(map((res: HttpResponse<IListing[]>) => res.body ?? []))
      .pipe(map((listings: IListing[]) => this.listingService.addListingToCollectionIfMissing<IListing>(listings, this.gallery?.listing)))
      .subscribe((listings: IListing[]) => this.listingsSharedCollection.set(listings));
  }
}
