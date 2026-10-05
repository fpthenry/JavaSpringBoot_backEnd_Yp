package com.mycompany.myapp.service.criteria;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.mycompany.myapp.domain.GalleryImage} entity. This class is used
 * in {@link com.mycompany.myapp.web.rest.GalleryImageResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /gallery-images?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class GalleryImageCriteria implements Serializable, Criteria {

    @Serial
    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter title;

    private StringFilter imageUrl;

    private StringFilter linkUrl;

    private StringFilter altText;

    private IntegerFilter displayOrder;

    private BooleanFilter active;

    private InstantFilter startAt;

    private InstantFilter endAt;

    private BooleanFilter openInNewTab;

    private LongFilter galleryId;

    private Boolean distinct;

    public GalleryImageCriteria() {}

    public GalleryImageCriteria(GalleryImageCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.title = other.optionalTitle().map(StringFilter::copy).orElse(null);
        this.imageUrl = other.optionalImageUrl().map(StringFilter::copy).orElse(null);
        this.linkUrl = other.optionalLinkUrl().map(StringFilter::copy).orElse(null);
        this.altText = other.optionalAltText().map(StringFilter::copy).orElse(null);
        this.displayOrder = other.optionalDisplayOrder().map(IntegerFilter::copy).orElse(null);
        this.active = other.optionalActive().map(BooleanFilter::copy).orElse(null);
        this.startAt = other.optionalStartAt().map(InstantFilter::copy).orElse(null);
        this.endAt = other.optionalEndAt().map(InstantFilter::copy).orElse(null);
        this.openInNewTab = other.optionalOpenInNewTab().map(BooleanFilter::copy).orElse(null);
        this.galleryId = other.optionalGalleryId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public GalleryImageCriteria copy() {
        return new GalleryImageCriteria(this);
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

    public StringFilter getTitle() {
        return title;
    }

    public Optional<StringFilter> optionalTitle() {
        return Optional.ofNullable(title);
    }

    public StringFilter title() {
        if (title == null) {
            setTitle(new StringFilter());
        }
        return title;
    }

    public void setTitle(StringFilter title) {
        this.title = title;
    }

    public StringFilter getImageUrl() {
        return imageUrl;
    }

    public Optional<StringFilter> optionalImageUrl() {
        return Optional.ofNullable(imageUrl);
    }

    public StringFilter imageUrl() {
        if (imageUrl == null) {
            setImageUrl(new StringFilter());
        }
        return imageUrl;
    }

    public void setImageUrl(StringFilter imageUrl) {
        this.imageUrl = imageUrl;
    }

    public StringFilter getLinkUrl() {
        return linkUrl;
    }

    public Optional<StringFilter> optionalLinkUrl() {
        return Optional.ofNullable(linkUrl);
    }

    public StringFilter linkUrl() {
        if (linkUrl == null) {
            setLinkUrl(new StringFilter());
        }
        return linkUrl;
    }

    public void setLinkUrl(StringFilter linkUrl) {
        this.linkUrl = linkUrl;
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

    public BooleanFilter getActive() {
        return active;
    }

    public Optional<BooleanFilter> optionalActive() {
        return Optional.ofNullable(active);
    }

    public BooleanFilter active() {
        if (active == null) {
            setActive(new BooleanFilter());
        }
        return active;
    }

    public void setActive(BooleanFilter active) {
        this.active = active;
    }

    public InstantFilter getStartAt() {
        return startAt;
    }

    public Optional<InstantFilter> optionalStartAt() {
        return Optional.ofNullable(startAt);
    }

    public InstantFilter startAt() {
        if (startAt == null) {
            setStartAt(new InstantFilter());
        }
        return startAt;
    }

    public void setStartAt(InstantFilter startAt) {
        this.startAt = startAt;
    }

    public InstantFilter getEndAt() {
        return endAt;
    }

    public Optional<InstantFilter> optionalEndAt() {
        return Optional.ofNullable(endAt);
    }

    public InstantFilter endAt() {
        if (endAt == null) {
            setEndAt(new InstantFilter());
        }
        return endAt;
    }

    public void setEndAt(InstantFilter endAt) {
        this.endAt = endAt;
    }

    public BooleanFilter getOpenInNewTab() {
        return openInNewTab;
    }

    public Optional<BooleanFilter> optionalOpenInNewTab() {
        return Optional.ofNullable(openInNewTab);
    }

    public BooleanFilter openInNewTab() {
        if (openInNewTab == null) {
            setOpenInNewTab(new BooleanFilter());
        }
        return openInNewTab;
    }

    public void setOpenInNewTab(BooleanFilter openInNewTab) {
        this.openInNewTab = openInNewTab;
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
        final GalleryImageCriteria that = (GalleryImageCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(title, that.title) &&
            Objects.equals(imageUrl, that.imageUrl) &&
            Objects.equals(linkUrl, that.linkUrl) &&
            Objects.equals(altText, that.altText) &&
            Objects.equals(displayOrder, that.displayOrder) &&
            Objects.equals(active, that.active) &&
            Objects.equals(startAt, that.startAt) &&
            Objects.equals(endAt, that.endAt) &&
            Objects.equals(openInNewTab, that.openInNewTab) &&
            Objects.equals(galleryId, that.galleryId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, imageUrl, linkUrl, altText, displayOrder, active, startAt, endAt, openInNewTab, galleryId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "GalleryImageCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalTitle().map(f -> "title=" + f + ", ").orElse("") +
            optionalImageUrl().map(f -> "imageUrl=" + f + ", ").orElse("") +
            optionalLinkUrl().map(f -> "linkUrl=" + f + ", ").orElse("") +
            optionalAltText().map(f -> "altText=" + f + ", ").orElse("") +
            optionalDisplayOrder().map(f -> "displayOrder=" + f + ", ").orElse("") +
            optionalActive().map(f -> "active=" + f + ", ").orElse("") +
            optionalStartAt().map(f -> "startAt=" + f + ", ").orElse("") +
            optionalEndAt().map(f -> "endAt=" + f + ", ").orElse("") +
            optionalOpenInNewTab().map(f -> "openInNewTab=" + f + ", ").orElse("") +
            optionalGalleryId().map(f -> "galleryId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
