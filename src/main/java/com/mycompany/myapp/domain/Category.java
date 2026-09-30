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
 * Ngành nghề (taxonomy pointfinderltypes + wp_termmeta.industry_code)
 */
@Entity
@Table(name = "category")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "category")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Category implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Long)
    private Long id;

    @NotNull
    @Size(max = 500)
    @Column(name = "name", length = 500, nullable = false)
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

    @Size(max = 50)
    @Column(name = "industry_code", length = 50)
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
    private String industryCode;

    @Min(value = 0)
    @Column(name = "display_order")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Integer)
    private Integer displayOrder;

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
    @JsonIgnoreProperties(value = { "childrens", "parent", "listingses" }, allowSetters = true)
    private Set<Category> childrens = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "childrens", "parent", "listingses" }, allowSetters = true)
    private Category parent;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "categorieses")
    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "imageses", "gallerieses", "author", "categorieses", "locationses" }, allowSetters = true)
    private Set<Listing> listingses = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Category id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public Category name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return this.slug;
    }

    public Category slug(String slug) {
        this.setSlug(slug);
        return this;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return this.description;
    }

    public Category description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCount() {
        return this.count;
    }

    public Category count(Integer count) {
        this.setCount(count);
        return this;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getIndustryCode() {
        return this.industryCode;
    }

    public Category industryCode(String industryCode) {
        this.setIndustryCode(industryCode);
        return this;
    }

    public void setIndustryCode(String industryCode) {
        this.industryCode = industryCode;
    }

    public Integer getDisplayOrder() {
        return this.displayOrder;
    }

    public Category displayOrder(Integer displayOrder) {
        this.setDisplayOrder(displayOrder);
        return this;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Category createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public Category updatedAt(Instant updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<Category> getChildrens() {
        return this.childrens;
    }

    public void setChildrens(Set<Category> categories) {
        if (this.childrens != null) {
            this.childrens.forEach(i -> i.setParent(null));
        }
        if (categories != null) {
            categories.forEach(i -> i.setParent(this));
        }
        this.childrens = categories;
    }

    public Category childrens(Set<Category> categories) {
        this.setChildrens(categories);
        return this;
    }

    public Category addChildren(Category category) {
        this.childrens.add(category);
        category.setParent(this);
        return this;
    }

    public Category removeChildren(Category category) {
        this.childrens.remove(category);
        category.setParent(null);
        return this;
    }

    public Category getParent() {
        return this.parent;
    }

    public void setParent(Category category) {
        this.parent = category;
    }

    public Category parent(Category category) {
        this.setParent(category);
        return this;
    }

    public Set<Listing> getListingses() {
        return this.listingses;
    }

    public void setListingses(Set<Listing> listings) {
        if (this.listingses != null) {
            this.listingses.forEach(i -> i.removeCategories(this));
        }
        if (listings != null) {
            listings.forEach(i -> i.addCategories(this));
        }
        this.listingses = listings;
    }

    public Category listingses(Set<Listing> listings) {
        this.setListingses(listings);
        return this;
    }

    public Category addListings(Listing listing) {
        this.listingses.add(listing);
        listing.getCategorieses().add(this);
        return this;
    }

    public Category removeListings(Listing listing) {
        this.listingses.remove(listing);
        listing.getCategorieses().remove(this);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Category)) {
            return false;
        }
        return getId() != null && getId().equals(((Category) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Category{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", slug='" + getSlug() + "'" +
            ", description='" + getDescription() + "'" +
            ", count=" + getCount() +
            ", industryCode='" + getIndustryCode() + "'" +
            ", displayOrder=" + getDisplayOrder() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
