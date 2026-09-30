package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Bài viết tin tức (wp_posts.post_type = 'post')
 */
@Entity
@Table(name = "article")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "article")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Article implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Long)
    private Long id;

    @NotNull
    @Size(max = 500)
    @Column(name = "title", length = 500, nullable = false)
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
    private String title;

    @NotNull
    @Size(max = 500)
    @Column(name = "slug", length = 500, nullable = false, unique = true)
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
    @Column(name = "excerpt")
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
    private String excerpt;

    @Lob
    @Column(name = "content")
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
    private String content;

    @Size(max = 20)
    @Column(name = "status", length = 20)
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
    private String status;

    @Lob
    @Column(name = "featured_image_url")
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
    private String featuredImageUrl;

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

    @ManyToOne(fetch = FetchType.LAZY)
    private User author;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rel_article__categories",
        joinColumns = @JoinColumn(name = "article_id"),
        inverseJoinColumns = @JoinColumn(name = "categories_id")
    )
    @JsonIgnoreProperties(value = { "childrens", "parent", "articleses" }, allowSetters = true)
    private Set<ArticleCategory> categorieses = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rel_article__tags",
        joinColumns = @JoinColumn(name = "article_id"),
        inverseJoinColumns = @JoinColumn(name = "tags_id")
    )
    @JsonIgnoreProperties(value = { "articleses" }, allowSetters = true)
    private Set<ArticleTag> tagses = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Article id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public Article title(String title) {
        this.setTitle(title);
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return this.slug;
    }

    public Article slug(String slug) {
        this.setSlug(slug);
        return this;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getExcerpt() {
        return this.excerpt;
    }

    public Article excerpt(String excerpt) {
        this.setExcerpt(excerpt);
        return this;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public String getContent() {
        return this.content;
    }

    public Article content(String content) {
        this.setContent(content);
        return this;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return this.status;
    }

    public Article status(String status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFeaturedImageUrl() {
        return this.featuredImageUrl;
    }

    public Article featuredImageUrl(String featuredImageUrl) {
        this.setFeaturedImageUrl(featuredImageUrl);
        return this;
    }

    public void setFeaturedImageUrl(String featuredImageUrl) {
        this.featuredImageUrl = featuredImageUrl;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Article createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public Article updatedAt(Instant updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getAuthor() {
        return this.author;
    }

    public void setAuthor(User user) {
        this.author = user;
    }

    public Article author(User user) {
        this.setAuthor(user);
        return this;
    }

    public Set<ArticleCategory> getCategorieses() {
        return this.categorieses;
    }

    public void setCategorieses(Set<ArticleCategory> articleCategories) {
        this.categorieses = articleCategories;
    }

    public Article categorieses(Set<ArticleCategory> articleCategories) {
        this.setCategorieses(articleCategories);
        return this;
    }

    public Article addCategories(ArticleCategory articleCategory) {
        this.categorieses.add(articleCategory);
        return this;
    }

    public Article removeCategories(ArticleCategory articleCategory) {
        this.categorieses.remove(articleCategory);
        return this;
    }

    public Set<ArticleTag> getTagses() {
        return this.tagses;
    }

    public void setTagses(Set<ArticleTag> articleTags) {
        this.tagses = articleTags;
    }

    public Article tagses(Set<ArticleTag> articleTags) {
        this.setTagses(articleTags);
        return this;
    }

    public Article addTags(ArticleTag articleTag) {
        this.tagses.add(articleTag);
        return this;
    }

    public Article removeTags(ArticleTag articleTag) {
        this.tagses.remove(articleTag);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Article)) {
            return false;
        }
        return getId() != null && getId().equals(((Article) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Article{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", slug='" + getSlug() + "'" +
            ", excerpt='" + getExcerpt() + "'" +
            ", content='" + getContent() + "'" +
            ", status='" + getStatus() + "'" +
            ", featuredImageUrl='" + getFeaturedImageUrl() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
