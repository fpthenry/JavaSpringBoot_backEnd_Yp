package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Danh mục bài viết (taxonomy category của WordPress), dạng cây
 */
@Entity
@Table(name = "blog_category")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "blogcategory")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class BlogCategory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Long)
    private Long id;

    /**
     * wp_terms.term_id
     */
    @Column(name = "wp_term_id", unique = true)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Long)
    private Long wpTermId;

    @NotNull
    @Size(max = 255)
    @Column(name = "name", length = 255, nullable = false)
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
    private String name;

    @Size(max = 255)
    @Column(name = "slug", length = 255)
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
    private String slug;

    @Lob
    @Column(name = "description")
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
    private String description;

    /**
     * Số bài viết theo WordPress
     */
    @Column(name = "post_count")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Integer)
    private Integer postCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "parent", "blogPosts" }, allowSetters = true)
    // Sửa tay: không ghi quan hệ vào Elasticsearch (tránh lazy-load khi reindex)
    @org.springframework.data.annotation.Transient
    private BlogCategory parent;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "categories")
    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "categories", "tags" }, allowSetters = true)
    private Set<BlogPost> blogPosts = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public BlogCategory id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWpTermId() {
        return this.wpTermId;
    }

    public BlogCategory wpTermId(Long wpTermId) {
        this.setWpTermId(wpTermId);
        return this;
    }

    public void setWpTermId(Long wpTermId) {
        this.wpTermId = wpTermId;
    }

    public String getName() {
        return this.name;
    }

    public BlogCategory name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return this.slug;
    }

    public BlogCategory slug(String slug) {
        this.setSlug(slug);
        return this;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return this.description;
    }

    public BlogCategory description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPostCount() {
        return this.postCount;
    }

    public BlogCategory postCount(Integer postCount) {
        this.setPostCount(postCount);
        return this;
    }

    public void setPostCount(Integer postCount) {
        this.postCount = postCount;
    }

    public BlogCategory getParent() {
        return this.parent;
    }

    public void setParent(BlogCategory blogCategory) {
        this.parent = blogCategory;
    }

    public BlogCategory parent(BlogCategory blogCategory) {
        this.setParent(blogCategory);
        return this;
    }

    public Set<BlogPost> getBlogPosts() {
        return this.blogPosts;
    }

    public void setBlogPosts(Set<BlogPost> blogPosts) {
        if (this.blogPosts != null) {
            this.blogPosts.forEach(i -> i.removeCategory(this));
        }
        if (blogPosts != null) {
            blogPosts.forEach(i -> i.addCategory(this));
        }
        this.blogPosts = blogPosts;
    }

    public BlogCategory blogPosts(Set<BlogPost> blogPosts) {
        this.setBlogPosts(blogPosts);
        return this;
    }

    public BlogCategory addBlogPost(BlogPost blogPost) {
        this.blogPosts.add(blogPost);
        blogPost.getCategories().add(this);
        return this;
    }

    public BlogCategory removeBlogPost(BlogPost blogPost) {
        this.blogPosts.remove(blogPost);
        blogPost.getCategories().remove(this);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BlogCategory)) {
            return false;
        }
        return getId() != null && getId().equals(((BlogCategory) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "BlogCategory{" +
            "id=" + getId() +
            ", wpTermId=" + getWpTermId() +
            ", name='" + getName() + "'" +
            ", slug='" + getSlug() + "'" +
            ", description='" + getDescription() + "'" +
            ", postCount=" + getPostCount() +
            "}";
    }
}
