package com.mycompany.myapp.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.ListingImage} entity.
 */
@Schema(description = "Ảnh doanh nghiệp (webbupointfinder_item_images, _thumbnail_id)")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ListingImageDTO implements Serializable {

    private Long id;

    @Lob
    private String imageUrl;

    @Lob
    private String thumbnailUrl;

    @Size(max = 500)
    private String altText;

    @Min(value = 0)
    private Integer displayOrder;

    private Boolean isFeatured;

    private Instant createdAt;

    private Instant updatedAt;

    @NotNull
    private ListingDTO listing;

    private GalleryDTO gallery;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
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

    public Boolean getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public ListingDTO getListing() {
        return listing;
    }

    public void setListing(ListingDTO listing) {
        this.listing = listing;
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
        if (!(o instanceof ListingImageDTO)) {
            return false;
        }

        ListingImageDTO listingImageDTO = (ListingImageDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, listingImageDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ListingImageDTO{" +
            "id=" + getId() +
            ", imageUrl='" + getImageUrl() + "'" +
            ", thumbnailUrl='" + getThumbnailUrl() + "'" +
            ", altText='" + getAltText() + "'" +
            ", displayOrder=" + getDisplayOrder() +
            ", isFeatured='" + getIsFeatured() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            ", listing=" + getListing() +
            ", gallery=" + getGallery() +
            "}";
    }
}
