package com.mycompany.myapp.service.criteria;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.mycompany.myapp.domain.Category} entity. This class is used
 * in {@link com.mycompany.myapp.web.rest.CategoryResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /categories?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CategoryCriteria implements Serializable, Criteria {

    @Serial
    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter name;

    private StringFilter slug;

    private IntegerFilter count;

    private StringFilter industryCode;

    private IntegerFilter displayOrder;

    private InstantFilter createdAt;

    private InstantFilter updatedAt;

    private LongFilter childrenId;

    private LongFilter parentId;

    private LongFilter listingsId;

    private Boolean distinct;

    public CategoryCriteria() {}

    public CategoryCriteria(CategoryCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.name = other.optionalName().map(StringFilter::copy).orElse(null);
        this.slug = other.optionalSlug().map(StringFilter::copy).orElse(null);
        this.count = other.optionalCount().map(IntegerFilter::copy).orElse(null);
        this.industryCode = other.optionalIndustryCode().map(StringFilter::copy).orElse(null);
        this.displayOrder = other.optionalDisplayOrder().map(IntegerFilter::copy).orElse(null);
        this.createdAt = other.optionalCreatedAt().map(InstantFilter::copy).orElse(null);
        this.updatedAt = other.optionalUpdatedAt().map(InstantFilter::copy).orElse(null);
        this.childrenId = other.optionalChildrenId().map(LongFilter::copy).orElse(null);
        this.parentId = other.optionalParentId().map(LongFilter::copy).orElse(null);
        this.listingsId = other.optionalListingsId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public CategoryCriteria copy() {
        return new CategoryCriteria(this);
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

    public StringFilter getName() {
        return name;
    }

    public Optional<StringFilter> optionalName() {
        return Optional.ofNullable(name);
    }

    public StringFilter name() {
        if (name == null) {
            setName(new StringFilter());
        }
        return name;
    }

    public void setName(StringFilter name) {
        this.name = name;
    }

    public StringFilter getSlug() {
        return slug;
    }

    public Optional<StringFilter> optionalSlug() {
        return Optional.ofNullable(slug);
    }

    public StringFilter slug() {
        if (slug == null) {
            setSlug(new StringFilter());
        }
        return slug;
    }

    public void setSlug(StringFilter slug) {
        this.slug = slug;
    }

    public IntegerFilter getCount() {
        return count;
    }

    public Optional<IntegerFilter> optionalCount() {
        return Optional.ofNullable(count);
    }

    public IntegerFilter count() {
        if (count == null) {
            setCount(new IntegerFilter());
        }
        return count;
    }

    public void setCount(IntegerFilter count) {
        this.count = count;
    }

    public StringFilter getIndustryCode() {
        return industryCode;
    }

    public Optional<StringFilter> optionalIndustryCode() {
        return Optional.ofNullable(industryCode);
    }

    public StringFilter industryCode() {
        if (industryCode == null) {
            setIndustryCode(new StringFilter());
        }
        return industryCode;
    }

    public void setIndustryCode(StringFilter industryCode) {
        this.industryCode = industryCode;
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

    public LongFilter getChildrenId() {
        return childrenId;
    }

    public Optional<LongFilter> optionalChildrenId() {
        return Optional.ofNullable(childrenId);
    }

    public LongFilter childrenId() {
        if (childrenId == null) {
            setChildrenId(new LongFilter());
        }
        return childrenId;
    }

    public void setChildrenId(LongFilter childrenId) {
        this.childrenId = childrenId;
    }

    public LongFilter getParentId() {
        return parentId;
    }

    public Optional<LongFilter> optionalParentId() {
        return Optional.ofNullable(parentId);
    }

    public LongFilter parentId() {
        if (parentId == null) {
            setParentId(new LongFilter());
        }
        return parentId;
    }

    public void setParentId(LongFilter parentId) {
        this.parentId = parentId;
    }

    public LongFilter getListingsId() {
        return listingsId;
    }

    public Optional<LongFilter> optionalListingsId() {
        return Optional.ofNullable(listingsId);
    }

    public LongFilter listingsId() {
        if (listingsId == null) {
            setListingsId(new LongFilter());
        }
        return listingsId;
    }

    public void setListingsId(LongFilter listingsId) {
        this.listingsId = listingsId;
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
        final CategoryCriteria that = (CategoryCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(name, that.name) &&
            Objects.equals(slug, that.slug) &&
            Objects.equals(count, that.count) &&
            Objects.equals(industryCode, that.industryCode) &&
            Objects.equals(displayOrder, that.displayOrder) &&
            Objects.equals(createdAt, that.createdAt) &&
            Objects.equals(updatedAt, that.updatedAt) &&
            Objects.equals(childrenId, that.childrenId) &&
            Objects.equals(parentId, that.parentId) &&
            Objects.equals(listingsId, that.listingsId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            name,
            slug,
            count,
            industryCode,
            displayOrder,
            createdAt,
            updatedAt,
            childrenId,
            parentId,
            listingsId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CategoryCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalName().map(f -> "name=" + f + ", ").orElse("") +
            optionalSlug().map(f -> "slug=" + f + ", ").orElse("") +
            optionalCount().map(f -> "count=" + f + ", ").orElse("") +
            optionalIndustryCode().map(f -> "industryCode=" + f + ", ").orElse("") +
            optionalDisplayOrder().map(f -> "displayOrder=" + f + ", ").orElse("") +
            optionalCreatedAt().map(f -> "createdAt=" + f + ", ").orElse("") +
            optionalUpdatedAt().map(f -> "updatedAt=" + f + ", ").orElse("") +
            optionalChildrenId().map(f -> "childrenId=" + f + ", ").orElse("") +
            optionalParentId().map(f -> "parentId=" + f + ", ").orElse("") +
            optionalListingsId().map(f -> "listingsId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
