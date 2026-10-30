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
 * Ngành nghề (bảng category)
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

    /**
     * Sửa tay: mã ngành VSIC 2025 (QĐ 36/2025/QĐ-TTg): chữ cái A–V ở cấp 1, 2–5 chữ số ở cấp 2–5. Thay wp_term_id.
     */
    @NotNull
    @Size(max = 10)
    @Column(name = "code", length = 10, nullable = false, unique = true)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Keyword)
    private String code;

    /**
     * Sửa tay: cấp trong hệ thống ngành (1–5), suy ra từ mã khi lưu.
     */
    @Column(name = "level")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Integer)
    private Integer level;

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

    @Size(max = 255)
    @Column(name = "slug", length = 255)
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

    @Column(name = "listing_count")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Integer)
    private Integer listingCount;

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
    @JsonIgnoreProperties(value = { "parent", "listings" }, allowSetters = true)
    // Sửa tay: không ghi quan hệ vào Elasticsearch (tránh lazy-load khi reindex); giai đoạn 3 index danh sách id riêng
    @org.springframework.data.annotation.Transient
    private Category parent;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "categories")
    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "categories", "locations" }, allowSetters = true)
    private Set<Listing> listings = new HashSet<>();

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

    public String getCode() {
        return this.code;
    }

    public Category code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getLevel() {
        return this.level;
    }

    public Category level(Integer level) {
        this.setLevel(level);
        return this;
    }

    public void setLevel(Integer level) {
        this.level = level;
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

    public Integer getListingCount() {
        return this.listingCount;
    }

    public Category listingCount(Integer listingCount) {
        this.setListingCount(listingCount);
        return this;
    }

    public void setListingCount(Integer listingCount) {
        this.listingCount = listingCount;
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

    public Set<Listing> getListings() {
        return this.listings;
    }

    public void setListings(Set<Listing> listings) {
        if (this.listings != null) {
            this.listings.forEach(i -> i.removeCategory(this));
        }
        if (listings != null) {
            listings.forEach(i -> i.addCategory(this));
        }
        this.listings = listings;
    }

    public Category listings(Set<Listing> listings) {
        this.setListings(listings);
        return this;
    }

    public Category addListing(Listing listing) {
        this.listings.add(listing);
        listing.getCategories().add(this);
        return this;
    }

    public Category removeListing(Listing listing) {
        this.listings.remove(listing);
        listing.getCategories().remove(this);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    /**
     * Sửa tay: cấp của mã ngành VSIC 2025: một chữ cái (A–V) là cấp 1, còn lại bằng số chữ số (01 → 2, 011 → 3,
     * 0111 → 4, 01110 → 5). Mã trống hoặc sai dạng trả null.
     */
    public static Integer levelOf(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        if (trimmed.matches("[A-Za-z]")) {
            return 1;
        }
        return trimmed.matches("\\d{2,5}") ? trimmed.length() : null;
    }

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
            ", code='" + getCode() + "'" +
            ", level=" + getLevel() +
            ", name='" + getName() + "'" +
            ", slug='" + getSlug() + "'" +
            ", description='" + getDescription() + "'" +
            ", listingCount=" + getListingCount() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
