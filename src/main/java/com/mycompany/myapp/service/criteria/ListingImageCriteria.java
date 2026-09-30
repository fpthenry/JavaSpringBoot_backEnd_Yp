package com.mycompany.myapp.service.criteria;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.mycompany.myapp.domain.ListingImage} entity. This class is used
 * in {@link com.mycompany.myapp.web.rest.ListingImageResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /listing-images?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ListingImageCriteria implements Serializable, Criteria {

    @Serial
    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter altText;

    private IntegerFilter displayOrder;

    private BooleanFilter isFeatured;

    private InstantFilter createdAt;

    private InstantFilter updatedAt;

    private LongFilter listingId;

    private LongFilter galleryId;

    private Boolean distinct;

    public ListingImageCriteria() {}

    public ListingImageCriteria(ListingImageCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.altText = other.optionalAltText().map(StringFilter::copy).orElse(null);
        this.displayOrder = other.optionalDisplayOrder().map(IntegerFilter::copy).orElse(null);
        this.isFeatured = other.optionalIsFeatured().map(BooleanFilter::copy).orElse(null);
        this.createdAt = other.optionalCreatedAt().map(InstantFilter::copy).orElse(null);
        this.updatedAt = other.optionalUpdatedAt().map(InstantFilter::copy).orElse(null);
        this.listingId = other.optionalListingId().map(LongFilter::copy).orElse(null);
        this.galleryId = other.optionalGalleryId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public ListingImageCriteria copy() {
        return new ListingImageCriteria(this);
    }

    public LongFilter getId() {
        return id;
    }

    public Optional<LongFilter> optionalId() {
        return Optional.ofNullable(id);
    }

    public LongFilter id() {
        if (id == null) {
            setId(new LongFilter());
        }
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public StringFilter getAltText() {
        return altText;
    }

    public Optional<StringFilter> optionalAltText() {
        return Optional.ofNullable(altText);
    }

    public StringFilter altText() {
        if (altText == null) {
            setAltText(new StringFilter());
        }
        return altText;
    }

    public void setAltText(StringFilter altText) {
        this.altText = altText;
    }

    public IntegerFilter getDisplayOrder() {
        return displayOrder;
    }

    public Optional<IntegerFilter> optionalDisplayOrder() {
        return Optional.ofNullable(displayOrder);
    }

    public IntegerFilter displayOrder() {
        if (displayOrder == null) {
            setDisplayOrder(new IntegerFilter());
        }
        return displayOrder;
    }

    public void setDisplayOrder(IntegerFilter displayOrder) {
        this.displayOrder = displayOrder;
    }

    public BooleanFilter getIsFeatured() {
        return isFeatured;
    }

    public Optional<BooleanFilter> optionalIsFeatured() {
        return Optional.ofNullable(isFeatured);
    }

    public BooleanFilter isFeatured() {
        if (isFeatured == null) {
            setIsFeatured(new BooleanFilter());
        }
        return isFeatured;
    }

    public void setIsFeatured(BooleanFilter isFeatured) {
        this.isFeatured = isFeatured;
    }

    public InstantFilter getCreatedAt() {
        return createdAt;
    }

    public Optional<InstantFilter> optionalCreatedAt() {
        return Optional.ofNullable(createdAt);
    }

    public InstantFilter createdAt() {
        if (createdAt == null) {
            setCreatedAt(new InstantFilter());
        }
        return createdAt;
    }

    public void setCreatedAt(InstantFilter createdAt) {
        this.createdAt = createdAt;
    }

    public InstantFilter getUpdatedAt() {
        return updatedAt;
    }

    public Optional<InstantFilter> optionalUpdatedAt() {
        return Optional.ofNullable(updatedAt);
    }

    public InstantFilter updatedAt() {
        if (updatedAt == null) {
            setUpdatedAt(new InstantFilter());
        }
        return updatedAt;
    }

    public void setUpdatedAt(InstantFilter updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LongFilter getListingId() {
        return listingId;
    }

    public Optional<LongFilter> optionalListingId() {
        return Optional.ofNullable(listingId);
    }

    public LongFilter listingId() {
        if (listingId == null) {
            setListingId(new LongFilter());
        }
        return listingId;
    }

    public void setListingId(LongFilter listingId) {
        this.listingId = listingId;
    }

    public LongFilter getGalleryId() {
        return galleryId;
    }

    public Optional<LongFilter> optionalGalleryId() {
        return Optional.ofNullable(galleryId);
    }

    public LongFilter galleryId() {
        if (galleryId == null) {
            setGalleryId(new LongFilter());
        }
        return galleryId;
    }

    public void setGalleryId(LongFilter galleryId) {
        this.galleryId = galleryId;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public Optional<Boolean> optionalDistinct() {
        return Optional.ofNullable(distinct);
    }

    public Boolean distinct() {
        if (distinct == null) {
            setDistinct(true);
        }
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ListingImageCriteria that = (ListingImageCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(altText, that.altText) &&
            Objects.equals(displayOrder, that.displayOrder) &&
            Objects.equals(isFeatured, that.isFeatured) &&
            Objects.equals(createdAt, that.createdAt) &&
            Objects.equals(updatedAt, that.updatedAt) &&
            Objects.equals(listingId, that.listingId) &&
            Objects.equals(galleryId, that.galleryId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, altText, displayOrder, isFeatured, createdAt, updatedAt, listingId, galleryId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ListingImageCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalAltText().map(f -> "altText=" + f + ", ").orElse("") +
            optionalDisplayOrder().map(f -> "displayOrder=" + f + ", ").orElse("") +
            optionalIsFeatured().map(f -> "isFeatured=" + f + ", ").orElse("") +
            optionalCreatedAt().map(f -> "createdAt=" + f + ", ").orElse("") +
            optionalUpdatedAt().map(f -> "updatedAt=" + f + ", ").orElse("") +
            optionalListingId().map(f -> "listingId=" + f + ", ").orElse("") +
            optionalGalleryId().map(f -> "galleryId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
