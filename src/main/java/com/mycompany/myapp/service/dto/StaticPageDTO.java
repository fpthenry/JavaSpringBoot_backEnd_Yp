package com.mycompany.myapp.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.StaticPage} entity.
 */
@Schema(description = "Trang tĩnh (wp_posts.post_type = 'page')")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class StaticPageDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 500)
    private String title;

    @NotNull
    @Size(max = 500)
    private String slug;

    @Lob
    private String content;

    @Size(max = 20)
    private String status;

    @Size(max = 255)
    private String template;

    private Integer menuOrder;

    private Instant createdAt;

    private Instant updatedAt;

    private UserDTO author;

    private StaticPageDTO parent;

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

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Integer getMenuOrder() {
        return menuOrder;
    }

    public void setMenuOrder(Integer menuOrder) {
        this.menuOrder = menuOrder;
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

    public StaticPageDTO getParent() {
        return parent;
    }

    public void setParent(StaticPageDTO parent) {
        this.parent = parent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StaticPageDTO)) {
            return false;
        }

        StaticPageDTO staticPageDTO = (StaticPageDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, staticPageDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "StaticPageDTO{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", slug='" + getSlug() + "'" +
            ", content='" + getContent() + "'" +
            ", status='" + getStatus() + "'" +
            ", template='" + getTemplate() + "'" +
            ", menuOrder=" + getMenuOrder() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            ", author=" + getAuthor() +
            ", parent=" + getParent() +
            "}";
    }
}
