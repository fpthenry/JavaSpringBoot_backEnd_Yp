package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Thẻ (bảng tag)
 */
@Entity
@Table(name = "tag")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "tag")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Tag implements Serializable {

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

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "tags")
    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "categories", "tags" }, allowSetters = true)
    private Set<BlogPost> blogPosts = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Tag id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWpTermId() {
        return this.wpTermId;
    }

    public Tag wpTermId(Long wpTermId) {
        this.setWpTermId(wpTermId);
        return this;
    }

    public void setWpTermId(Long wpTermId) {
        this.wpTermId = wpTermId;
    }

    public String getName() {
        return this.name;
    }

    public Tag name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return this.slug;
    }

    public Tag slug(String slug) {
        this.setSlug(slug);
        return this;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Set<BlogPost> getBlogPosts() {
        return this.blogPosts;
    }

    public void setBlogPosts(Set<BlogPost> blogPosts) {
        if (this.blogPosts != null) {
            this.blogPosts.forEach(i -> i.removeTag(this));
        }
        if (blogPosts != null) {
            blogPosts.forEach(i -> i.addTag(this));
        }
        this.blogPosts = blogPosts;
    }

    public Tag blogPosts(Set<BlogPost> blogPosts) {
        this.setBlogPosts(blogPosts);
        return this;
    }

    public Tag addBlogPost(BlogPost blogPost) {
        this.blogPosts.add(blogPost);
        blogPost.getTags().add(this);
        return this;
    }

    public Tag removeBlogPost(BlogPost blogPost) {
        this.blogPosts.remove(blogPost);
        blogPost.getTags().remove(this);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tag)) {
            return false;
        }
        return getId() != null && getId().equals(((Tag) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Tag{" +
            "id=" + getId() +
            ", wpTermId=" + getWpTermId() +
            ", name='" + getName() + "'" +
            ", slug='" + getSlug() + "'" +
            "}";
    }
}
