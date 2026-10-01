import { HttpClient, HttpResponse, httpResource } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';

import dayjs from 'dayjs/esm';
import { Observable, asapScheduler, catchError, map, scheduled } from 'rxjs';

import { serverApiUrl } from 'app/config';
import { SearchWithPagination, createRequestOption } from 'app/core/request';
import { ILocation, NewLocation } from '../location.model';

export type PartialUpdateLocation = Partial<ILocation> & Pick<ILocation, 'id'>;

type RestOf<T extends ILocation | NewLocation> = Omit<T, 'createdAt'> & {
  createdAt?: string | null;
};

export type RestLocation = RestOf<ILocation>;

export type NewRestLocation = RestOf<NewLocation>;

export type PartialUpdateRestLocation = RestOf<PartialUpdateLocation>;

@Service()
export class LocationsService {
  readonly locationsParams = signal<Record<string, string | number | boolean | readonly (string | number | boolean)[]> | undefined>(
    undefined,
  );
  readonly locationsResource = httpResource<RestLocation[]>(() => {
    const params = this.locationsParams();
    if (!params) {
      return undefined;
    }
    return { url: params.query ? this.resourceSearchUrl : this.resourceUrl, params };
  });
  /**
   * This signal holds the list of location that have been fetched. It is updated when the locationsResource emits a new value.
   * In case of error while fetching the locations, the signal is set to an empty array.
   */
  readonly locations = computed(() =>
    (this.locationsResource.hasValue() ? this.locationsResource.value() : []).map(item => this.convertValueFromServer(item)),
  );
  protected readonly resourceUrl = `${serverApiUrl}api/locations`;
  protected readonly resourceSearchUrl = `${serverApiUrl}api/locations/_search`;

  protected convertValueFromServer(restLocation: RestLocation): ILocation {
    return {
      ...restLocation,
      createdAt: restLocation.createdAt ? dayjs(restLocation.createdAt) : undefined,
    };
  }
}

@Service()
export class LocationService extends LocationsService {
  protected readonly http = inject(HttpClient);

  create(location: NewLocation): Observable<ILocation> {
    const copy = this.convertValueFromClient(location);
    return this.http.post<RestLocation>(this.resourceUrl, copy).pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(location: ILocation): Observable<ILocation> {
    const copy = this.convertValueFromClient(location);
    return this.http
      .put<RestLocation>(`${this.resourceUrl}/${encodeURIComponent(this.getLocationIdentifier(location))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(location: PartialUpdateLocation): Observable<ILocation> {
    const copy = this.convertValueFromClient(location);
    return this.http
      .patch<RestLocation>(`${this.resourceUrl}/${encodeURIComponent(this.getLocationIdentifier(location))}`, copy)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<ILocation> {
    return this.http
      .get<RestLocation>(`${this.resourceUrl}/${encodeURIComponent(id)}`)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<HttpResponse<ILocation[]>> {
    const options = createRequestOption(req);
    return this.http
      .get<RestLocation[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => res.clone({ body: this.convertResponseArrayFromServer(res.body!) })));
  }

  delete(id: number): Observable<undefined> {
    return this.http.delete<undefined>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  search(req: SearchWithPagination): Observable<ILocation[]> {
    const options = createRequestOption(req);
    return this.http.get<RestLocation[]>(this.resourceSearchUrl, { params: options }).pipe(
      map(res => this.convertResponseArrayFromServer(res)),
      catchError(() => scheduled([], asapScheduler)),
    );
  }

  getLocationIdentifier(location: Pick<ILocation, 'id'>): number {
    return location.id;
  }

  compareLocation(o1: Pick<ILocation, 'id'> | null, o2: Pick<ILocation, 'id'> | null): boolean {
    return o1 && o2 ? this.getLocationIdentifier(o1) === this.getLocationIdentifier(o2) : o1 === o2;
  }

  addLocationToCollectionIfMissing<Type extends Pick<ILocation, 'id'>>(
    locationCollection: Type[],
    ...locationsToCheck: (Type | null | undefined)[]
  ): Type[] {
    const locations: Type[] = locationsToCheck.filter(locationItem => locationItem !== null && locationItem !== undefined);
    if (locations.length > 0) {
      const locationCollectionIdentifiers = locationCollection.map(locationItem => this.getLocationIdentifier(locationItem));
      const locationsToAdd = locations.filter(locationItem => {
        const locationIdentifier = this.getLocationIdentifier(locationItem);
        if (locationCollectionIdentifiers.includes(locationIdentifier)) {
          return false;
        }
        locationCollectionIdentifiers.push(locationIdentifier);
        return true;
      });
      return [...locationsToAdd, ...locationCollection];
    }
    return locationCollection;
  }

  protected convertValueFromClient<T extends ILocation | NewLocation | PartialUpdateLocation>(location: T): RestOf<T> {
    return {
      ...location,
      createdAt: location.createdAt?.toJSON() ?? null,
    };
  }

  protected convertResponseFromServer(res: RestLocation): ILocation {
    return this.convertValueFromServer(res);
  }

  protected convertResponseArrayFromServer(res: RestLocation[]): ILocation[] {
    return res.map(item => this.convertValueFromServer(item));
  }
}
