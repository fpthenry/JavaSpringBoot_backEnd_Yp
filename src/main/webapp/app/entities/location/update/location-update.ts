import { HttpResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap/tooltip';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable, finalize, map } from 'rxjs';

import { IListing } from 'app/entities/listing/listing.model';
import { ListingService } from 'app/entities/listing/service/listing.service';
import { AlertError } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { ILocation } from '../location.model';
import { LocationService } from '../service/location.service';

import { LocationFormGroup, LocationFormService } from './location-form.service';

@Component({
  selector: 'jhi-location-update',
  templateUrl: './location-update.html',
  imports: [TranslateDirective, TranslatePipe, FontAwesomeModule, AlertError, ReactiveFormsModule, NgbTooltip],
})
export class LocationUpdate implements OnInit {
  readonly isSaving = signal(false);
  location: ILocation | null = null;

  locationsSharedCollection = signal<ILocation[]>([]);
  listingsSharedCollection = signal<IListing[]>([]);

  protected locationService = inject(LocationService);
  protected locationFormService = inject(LocationFormService);
  protected listingService = inject(ListingService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: LocationFormGroup = this.locationFormService.createLocationFormGroup();

  compareLocation = (o1: ILocation | null, o2: ILocation | null): boolean => this.locationService.compareLocation(o1, o2);

  compareListing = (o1: IListing | null, o2: IListing | null): boolean => this.listingService.compareListing(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ location }) => {
      this.location = location;
      if (location) {
        this.updateForm(location);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    globalThis.history.back();
  }

  save(): void {
    this.isSaving.set(true);
    const location = this.locationFormService.getLocation(this.editForm);
    if (location.id === null) {
      this.subscribeToSaveResponse(this.locationService.create(location));
    } else {
      this.subscribeToSaveResponse(this.locationService.update(location));
    }
  }

  protected subscribeToSaveResponse(result: Observable<ILocation | null>): void {
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

  protected updateForm(location: ILocation): void {
    this.location = location;
    this.locationFormService.resetForm(this.editForm, location);

    this.locationsSharedCollection.update(locations =>
      this.locationService.addLocationToCollectionIfMissing<ILocation>(locations, location.parent),
    );
    this.listingsSharedCollection.update(listings =>
      this.listingService.addListingToCollectionIfMissing<IListing>(listings, ...(location.listings ?? [])),
    );
  }

  protected loadRelationshipsOptions(): void {
    this.locationService
      .query()
      .pipe(map((res: HttpResponse<ILocation[]>) => res.body ?? []))
      .pipe(
        map((locations: ILocation[]) => this.locationService.addLocationToCollectionIfMissing<ILocation>(locations, this.location?.parent)),
      )
      .subscribe((locations: ILocation[]) => this.locationsSharedCollection.set(locations));

    this.listingService
      .query()
      .pipe(map((res: HttpResponse<IListing[]>) => res.body ?? []))
      .pipe(
        map((listings: IListing[]) =>
          this.listingService.addListingToCollectionIfMissing<IListing>(listings, ...(this.location?.listings ?? [])),
        ),
      )
      .subscribe((listings: IListing[]) => this.listingsSharedCollection.set(listings));
  }
}
