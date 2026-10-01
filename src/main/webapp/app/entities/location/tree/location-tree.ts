import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

import { AlertError } from 'app/shared/alert';
import { TranslateDirective } from 'app/shared/language';
import { TreeItem, TreeView, createTreeSource } from 'app/shared/tree';
import { ILocation } from '../location.model';
import { LocationService } from '../service/location.service';

/** Cây đơn vị hành chính: tỉnh/thành -> quận/huyện -> phường/xã. */
@Component({
  selector: 'jhi-location-tree',
  templateUrl: './location-tree.html',
  imports: [RouterLink, FontAwesomeModule, AlertError, TranslateDirective, TreeView],
})
export class LocationTree {
  readonly source = createTreeSource(inject(LocationService));

  /** Phường/xã là cấp cuối. */
  readonly isLeaf = (item: TreeItem): boolean => (item as ILocation).type === 'ward';
  readonly typeLabel = (item: TreeItem): string | null => {
    const type = (item as ILocation).type;
    return type ? `javaSpringBootBackEndApp.location.tree.type.${type}` : null;
  };
}
