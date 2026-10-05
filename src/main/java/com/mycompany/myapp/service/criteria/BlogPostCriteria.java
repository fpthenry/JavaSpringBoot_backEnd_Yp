package com.mycompany.myapp.service.criteria;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.mycompany.myapp.domain.BlogPost} entity. This class is used
 * in {@link com.mycompany.myapp.web.rest.BlogPostResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /blog-posts?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class BlogPostCriteria implements Serializable, Criteria {

    @Serial
    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private LongFilter wpId;

    private StringFilter title;

    private StringFilter slug;

    private StringFilter thumbnail;

    private StringFilter status;

    private IntegerFilter viewCount;

    private InstantFilter publishedAt;

    private InstantFilter createdAt;

    private InstantFilter updatedAt;

    private StringFilter authorName;

    private LongFilter categoryId;

    private LongFilter tagId;

    private Boolean distinct;

    public BlogPostCriteria() {}

    public BlogPostCriteria(BlogPostCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.wpId = other.optionalWpId().map(LongFilter::copy).orElse(null);
        this.title = other.optionalTitle().map(StringFilter::copy).orElse(null);
        this.slug = other.optionalSlug().map(StringFilter::copy).orElse(null);
        this.thumbnail = other.optionalThumbnail().map(StringFilter::copy).orElse(null);
        this.status = other.optionalStatus().map(StringFilter::copy).orElse(null);
        this.viewCount = other.optionalViewCount().map(IntegerFilter::copy).orElse(null);
        this.publishedAt = other.optionalPublishedAt().map(InstantFilter::copy).orElse(null);
        this.createdAt = other.optionalCreatedAt().map(InstantFilter::copy).orElse(null);
        this.updatedAt = other.optionalUpdatedAt().map(InstantFilter::copy).orElse(null);
        this.authorName = other.optionalAuthorName().map(StringFilter::copy).orElse(null);
        this.categoryId = other.optionalCategoryId().map(LongFilter::copy).orElse(null);
        this.tagId = other.optionalTagId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public BlogPostCriteria copy() {
        return new BlogPostCriteria(this);
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

    public LongFilter getWpId() {
        return wpId;
    }

    public Optional<LongFilter> optionalWpId() {
        return Optional.ofNullable(wpId);
    }

    public LongFilter wpId() {
        if (wpId == null) {
            setWpId(new LongFilter());
        }
        return wpId;
    }

    public void setWpId(LongFilter wpId) {
        this.wpId = wpId;
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

    public StringFilter getThumbnail() {
        return thumbnail;
    }

    public Optional<StringFilter> optionalThumbnail() {
        return Optional.ofNullable(thumbnail);
    }

    public StringFilter thumbnail() {
        if (thumbnail == null) {
            setThumbnail(new StringFilter());
        }
        return thumbnail;
    }

    public void setThumbnail(StringFilter thumbnail) {
        this.thumbnail = thumbnail;
    }

    public StringFilter getStatus() {
        return status;
    }

    public Optional<StringFilter> optionalStatus() {
        return Optional.ofNullable(status);
    }

    public StringFilter status() {
        if (status == null) {
            setStatus(new StringFilter());
        }
        return status;
    }

    public void setStatus(StringFilter status) {
        this.status = status;
    }

    public IntegerFilter getViewCount() {
        return viewCount;
    }

    public Optional<IntegerFilter> optionalViewCount() {
        return Optional.ofNullable(viewCount);
    }

    public IntegerFilter viewCount() {
        if (viewCount == null) {
            setViewCount(new IntegerFilter());
        }
        return viewCount;
    }

    public void setViewCount(IntegerFilter viewCount) {
        this.viewCount = viewCount;
    }

    public InstantFilter getPublishedAt() {
        return publishedAt;
    }

    public Optional<InstantFilter> optionalPublishedAt() {
        return Optional.ofNullable(publishedAt);
    }

    public InstantFilter publishedAt() {
        if (publishedAt == null) {
            setPublishedAt(new InstantFilter());
        }
        return publishedAt;
    }

    public void setPublishedAt(InstantFilter publishedAt) {
        this.publishedAt = publishedAt;
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

    public StringFilter getAuthorName() {
        return authorName;
    }

    public Optional<StringFilter> optionalAuthorName() {
        return Optional.ofNullable(authorName);
    }

    public StringFilter authorName() {
        if (authorName == null) {
            setAuthorName(new StringFilter());
        }
        return authorName;
    }

    public void setAuthorName(StringFilter authorName) {
        this.authorName = authorName;
    }

    public LongFilter getCategoryId() {
        return categoryId;
    }

    public Optional<LongFilter> optionalCategoryId() {
        return Optional.ofNullable(categoryId);
    }

    public LongFilter categoryId() {
        if (categoryId == null) {
            setCategoryId(new LongFilter());
        }
        return categoryId;
    }

    public void setCategoryId(LongFilter categoryId) {
        this.categoryId = categoryId;
    }

    public LongFilter getTagId() {
        return tagId;
    }

    public Optional<LongFilter> optionalTagId() {
        return Optional.ofNullable(tagId);
    }

    public LongFilter tagId() {
        if (tagId == null) {
            setTagId(new LongFilter());
        }
        return tagId;
    }

    public void setTagId(LongFilter tagId) {
        this.tagId = tagId;
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
        final BlogPostCriteria that = (BlogPostCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(wpId, that.wpId) &&
            Objects.equals(title, that.title) &&
            Objects.equals(slug, that.slug) &&
            Objects.equals(thumbnail, that.thumbnail) &&
            Objects.equals(status, that.status) &&
            Objects.equals(viewCount, that.viewCount) &&
            Objects.equals(publishedAt, that.publishedAt) &&
            Objects.equals(createdAt, that.createdAt) &&
            Objects.equals(updatedAt, that.updatedAt) &&
            Objects.equals(authorName, that.authorName) &&
            Objects.equals(categoryId, that.categoryId) &&
            Objects.equals(tagId, that.tagId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            wpId,
            title,
            slug,
            thumbnail,
            status,
            viewCount,
            publishedAt,
            createdAt,
            updatedAt,
            authorName,
            categoryId,
            tagId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "BlogPostCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalWpId().map(f -> "wpId=" + f + ", ").orElse("") +
            optionalTitle().map(f -> "title=" + f + ", ").orElse("") +
            optionalSlug().map(f -> "slug=" + f + ", ").orElse("") +
            optionalThumbnail().map(f -> "thumbnail=" + f + ", ").orElse("") +
            optionalStatus().map(f -> "status=" + f + ", ").orElse("") +
            optionalViewCount().map(f -> "viewCount=" + f + ", ").orElse("") +
            optionalPublishedAt().map(f -> "publishedAt=" + f + ", ").orElse("") +
            optionalCreatedAt().map(f -> "createdAt=" + f + ", ").orElse("") +
            optionalUpdatedAt().map(f -> "updatedAt=" + f + ", ").orElse("") +
            optionalAuthorName().map(f -> "authorName=" + f + ", ").orElse("") +
            optionalCategoryId().map(f -> "categoryId=" + f + ", ").orElse("") +
            optionalTagId().map(f -> "tagId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
