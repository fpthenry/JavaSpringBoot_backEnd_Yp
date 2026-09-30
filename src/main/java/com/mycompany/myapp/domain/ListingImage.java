package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Ảnh doanh nghiệp (webbupointfinder_item_images, _thumbnail_id)
 */
@Entity
@Table(name = "listing_image")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "listingimage")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ListingImage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Long)
    private Long id;

    @Lob
    @Column(name = "image_url", nullable = false)
    @org.springframework.data.elasticsearch.annotations.MultiField(
        mainField = @org.springframework.data.elasticsearch.annotations.Field(
            type = org.springframework.data.elasticsearch.annotations.FieldType.Text
        ),
        otherFields = {
            @org.springframework.data.elasticsearch.annotations.InnerField(
                suffix = "keyword",
                type = org.springframework.data.elasticsearch.annotations.FieldType.Keyword,
                ignoreAbove = 256
            ),
        }
    )
    private String imageUrl;

    @Lob
    @Column(name = "thumbnail_url")
    @org.springframework.data.elasticsearch.annotations.MultiField(
        mainField = @org.springframework.data.elasticsearch.annotations.Field(
            type = org.springframework.data.elasticsearch.annotations.FieldType.Text
        ),
        otherFields = {
            @org.springframework.data.elasticsearch.annotations.InnerField(
                suffix = "keyword",
                type = org.springframework.data.elasticsearch.annotations.FieldType.Keyword,
                ignoreAbove = 256
            ),
        }
    )
    private String thumbnailUrl;

    @Size(max = 500)
    @Column(name = "alt_text", length = 500)
    @org.springframework.data.elasticsearch.annotations.MultiField(
        mainField = @org.springframework.data.elasticsearch.annotations.Field(
            type = org.springframework.data.elasticsearch.annotations.FieldType.Text
        ),
        otherFields = {
            @org.springframework.data.elasticsearch.annotations.InnerField(
                suffix = "keyword",
                type = org.springframework.data.elasticsearch.annotations.FieldType.Keyword,
                ignoreAbove = 256
            ),
        }
    )
    private String altText;

    @Min(value = 0)
    @Column(name = "display_order")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Integer)
    private Integer displayOrder;

    @Column(name = "is_featured")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Boolean)
    private Boolean isFeatured;

    @Column(name = "created_at")
    @org.springframework.data.elasticsearch.annotations.Field(
        type = org.springframework.data.elasticsearch.annotations.FieldType.Date,
        format = org.springframework.data.elasticsearch.annotations.DateFormat.date_time
    )
    private Instant createdAt;

    @Column(name = "updated_at")
    @org.springframework.data.elasticsearch.annotations.Field(
        type = org.springframework.data.elasticsearch.annotations.FieldType.Date,
        format = org.springframework.data.elasticsearch.annotations.DateFormat.date_time
    )
    private Instant updatedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "imageses", "gallerieses", "author", "categorieses", "locationses" }, allowSetters = true)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "imageses", "author", "listing" }, allowSetters = true)
    private Gallery gallery;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public ListingImage id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    public ListingImage imageUrl(String imageUrl) {
        this.setImageUrl(imageUrl);
        return this;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getThumbnailUrl() {
        return this.thumbnailUrl;
    }

    public ListingImage thumbnailUrl(String thumbnailUrl) {
        this.setThumbnailUrl(thumbnailUrl);
        return this;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getAltText() {
        return this.altText;
    }

    public ListingImage altText(String altText) {
        this.setAltText(altText);
        return this;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public Integer getDisplayOrder() {
        return this.displayOrder;
    }

    public ListingImage displayOrder(Integer displayOrder) {
        this.setDisplayOrder(displayOrder);
        return this;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getIsFeatured() {
        return this.isFeatured;
    }

    public ListingImage isFeatured(Boolean isFeatured) {
        this.setIsFeatured(isFeatured);
        return this;
    }

    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public ListingImage createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public ListingImage updatedAt(Instant updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Listing getListing() {
        return this.listing;
    }

    public void setListing(Listing listing) {
        this.listing = listing;
    }

    public ListingImage listing(Listing listing) {
        this.setListing(listing);
        return this;
    }

    public Gallery getGallery() {
        return this.gallery;
    }

    public void setGallery(Gallery gallery) {
        this.gallery = gallery;
    }

    public ListingImage gallery(Gallery gallery) {
        this.setGallery(gallery);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ListingImage)) {
            return false;
        }
        return getId() != null && getId().equals(((ListingImage) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ListingImage{" +
            "id=" + getId() +
            ", imageUrl='" + getImageUrl() + "'" +
            ", thumbnailUrl='" + getThumbnailUrl() + "'" +
            ", altText='" + getAltText() + "'" +
            ", displayOrder=" + getDisplayOrder() +
            ", isFeatured='" + getIsFeatured() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
