import { HttpResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Observable, finalize, map } from 'rxjs';

import { DataUtils, EventManager, EventWithContent, FileLoadError } from 'app/core/util';
import { IGallery } from 'app/entities/gallery/gallery.model';
import { GalleryService } from 'app/entities/gallery/service/gallery.service';
import { IListing } from 'app/entities/listing/listing.model';
import { ListingService } from 'app/entities/listing/service/listing.service';
import { AlertError, AlertErrorModel } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { IListingImage } from '../listing-image.model';
import { ListingImageService } from '../service/listing-image.service';

import { ListingImageFormGroup, ListingImageFormService } from './listing-image-form.service';

@Component({
  selector: 'jhi-listing-image-update',
  templateUrl: './listing-image-update.html',
  imports: [TranslateDirective, FontAwesomeModule, AlertError, ReactiveFormsModule],
})
export class ListingImageUpdate implements OnInit {
  readonly isSaving = signal(false);
  listingImage: IListingImage | null = null;

  listingsSharedCollection = signal<IListing[]>([]);
  galleriesSharedCollection = signal<IGallery[]>([]);

  protected dataUtils = inject(DataUtils);
  protected eventManager = inject(EventManager);
  protected listingImageService = inject(ListingImageService);
  protected listingImageFormService = inject(ListingImageFormService);
  protected listingService = inject(ListingService);
  protected galleryService = inject(GalleryService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: ListingImageFormGroup = this.listingImageFormService.createListingImageFormGroup();

  compareListing = (o1: IListing | null, o2: IListing | null): boolean => this.listingService.compareListing(o1, o2);

  compareGallery = (o1: IGallery | null, o2: IGallery | null): boolean => this.galleryService.compareGallery(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ listingImage }) => {
      this.listingImage = listingImage;
      if (listingImage) {
        this.updateForm(listingImage);
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
    const listingImage = this.listingImageFormService.getListingImage(this.editForm);
    if (listingImage.id === null) {
      this.subscribeToSaveResponse(this.listingImageService.create(listingImage));
    } else {
      this.subscribeToSaveResponse(this.listingImageService.update(listingImage));
    }
  }

  protected subscribeToSaveResponse(result: Observable<IListingImage | null>): void {
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

  protected updateForm(listingImage: IListingImage): void {
    this.listingImage = listingImage;
    this.listingImageFormService.resetForm(this.editForm, listingImage);

    this.listingsSharedCollection.update(listings =>
      this.listingService.addListingToCollectionIfMissing<IListing>(listings, listingImage.listing),
    );
    this.galleriesSharedCollection.update(galleries =>
      this.galleryService.addGalleryToCollectionIfMissing<IGallery>(galleries, listingImage.gallery),
    );
  }

  protected loadRelationshipsOptions(): void {
    this.listingService
      .query()
      .pipe(map((res: HttpResponse<IListing[]>) => res.body ?? []))
      .pipe(
        map((listings: IListing[]) => this.listingService.addListingToCollectionIfMissing<IListing>(listings, this.listingImage?.listing)),
      )
      .subscribe((listings: IListing[]) => this.listingsSharedCollection.set(listings));

    this.galleryService
      .query()
      .pipe(map((res: HttpResponse<IGallery[]>) => res.body ?? []))
      .pipe(
        map((galleries: IGallery[]) =>
          this.galleryService.addGalleryToCollectionIfMissing<IGallery>(galleries, this.listingImage?.gallery),
        ),
      )
      .subscribe((galleries: IGallery[]) => this.galleriesSharedCollection.set(galleries));
  }
}
