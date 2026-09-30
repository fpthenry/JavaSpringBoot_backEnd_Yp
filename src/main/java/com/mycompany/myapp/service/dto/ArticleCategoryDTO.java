package com.mycompany.myapp.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.ArticleCategory} entity.
 */
@Schema(description = "Chuyên mục blog (taxonomy 'category')")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ArticleCategoryDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 255)
    private String name;

    @NotNull
    @Size(max = 255)
    private String slug;

    @Lob
    private String description;

    @Min(value = 0)
    private Integer count;

    private ArticleCategoryDTO parent;

    private Set<ArticleDTO> articleses = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public ArticleCategoryDTO getParent() {
        return parent;
    }

    public void setParent(ArticleCategoryDTO parent) {
        this.parent = parent;
    }

    public Set<ArticleDTO> getArticleses() {
        return articleses;
    }

    public void setArticleses(Set<ArticleDTO> articleses) {
        this.articleses = articleses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ArticleCategoryDTO)) {
            return false;
        }

        ArticleCategoryDTO articleCategoryDTO = (ArticleCategoryDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, articleCategoryDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ArticleCategoryDTO{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", slug='" + getSlug() + "'" +
            ", description='" + getDescription() + "'" +
            ", count=" + getCount() +
            ", parent=" + getParent() +
            ", articleses=" + getArticleses() +
            "}";
    }
}
