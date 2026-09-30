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
 * Trang tĩnh (wp_posts.post_type = 'page')
 */
@Entity
@Table(name = "static_page")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "staticpage")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class StaticPage implements Serializable {

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

    @Size(max = 255)
    @Column(name = "template", length = 255)
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
    private String template;

    @Column(name = "menu_order")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Integer)
    private Integer menuOrder;

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

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "parent")
    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "childrens", "author", "parent" }, allowSetters = true)
    private Set<StaticPage> childrens = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "childrens", "author", "parent" }, allowSetters = true)
    private StaticPage parent;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public StaticPage id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public StaticPage title(String title) {
        this.setTitle(title);
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return this.slug;
    }

    public StaticPage slug(String slug) {
        this.setSlug(slug);
        return this;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getContent() {
        return this.content;
    }

    public StaticPage content(String content) {
        this.setContent(content);
        return this;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return this.status;
    }

    public StaticPage status(String status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTemplate() {
        return this.template;
    }

    public StaticPage template(String template) {
        this.setTemplate(template);
        return this;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Integer getMenuOrder() {
        return this.menuOrder;
    }

    public StaticPage menuOrder(Integer menuOrder) {
        this.setMenuOrder(menuOrder);
        return this;
    }

    public void setMenuOrder(Integer menuOrder) {
        this.menuOrder = menuOrder;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public StaticPage createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public StaticPage updatedAt(Instant updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<StaticPage> getChildrens() {
        return this.childrens;
    }

    public void setChildrens(Set<StaticPage> staticPages) {
        if (this.childrens != null) {
            this.childrens.forEach(i -> i.setParent(null));
        }
        if (staticPages != null) {
            staticPages.forEach(i -> i.setParent(this));
        }
        this.childrens = staticPages;
    }

    public StaticPage childrens(Set<StaticPage> staticPages) {
        this.setChildrens(staticPages);
        return this;
    }

    public StaticPage addChildren(StaticPage staticPage) {
        this.childrens.add(staticPage);
        staticPage.setParent(this);
        return this;
    }

    public StaticPage removeChildren(StaticPage staticPage) {
        this.childrens.remove(staticPage);
        staticPage.setParent(null);
        return this;
    }

    public User getAuthor() {
        return this.author;
    }

    public void setAuthor(User user) {
        this.author = user;
    }

    public StaticPage author(User user) {
        this.setAuthor(user);
        return this;
    }

    public StaticPage getParent() {
        return this.parent;
    }

    public void setParent(StaticPage staticPage) {
        this.parent = staticPage;
    }

    public StaticPage parent(StaticPage staticPage) {
        this.setParent(staticPage);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StaticPage)) {
            return false;
        }
        return getId() != null && getId().equals(((StaticPage) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "StaticPage{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", slug='" + getSlug() + "'" +
            ", content='" + getContent() + "'" +
            ", status='" + getStatus() + "'" +
            ", template='" + getTemplate() + "'" +
            ", menuOrder=" + getMenuOrder() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
