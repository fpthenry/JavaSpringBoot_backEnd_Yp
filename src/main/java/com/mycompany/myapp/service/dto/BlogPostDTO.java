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
 * A DTO for the {@link com.mycompany.myapp.domain.BlogPost} entity.
 */
@Schema(description = "Bài viết tin tức (bảng blog_post)")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class BlogPostDTO implements Serializable {

    private Long id;

    // Sửa tay: không bắt buộc (bài soạn mới không có id WordPress)
    @Schema(description = "wp_posts.ID; null với bài soạn mới trên trang quản trị")
    private Long wpId;

    @NotNull
    @Size(max = 500)
    private String title;

    @Size(max = 500)
    private String slug;

    @Lob
    private String content;

    @Lob
    private String excerpt;

    @Size(max = 500)
    @Schema(description = "URL ảnh đại diện (featured image)")
    private String thumbnail;

    @Size(max = 50)
    private String status;

    private Integer viewCount;

    private Instant publishedAt;

    private Instant createdAt;

    private Instant updatedAt;

    @Size(max = 255)
    @Schema(description = "Tên tác giả trên WordPress, vd: ADMIN HCM")
    private String authorName;

    private Set<BlogCategoryDTO> categories = new HashSet<>();

    private Set<TagDTO> tags = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWpId() {
        return wpId;
    }

    public void setWpId(Long wpId) {
        this.wpId = wpId;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
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

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Set<BlogCategoryDTO> getCategories() {
        return categories;
    }

    public void setCategories(Set<BlogCategoryDTO> categories) {
        this.categories = categories;
    }

    public Set<TagDTO> getTags() {
        return tags;
    }

    public void setTags(Set<TagDTO> tags) {
        this.tags = tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BlogPostDTO)) {
            return false;
        }

        BlogPostDTO blogPostDTO = (BlogPostDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, blogPostDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "BlogPostDTO{" +
            "id=" + getId() +
            ", wpId=" + getWpId() +
            ", title='" + getTitle() + "'" +
            ", slug='" + getSlug() + "'" +
            ", content='" + getContent() + "'" +
            ", excerpt='" + getExcerpt() + "'" +
            ", thumbnail='" + getThumbnail() + "'" +
            ", status='" + getStatus() + "'" +
            ", viewCount=" + getViewCount() +
            ", publishedAt='" + getPublishedAt() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            ", authorName='" + getAuthorName() + "'" +
            ", categories=" + getCategories() +
            ", tags=" + getTags() +
            "}";
    }
}
