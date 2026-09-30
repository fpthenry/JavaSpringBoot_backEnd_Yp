import { HttpResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Observable, finalize, map } from 'rxjs';

import { DataUtils, EventManager, EventWithContent, FileLoadError } from 'app/core/util';
import { UserService } from 'app/entities/user/service/user.service';
import { IUser } from 'app/entities/user/user.model';
import { AlertError, AlertErrorModel } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { StaticPageService } from '../service/static-page.service';
import { IStaticPage } from '../static-page.model';

import { StaticPageFormGroup, StaticPageFormService } from './static-page-form.service';

@Component({
  selector: 'jhi-static-page-update',
  templateUrl: './static-page-update.html',
  imports: [TranslateDirective, FontAwesomeModule, AlertError, ReactiveFormsModule],
})
export class StaticPageUpdate implements OnInit {
  readonly isSaving = signal(false);
  staticPage: IStaticPage | null = null;

  staticPagesSharedCollection = signal<IStaticPage[]>([]);
  usersSharedCollection = signal<IUser[]>([]);

  protected dataUtils = inject(DataUtils);
  protected eventManager = inject(EventManager);
  protected staticPageService = inject(StaticPageService);
  protected staticPageFormService = inject(StaticPageFormService);
  protected userService = inject(UserService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: StaticPageFormGroup = this.staticPageFormService.createStaticPageFormGroup();

  compareStaticPage = (o1: IStaticPage | null, o2: IStaticPage | null): boolean => this.staticPageService.compareStaticPage(o1, o2);

  compareUser = (o1: IUser | null, o2: IUser | null): boolean => this.userService.compareUser(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ staticPage }) => {
      this.staticPage = staticPage;
      if (staticPage) {
        this.updateForm(staticPage);
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
    const staticPage = this.staticPageFormService.getStaticPage(this.editForm);
    if (staticPage.id === null) {
      this.subscribeToSaveResponse(this.staticPageService.create(staticPage));
    } else {
      this.subscribeToSaveResponse(this.staticPageService.update(staticPage));
    }
  }

  protected subscribeToSaveResponse(result: Observable<IStaticPage | null>): void {
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

  protected updateForm(staticPage: IStaticPage): void {
    this.staticPage = staticPage;
    this.staticPageFormService.resetForm(this.editForm, staticPage);

    this.staticPagesSharedCollection.update(staticPages =>
      this.staticPageService.addStaticPageToCollectionIfMissing<IStaticPage>(staticPages, staticPage.parent),
    );
    this.usersSharedCollection.update(users => this.userService.addUserToCollectionIfMissing<IUser>(users, staticPage.author));
  }

  protected loadRelationshipsOptions(): void {
    this.staticPageService
      .query()
      .pipe(map((res: HttpResponse<IStaticPage[]>) => res.body ?? []))
      .pipe(
        map((staticPages: IStaticPage[]) =>
          this.staticPageService.addStaticPageToCollectionIfMissing<IStaticPage>(staticPages, this.staticPage?.parent),
        ),
      )
      .subscribe((staticPages: IStaticPage[]) => this.staticPagesSharedCollection.set(staticPages));

    this.userService
      .query()
      .pipe(map((res: HttpResponse<IUser[]>) => res.body ?? []))
      .pipe(map((users: IUser[]) => this.userService.addUserToCollectionIfMissing<IUser>(users, this.staticPage?.author)))
      .subscribe((users: IUser[]) => this.usersSharedCollection.set(users));
  }
}
