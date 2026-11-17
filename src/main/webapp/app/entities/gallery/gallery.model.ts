export interface IGallery {
  id: number;
  name?: string | null;
  code?: string | null;
  description?: string | null;
  active?: boolean | null;
  wpId?: number | null;
}

export type NewGallery = Omit<IGallery, 'id'> & { id: null };

/**
 * Sửa tay: một vị trí banner kèm mọi ảnh, từ GET /api/galleries/with-images (GalleryWithImagesDTO). Dùng cho trang quản lý banner.
 * Ngày giờ dạng chuỗi ISO như server trả về.
 */
export interface IGalleryBanner {
  id: number;
  title?: string | null;
  uploaded: boolean;
  imageContentType?: string | null;
  /** URL xem ảnh: ảnh upload thì là endpoint media công khai, không thì là link ảnh. */
  previewUrl?: string | null;
  imageUrl?: string | null;
  linkUrl?: string | null;
  altText?: string | null;
  displayOrder?: number | null;
  active?: boolean | null;
  startAt?: string | null;
  endAt?: string | null;
  openInNewTab?: boolean | null;
  /** Đang hiện trên website: ảnh và vị trí đều bật, trong khoảng ngày, có ảnh. */
  showingNow: boolean;
}

export interface IGalleryWithImages {
  id: number;
  name?: string | null;
  code?: string | null;
  description?: string | null;
  active?: boolean | null;
  imageCount: number;
  images: IGalleryBanner[];
}
