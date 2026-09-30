package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Chuyên mục blog (taxonomy 'category')
 */
@Entity
@Table(name = "article_category")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "articlecategory")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ArticleCategory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Long)
    private Long id;

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

    @NotNull
    @Size(max = 255)
    @Column(name = "slug", length = 255, nullable = false, unique = true)
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

    @Min(value = 0)
    @Column(name = "count")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Integer)
    private Integer count;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "parent")
    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "childrens", "parent", "articleses" }, allowSetters = true)
    private Set<ArticleCategory> childrens = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "childrens", "parent", "articleses" }, allowSetters = true)
    private ArticleCategory parent;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "categorieses")
    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "author", "categorieses", "tagses" }, allowSetters = true)
    private Set<Article> articleses = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public ArticleCategory id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public ArticleCategory name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return this.slug;
    }

    public ArticleCategory slug(String slug) {
        this.setSlug(slug);
        return this;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return this.description;
    }

    public ArticleCategory description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCount() {
        return this.count;
    }

    public ArticleCategory count(Integer count) {
        this.setCount(count);
        return this;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Set<ArticleCategory> getChildrens() {
        return this.childrens;
    }

    public void setChildrens(Set<ArticleCategory> articleCategories) {
        if (this.childrens != null) {
            this.childrens.forEach(i -> i.setParent(null));
        }
        if (articleCategories != null) {
            articleCategories.forEach(i -> i.setParent(this));
        }
        this.childrens = articleCategories;
    }

    public ArticleCategory childrens(Set<ArticleCategory> articleCategories) {
        this.setChildrens(articleCategories);
        return this;
    }

    public ArticleCategory addChildren(ArticleCategory articleCategory) {
        this.childrens.add(articleCategory);
        articleCategory.setParent(this);
        return this;
    }

    public ArticleCategory removeChildren(ArticleCategory articleCategory) {
        this.childrens.remove(articleCategory);
        articleCategory.setParent(null);
        return this;
    }

    public ArticleCategory getParent() {
        return this.parent;
    }

    public void setParent(ArticleCategory articleCategory) {
        this.parent = articleCategory;
    }

    public ArticleCategory parent(ArticleCategory articleCategory) {
        this.setParent(articleCategory);
        return this;
    }

    public Set<Article> getArticleses() {
        return this.articleses;
    }

    public void setArticleses(Set<Article> articles) {
        if (this.articleses != null) {
            this.articleses.forEach(i -> i.removeCategories(this));
        }
        if (articles != null) {
            articles.forEach(i -> i.addCategories(this));
        }
        this.articleses = articles;
    }

    public ArticleCategory articleses(Set<Article> articles) {
        this.setArticleses(articles);
        return this;
    }

    public ArticleCategory addArticles(Article article) {
        this.articleses.add(article);
        article.getCategorieses().add(this);
        return this;
    }

    public ArticleCategory removeArticles(Article article) {
        this.articleses.remove(article);
        article.getCategorieses().remove(this);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ArticleCategory)) {
            return false;
        }
        return getId() != null && getId().equals(((ArticleCategory) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ArticleCategory{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", slug='" + getSlug() + "'" +
            ", description='" + getDescription() + "'" +
            ", count=" + getCount() +
            "}";
    }
}
