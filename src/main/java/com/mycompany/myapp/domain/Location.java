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
 * Địa phương (taxonomy pointfinderlocations + province/district/ward_code)
 */
@Entity
@Table(name = "location")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "location")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Location implements Serializable {

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

    @Size(max = 50)
    @Column(name = "type", length = 50)
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
    private String type;

    @Size(max = 10)
    @Column(name = "province_code", length = 10)
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
    private String provinceCode;

    @Size(max = 10)
    @Column(name = "district_code", length = 10)
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
    private String districtCode;

    @Size(max = 10)
    @Column(name = "ward_code", length = 10)
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
    private String wardCode;

    @Min(value = 0)
    @Column(name = "count")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Integer)
    private Integer count;

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
    private Set<Location> childrens = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "childrens", "parent", "listingses" }, allowSetters = true)
    private Location parent;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "locationses")
    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "imageses", "gallerieses", "author", "categorieses", "locationses" }, allowSetters = true)
    private Set<Listing> listingses = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Location id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public Location name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return this.slug;
    }

    public Location slug(String slug) {
        this.setSlug(slug);
        return this;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getType() {
        return this.type;
    }

    public Location type(String type) {
        this.setType(type);
        return this;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getProvinceCode() {
        return this.provinceCode;
    }

    public Location provinceCode(String provinceCode) {
        this.setProvinceCode(provinceCode);
        return this;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public String getDistrictCode() {
        return this.districtCode;
    }

    public Location districtCode(String districtCode) {
        this.setDistrictCode(districtCode);
        return this;
    }

    public void setDistrictCode(String districtCode) {
        this.districtCode = districtCode;
    }

    public String getWardCode() {
        return this.wardCode;
    }

    public Location wardCode(String wardCode) {
        this.setWardCode(wardCode);
        return this;
    }

    public void setWardCode(String wardCode) {
        this.wardCode = wardCode;
    }

    public Integer getCount() {
        return this.count;
    }

    public Location count(Integer count) {
        this.setCount(count);
        return this;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Integer getDisplayOrder() {
        return this.displayOrder;
    }

    public Location displayOrder(Integer displayOrder) {
        this.setDisplayOrder(displayOrder);
        return this;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Location createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public Location updatedAt(Instant updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<Location> getChildrens() {
        return this.childrens;
    }

    public void setChildrens(Set<Location> locations) {
        if (this.childrens != null) {
            this.childrens.forEach(i -> i.setParent(null));
        }
        if (locations != null) {
            locations.forEach(i -> i.setParent(this));
        }
        this.childrens = locations;
    }

    public Location childrens(Set<Location> locations) {
        this.setChildrens(locations);
        return this;
    }

    public Location addChildren(Location location) {
        this.childrens.add(location);
        location.setParent(this);
        return this;
    }

    public Location removeChildren(Location location) {
        this.childrens.remove(location);
        location.setParent(null);
        return this;
    }

    public Location getParent() {
        return this.parent;
    }

    public void setParent(Location location) {
        this.parent = location;
    }

    public Location parent(Location location) {
        this.setParent(location);
        return this;
    }

    public Set<Listing> getListingses() {
        return this.listingses;
    }

    public void setListingses(Set<Listing> listings) {
        if (this.listingses != null) {
            this.listingses.forEach(i -> i.removeLocations(this));
        }
        if (listings != null) {
            listings.forEach(i -> i.addLocations(this));
        }
        this.listingses = listings;
    }

    public Location listingses(Set<Listing> listings) {
        this.setListingses(listings);
        return this;
    }

    public Location addListings(Listing listing) {
        this.listingses.add(listing);
        listing.getLocationses().add(this);
        return this;
    }

    public Location removeListings(Listing listing) {
        this.listingses.remove(listing);
        listing.getLocationses().remove(this);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Location)) {
            return false;
        }
        return getId() != null && getId().equals(((Location) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Location{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", slug='" + getSlug() + "'" +
            ", type='" + getType() + "'" +
            ", provinceCode='" + getProvinceCode() + "'" +
            ", districtCode='" + getDistrictCode() + "'" +
            ", wardCode='" + getWardCode() + "'" +
            ", count=" + getCount() +
            ", displayOrder=" + getDisplayOrder() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
