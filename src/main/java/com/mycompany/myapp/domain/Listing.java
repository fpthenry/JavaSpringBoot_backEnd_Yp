package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Doanh nghiệp (wp_posts.post_type = 'listing' + wp_postmeta)
 */
@Entity
@Table(name = "listing")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "listing")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Listing implements Serializable {

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
    @Column(name = "email", length = 255)
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
    private String email;

    @Size(max = 50)
    @Column(name = "telephone", length = 50)
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
    private String telephone;

    @Size(max = 50)
    @Column(name = "mobile", length = 50)
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
    private String mobile;

    @Size(max = 255)
    @Column(name = "website", length = 255)
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
    private String website;

    @Size(max = 50)
    @Column(name = "fax", length = 50)
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
    private String fax;

    @Size(max = 50)
    @Column(name = "tax_code", length = 50)
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
    private String taxCode;

    @Size(max = 500)
    @Column(name = "name_alias", length = 500)
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
    private String nameAlias;

    @Size(max = 500)
    @Column(name = "name_en", length = 500)
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
    private String nameEn;

    @Size(max = 500)
    @Column(name = "representative", length = 500)
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
    private String representative;

    @Size(max = 500)
    @Column(name = "main_industry", length = 500)
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
    private String mainIndustry;

    @Size(max = 500)
    @Column(name = "managed_by", length = 500)
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
    private String managedBy;

    @Size(max = 100)
    @Column(name = "business_type", length = 100)
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
    private String businessType;

    @Size(max = 50)
    @Column(name = "status_yp", length = 50)
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
    private String statusYp;

    @Column(name = "founded_date")
    @org.springframework.data.elasticsearch.annotations.Field(
        type = org.springframework.data.elasticsearch.annotations.FieldType.Date,
        format = org.springframework.data.elasticsearch.annotations.DateFormat.date_time
    )
    private Instant foundedDate;

    @Column(name = "license_modified_date")
    @org.springframework.data.elasticsearch.annotations.Field(
        type = org.springframework.data.elasticsearch.annotations.FieldType.Date,
        format = org.springframework.data.elasticsearch.annotations.DateFormat.date_time
    )
    private Instant licenseModifiedDate;

    @Lob
    @Column(name = "address")
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
    private String address;

    @Lob
    @Column(name = "address_alternative")
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
    private String addressAlternative;

    @Column(name = "latitude", precision = 21, scale = 2)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 21, scale = 2)
    private BigDecimal longitude;

    @Column(name = "api_id")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Long)
    private Long apiId;

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

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "listing")
    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "listing", "gallery" }, allowSetters = true)
    private Set<ListingImage> imageses = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "listing")
    @org.springframework.data.annotation.Transient
    @JsonIgnoreProperties(value = { "imageses", "author", "listing" }, allowSetters = true)
    private Set<Gallery> gallerieses = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    private User author;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rel_listing__categories",
        joinColumns = @JoinColumn(name = "listing_id"),
        inverseJoinColumns = @JoinColumn(name = "categories_id")
    )
    @JsonIgnoreProperties(value = { "childrens", "parent", "listingses" }, allowSetters = true)
    private Set<Category> categorieses = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rel_listing__locations",
        joinColumns = @JoinColumn(name = "listing_id"),
        inverseJoinColumns = @JoinColumn(name = "locations_id")
    )
    @JsonIgnoreProperties(value = { "childrens", "parent", "listingses" }, allowSetters = true)
    private Set<Location> locationses = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Listing id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public Listing title(String title) {
        this.setTitle(title);
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return this.slug;
    }

    public Listing slug(String slug) {
        this.setSlug(slug);
        return this;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getContent() {
        return this.content;
    }

    public Listing content(String content) {
        this.setContent(content);
        return this;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return this.status;
    }

    public Listing status(String status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEmail() {
        return this.email;
    }

    public Listing email(String email) {
        this.setEmail(email);
        return this;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return this.telephone;
    }

    public Listing telephone(String telephone) {
        this.setTelephone(telephone);
        return this;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getMobile() {
        return this.mobile;
    }

    public Listing mobile(String mobile) {
        this.setMobile(mobile);
        return this;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getWebsite() {
        return this.website;
    }

    public Listing website(String website) {
        this.setWebsite(website);
        return this;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getFax() {
        return this.fax;
    }

    public Listing fax(String fax) {
        this.setFax(fax);
        return this;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getTaxCode() {
        return this.taxCode;
    }

    public Listing taxCode(String taxCode) {
        this.setTaxCode(taxCode);
        return this;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getNameAlias() {
        return this.nameAlias;
    }

    public Listing nameAlias(String nameAlias) {
        this.setNameAlias(nameAlias);
        return this;
    }

    public void setNameAlias(String nameAlias) {
        this.nameAlias = nameAlias;
    }

    public String getNameEn() {
        return this.nameEn;
    }

    public Listing nameEn(String nameEn) {
        this.setNameEn(nameEn);
        return this;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public String getRepresentative() {
        return this.representative;
    }

    public Listing representative(String representative) {
        this.setRepresentative(representative);
        return this;
    }

    public void setRepresentative(String representative) {
        this.representative = representative;
    }

    public String getMainIndustry() {
        return this.mainIndustry;
    }

    public Listing mainIndustry(String mainIndustry) {
        this.setMainIndustry(mainIndustry);
        return this;
    }

    public void setMainIndustry(String mainIndustry) {
        this.mainIndustry = mainIndustry;
    }

    public String getManagedBy() {
        return this.managedBy;
    }

    public Listing managedBy(String managedBy) {
        this.setManagedBy(managedBy);
        return this;
    }

    public void setManagedBy(String managedBy) {
        this.managedBy = managedBy;
    }

    public String getBusinessType() {
        return this.businessType;
    }

    public Listing businessType(String businessType) {
        this.setBusinessType(businessType);
        return this;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getStatusYp() {
        return this.statusYp;
    }

    public Listing statusYp(String statusYp) {
        this.setStatusYp(statusYp);
        return this;
    }

    public void setStatusYp(String statusYp) {
        this.statusYp = statusYp;
    }

    public Instant getFoundedDate() {
        return this.foundedDate;
    }

    public Listing foundedDate(Instant foundedDate) {
        this.setFoundedDate(foundedDate);
        return this;
    }

    public void setFoundedDate(Instant foundedDate) {
        this.foundedDate = foundedDate;
    }

    public Instant getLicenseModifiedDate() {
        return this.licenseModifiedDate;
    }

    public Listing licenseModifiedDate(Instant licenseModifiedDate) {
        this.setLicenseModifiedDate(licenseModifiedDate);
        return this;
    }

    public void setLicenseModifiedDate(Instant licenseModifiedDate) {
        this.licenseModifiedDate = licenseModifiedDate;
    }

    public String getAddress() {
        return this.address;
    }

    public Listing address(String address) {
        this.setAddress(address);
        return this;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddressAlternative() {
        return this.addressAlternative;
    }

    public Listing addressAlternative(String addressAlternative) {
        this.setAddressAlternative(addressAlternative);
        return this;
    }

    public void setAddressAlternative(String addressAlternative) {
        this.addressAlternative = addressAlternative;
    }

    public BigDecimal getLatitude() {
        return this.latitude;
    }

    public Listing latitude(BigDecimal latitude) {
        this.setLatitude(latitude);
        return this;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return this.longitude;
    }

    public Listing longitude(BigDecimal longitude) {
        this.setLongitude(longitude);
        return this;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public Long getApiId() {
        return this.apiId;
    }

    public Listing apiId(Long apiId) {
        this.setApiId(apiId);
        return this;
    }

    public void setApiId(Long apiId) {
        this.apiId = apiId;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Listing createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public Listing updatedAt(Instant updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<ListingImage> getImageses() {
        return this.imageses;
    }

    public void setImageses(Set<ListingImage> listingImages) {
        if (this.imageses != null) {
            this.imageses.forEach(i -> i.setListing(null));
        }
        if (listingImages != null) {
            listingImages.forEach(i -> i.setListing(this));
        }
        this.imageses = listingImages;
    }

    public Listing imageses(Set<ListingImage> listingImages) {
        this.setImageses(listingImages);
        return this;
    }

    public Listing addImages(ListingImage listingImage) {
        this.imageses.add(listingImage);
        listingImage.setListing(this);
        return this;
    }

    public Listing removeImages(ListingImage listingImage) {
        this.imageses.remove(listingImage);
        listingImage.setListing(null);
        return this;
    }

    public Set<Gallery> getGallerieses() {
        return this.gallerieses;
    }

    public void setGallerieses(Set<Gallery> galleries) {
        if (this.gallerieses != null) {
            this.gallerieses.forEach(i -> i.setListing(null));
        }
        if (galleries != null) {
            galleries.forEach(i -> i.setListing(this));
        }
        this.gallerieses = galleries;
    }

    public Listing gallerieses(Set<Gallery> galleries) {
        this.setGallerieses(galleries);
        return this;
    }

    public Listing addGalleries(Gallery gallery) {
        this.gallerieses.add(gallery);
        gallery.setListing(this);
        return this;
    }

    public Listing removeGalleries(Gallery gallery) {
        this.gallerieses.remove(gallery);
        gallery.setListing(null);
        return this;
    }

    public User getAuthor() {
        return this.author;
    }

    public void setAuthor(User user) {
        this.author = user;
    }

    public Listing author(User user) {
        this.setAuthor(user);
        return this;
    }

    public Set<Category> getCategorieses() {
        return this.categorieses;
    }

    public void setCategorieses(Set<Category> categories) {
        this.categorieses = categories;
    }

    public Listing categorieses(Set<Category> categories) {
        this.setCategorieses(categories);
        return this;
    }

    public Listing addCategories(Category category) {
        this.categorieses.add(category);
        return this;
    }

    public Listing removeCategories(Category category) {
        this.categorieses.remove(category);
        return this;
    }

    public Set<Location> getLocationses() {
        return this.locationses;
    }

    public void setLocationses(Set<Location> locations) {
        this.locationses = locations;
    }

    public Listing locationses(Set<Location> locations) {
        this.setLocationses(locations);
        return this;
    }

    public Listing addLocations(Location location) {
        this.locationses.add(location);
        return this;
    }

    public Listing removeLocations(Location location) {
        this.locationses.remove(location);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Listing)) {
            return false;
        }
        return getId() != null && getId().equals(((Listing) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Listing{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", slug='" + getSlug() + "'" +
            ", content='" + getContent() + "'" +
            ", status='" + getStatus() + "'" +
            ", email='" + getEmail() + "'" +
            ", telephone='" + getTelephone() + "'" +
            ", mobile='" + getMobile() + "'" +
            ", website='" + getWebsite() + "'" +
            ", fax='" + getFax() + "'" +
            ", taxCode='" + getTaxCode() + "'" +
            ", nameAlias='" + getNameAlias() + "'" +
            ", nameEn='" + getNameEn() + "'" +
            ", representative='" + getRepresentative() + "'" +
            ", mainIndustry='" + getMainIndustry() + "'" +
            ", managedBy='" + getManagedBy() + "'" +
            ", businessType='" + getBusinessType() + "'" +
            ", statusYp='" + getStatusYp() + "'" +
            ", foundedDate='" + getFoundedDate() + "'" +
            ", licenseModifiedDate='" + getLicenseModifiedDate() + "'" +
            ", address='" + getAddress() + "'" +
            ", addressAlternative='" + getAddressAlternative() + "'" +
            ", latitude=" + getLatitude() +
            ", longitude=" + getLongitude() +
            ", apiId=" + getApiId() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
