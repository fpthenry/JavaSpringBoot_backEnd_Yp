package com.mycompany.myapp.service.criteria;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.mycompany.myapp.domain.RedirectRule} entity. This class is used
 * in {@link com.mycompany.myapp.web.rest.RedirectRuleResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /redirect-rules?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RedirectRuleCriteria implements Serializable, Criteria {

    @Serial
    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private LongFilter sourceId;

    private StringFilter sourceSlug;

    private LongFilter destinationId;

    private StringFilter destinationSlug;

    private StringFilter objectType;

    private Boolean distinct;

    public RedirectRuleCriteria() {}

    public RedirectRuleCriteria(RedirectRuleCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.sourceId = other.optionalSourceId().map(LongFilter::copy).orElse(null);
        this.sourceSlug = other.optionalSourceSlug().map(StringFilter::copy).orElse(null);
        this.destinationId = other.optionalDestinationId().map(LongFilter::copy).orElse(null);
        this.destinationSlug = other.optionalDestinationSlug().map(StringFilter::copy).orElse(null);
        this.objectType = other.optionalObjectType().map(StringFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public RedirectRuleCriteria copy() {
        return new RedirectRuleCriteria(this);
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

    public LongFilter getSourceId() {
        return sourceId;
    }

    public Optional<LongFilter> optionalSourceId() {
        return Optional.ofNullable(sourceId);
    }

    public LongFilter sourceId() {
        if (sourceId == null) {
            setSourceId(new LongFilter());
        }
        return sourceId;
    }

    public void setSourceId(LongFilter sourceId) {
        this.sourceId = sourceId;
    }

    public StringFilter getSourceSlug() {
        return sourceSlug;
    }

    public Optional<StringFilter> optionalSourceSlug() {
        return Optional.ofNullable(sourceSlug);
    }

    public StringFilter sourceSlug() {
        if (sourceSlug == null) {
            setSourceSlug(new StringFilter());
        }
        return sourceSlug;
    }

    public void setSourceSlug(StringFilter sourceSlug) {
        this.sourceSlug = sourceSlug;
    }

    public LongFilter getDestinationId() {
        return destinationId;
    }

    public Optional<LongFilter> optionalDestinationId() {
        return Optional.ofNullable(destinationId);
    }

    public LongFilter destinationId() {
        if (destinationId == null) {
            setDestinationId(new LongFilter());
        }
        return destinationId;
    }

    public void setDestinationId(LongFilter destinationId) {
        this.destinationId = destinationId;
    }

    public StringFilter getDestinationSlug() {
        return destinationSlug;
    }

    public Optional<StringFilter> optionalDestinationSlug() {
        return Optional.ofNullable(destinationSlug);
    }

    public StringFilter destinationSlug() {
        if (destinationSlug == null) {
            setDestinationSlug(new StringFilter());
        }
        return destinationSlug;
    }

    public void setDestinationSlug(StringFilter destinationSlug) {
        this.destinationSlug = destinationSlug;
    }

    public StringFilter getObjectType() {
        return objectType;
    }

    public Optional<StringFilter> optionalObjectType() {
        return Optional.ofNullable(objectType);
    }

    public StringFilter objectType() {
        if (objectType == null) {
            setObjectType(new StringFilter());
        }
        return objectType;
    }

    public void setObjectType(StringFilter objectType) {
        this.objectType = objectType;
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
        final RedirectRuleCriteria that = (RedirectRuleCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(sourceId, that.sourceId) &&
            Objects.equals(sourceSlug, that.sourceSlug) &&
            Objects.equals(destinationId, that.destinationId) &&
            Objects.equals(destinationSlug, that.destinationSlug) &&
            Objects.equals(objectType, that.objectType) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sourceId, sourceSlug, destinationId, destinationSlug, objectType, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RedirectRuleCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalSourceId().map(f -> "sourceId=" + f + ", ").orElse("") +
            optionalSourceSlug().map(f -> "sourceSlug=" + f + ", ").orElse("") +
            optionalDestinationId().map(f -> "destinationId=" + f + ", ").orElse("") +
            optionalDestinationSlug().map(f -> "destinationSlug=" + f + ", ").orElse("") +
            optionalObjectType().map(f -> "objectType=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
