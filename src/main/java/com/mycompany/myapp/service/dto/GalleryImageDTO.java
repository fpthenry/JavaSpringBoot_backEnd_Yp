package com.mycompany.myapp.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.GalleryImage} entity.
 */
@Schema(description = "Ảnh quảng cáo trong một vị trí banner")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class GalleryImageDTO implements Serializable {

    private Long id;

    @Size(max = 255)
    private String title;

    @Schema(description = "Ảnh upload lên (lưu trong database)")
    @Lob
    private byte[] image;

    private String imageContentType;

    @Size(max = 1000)
    @Schema(description = "Hoặc link ảnh có sẵn (CDN, wp-content/uploads...); dùng khi không upload")
    private String imageUrl;

    @Size(max = 1000)
    @Schema(description = "Bấm vào ảnh thì mở link này")
    private String linkUrl;

    @Size(max = 255)
    private String altText;

    @NotNull
    @Min(value = 0)
    @Schema(description = "Thứ tự hiển thị, nhỏ đứng trước", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer displayOrder;

    @NotNull
    private Boolean active;

    @Schema(description = "Hẹn giờ: chỉ hiển thị từ thời điểm này (bỏ trống = ngay)")
    private Instant startAt;

    @Schema(description = "Hẹn giờ: ngừng hiển thị sau thời điểm này (bỏ trống = không hết hạn)")
    private Instant endAt;

    private Boolean openInNewTab;

    @NotNull
    private GalleryDTO gallery;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public String getImageContentType() {
        return imageContentType;
    }

    public void setImageContentType(String imageContentType) {
        this.imageContentType = imageContentType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }

    public Boolean getOpenInNewTab() {
        return openInNewTab;
    }

    public void setOpenInNewTab(Boolean openInNewTab) {
        this.openInNewTab = openInNewTab;
    }

    public GalleryDTO getGallery() {
        return gallery;
    }

    public void setGallery(GalleryDTO gallery) {
        this.gallery = gallery;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GalleryImageDTO)) {
            return false;
        }

        GalleryImageDTO galleryImageDTO = (GalleryImageDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, galleryImageDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "GalleryImageDTO{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", image='" + getImage() + "'" +
            ", imageUrl='" + getImageUrl() + "'" +
            ", linkUrl='" + getLinkUrl() + "'" +
            ", altText='" + getAltText() + "'" +
            ", displayOrder=" + getDisplayOrder() +
            ", active='" + getActive() + "'" +
            ", startAt='" + getStartAt() + "'" +
            ", endAt='" + getEndAt() + "'" +
            ", openInNewTab='" + getOpenInNewTab() + "'" +
            ", gallery=" + getGallery() +
            "}";
    }
}
