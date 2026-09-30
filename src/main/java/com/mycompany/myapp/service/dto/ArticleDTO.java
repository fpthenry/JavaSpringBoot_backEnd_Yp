package com.mycompany.myapp.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.Article} entity.
 */
@Schema(description = "Bài viết tin tức (wp_posts.post_type = 'post')")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ArticleDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 500)
    private String title;

    @NotNull
    @Size(max = 500)
    private String slug;

    @Lob
    private String excerpt;

    @Lob
    private String content;

    @Size(max = 20)
    private String status;

    @Lob
    private String featuredImageUrl;

    private Instant createdAt;

    private Instant updatedAt;

    private UserDTO author;

    private Set<ArticleCategoryDTO> categorieses = new HashSet<>();

    private Set<ArticleTagDTO> tagses = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFeaturedImageUrl() {
        return featuredImageUrl;
    }

    public void setFeaturedImageUrl(String featuredImageUrl) {
        this.featuredImageUrl = featuredImageUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UserDTO getAuthor() {
        return author;
    }

    public void setAuthor(UserDTO author) {
        this.author = author;
    }

    public Set<ArticleCategoryDTO> getCategorieses() {
        return categorieses;
    }

    public void setCategorieses(Set<ArticleCategoryDTO> categorieses) {
        this.categorieses = categorieses;
    }

    public Set<ArticleTagDTO> getTagses() {
        return tagses;
    }

    public void setTagses(Set<ArticleTagDTO> tagses) {
        this.tagses = tagses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ArticleDTO)) {
            return false;
        }

        ArticleDTO articleDTO = (ArticleDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, articleDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ArticleDTO{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", slug='" + getSlug() + "'" +
            ", excerpt='" + getExcerpt() + "'" +
            ", content='" + getContent() + "'" +
            ", status='" + getStatus() + "'" +
            ", featuredImageUrl='" + getFeaturedImageUrl() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            ", author=" + getAuthor() +
            ", categorieses=" + getCategorieses() +
            ", tagses=" + getTagses() +
            "}";
    }
}
