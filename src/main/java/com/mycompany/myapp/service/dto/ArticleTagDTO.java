package com.mycompany.myapp.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.ArticleTag} entity.
 */
@Schema(description = "Thẻ blog (taxonomy 'post_tag')")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ArticleTagDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 255)
    private String name;

    @NotNull
    @Size(max = 255)
    private String slug;

    @Min(value = 0)
    private Integer count;

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

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
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
        if (!(o instanceof ArticleTagDTO)) {
            return false;
        }

        ArticleTagDTO articleTagDTO = (ArticleTagDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, articleTagDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ArticleTagDTO{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", slug='" + getSlug() + "'" +
            ", count=" + getCount() +
            ", articleses=" + getArticleses() +
            "}";
    }
}
