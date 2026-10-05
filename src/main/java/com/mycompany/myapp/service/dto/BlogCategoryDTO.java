package com.mycompany.myapp.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.BlogCategory} entity.
 */
@Schema(description = "Danh mục bài viết (taxonomy category của WordPress), dạng cây")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class BlogCategoryDTO implements Serializable {

    private Long id;

    @Schema(description = "wp_terms.term_id")
    private Long wpTermId;

    @NotNull
    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String slug;

    @Lob
    private String description;

    @Schema(description = "Số bài viết theo WordPress")
    private Integer postCount;

    private BlogCategoryDTO parent;

    private Set<BlogPostDTO> blogPosts = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWpTermId() {
        return wpTermId;
    }

    public void setWpTermId(Long wpTermId) {
        this.wpTermId = wpTermId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPostCount() {
        return postCount;
    }

    public void setPostCount(Integer postCount) {
        this.postCount = postCount;
    }

    public BlogCategoryDTO getParent() {
        return parent;
    }

    public void setParent(BlogCategoryDTO parent) {
        this.parent = parent;
    }

    public Set<BlogPostDTO> getBlogPosts() {
        return blogPosts;
    }

    public void setBlogPosts(Set<BlogPostDTO> blogPosts) {
        this.blogPosts = blogPosts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BlogCategoryDTO)) {
            return false;
        }

        BlogCategoryDTO blogCategoryDTO = (BlogCategoryDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, blogCategoryDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "BlogCategoryDTO{" +
            "id=" + getId() +
            ", wpTermId=" + getWpTermId() +
            ", name='" + getName() + "'" +
            ", slug='" + getSlug() + "'" +
            ", description='" + getDescription() + "'" +
            ", postCount=" + getPostCount() +
            ", parent=" + getParent() +
            ", blogPosts=" + getBlogPosts() +
            "}";
    }
}
