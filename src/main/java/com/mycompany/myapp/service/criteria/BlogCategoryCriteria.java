package com.mycompany.myapp.service.criteria;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.mycompany.myapp.domain.BlogCategory} entity. This class is used
 * in {@link com.mycompany.myapp.web.rest.BlogCategoryResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /blog-categories?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class BlogCategoryCriteria implements Serializable, Criteria {

    @Serial
    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private LongFilter wpTermId;

    private StringFilter name;

    private StringFilter slug;

    private IntegerFilter postCount;

    private LongFilter parentId;

    private LongFilter blogPostId;

    private Boolean distinct;

    public BlogCategoryCriteria() {}

    public BlogCategoryCriteria(BlogCategoryCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.wpTermId = other.optionalWpTermId().map(LongFilter::copy).orElse(null);
        this.name = other.optionalName().map(StringFilter::copy).orElse(null);
        this.slug = other.optionalSlug().map(StringFilter::copy).orElse(null);
        this.postCount = other.optionalPostCount().map(IntegerFilter::copy).orElse(null);
        this.parentId = other.optionalParentId().map(LongFilter::copy).orElse(null);
        this.blogPostId = other.optionalBlogPostId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public BlogCategoryCriteria copy() {
        return new BlogCategoryCriteria(this);
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

    public LongFilter getWpTermId() {
        return wpTermId;
    }

    public Optional<LongFilter> optionalWpTermId() {
        return Optional.ofNullable(wpTermId);
    }

    public LongFilter wpTermId() {
        if (wpTermId == null) {
            setWpTermId(new LongFilter());
        }
        return wpTermId;
    }

    public void setWpTermId(LongFilter wpTermId) {
        this.wpTermId = wpTermId;
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

    public IntegerFilter getPostCount() {
        return postCount;
    }

    public Optional<IntegerFilter> optionalPostCount() {
        return Optional.ofNullable(postCount);
    }

    public IntegerFilter postCount() {
        if (postCount == null) {
            setPostCount(new IntegerFilter());
        }
        return postCount;
    }

    public void setPostCount(IntegerFilter postCount) {
        this.postCount = postCount;
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

    public LongFilter getBlogPostId() {
        return blogPostId;
    }

    public Optional<LongFilter> optionalBlogPostId() {
        return Optional.ofNullable(blogPostId);
    }

    public LongFilter blogPostId() {
        if (blogPostId == null) {
            setBlogPostId(new LongFilter());
        }
        return blogPostId;
    }

    public void setBlogPostId(LongFilter blogPostId) {
        this.blogPostId = blogPostId;
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
        final BlogCategoryCriteria that = (BlogCategoryCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(wpTermId, that.wpTermId) &&
            Objects.equals(name, that.name) &&
            Objects.equals(slug, that.slug) &&
            Objects.equals(postCount, that.postCount) &&
            Objects.equals(parentId, that.parentId) &&
            Objects.equals(blogPostId, that.blogPostId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, wpTermId, name, slug, postCount, parentId, blogPostId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "BlogCategoryCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalWpTermId().map(f -> "wpTermId=" + f + ", ").orElse("") +
            optionalName().map(f -> "name=" + f + ", ").orElse("") +
            optionalSlug().map(f -> "slug=" + f + ", ").orElse("") +
            optionalPostCount().map(f -> "postCount=" + f + ", ").orElse("") +
            optionalParentId().map(f -> "parentId=" + f + ", ").orElse("") +
            optionalBlogPostId().map(f -> "blogPostId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
