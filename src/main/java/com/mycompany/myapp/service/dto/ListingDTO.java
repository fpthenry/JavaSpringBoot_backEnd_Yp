package com.mycompany.myapp.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.Listing} entity.
 */
@Schema(description = "Doanh nghiệp (bảng listing)")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ListingDTO implements Serializable {

    private Long id;

    @NotNull
    @Schema(description = "ID bài viết WordPress (wp_posts.ID)", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long wpId;

    @Size(max = 100)
    @Schema(description = "ID từ API nguồn")
    private String apiId;

    @NotNull
    @Size(max = 500)
    private String name;

    @Size(max = 500)
    private String nameEn;

    @Size(max = 500)
    private String nameAlias;

    @Size(max = 500)
    @Schema(description = "Không unique: dữ liệu gốc có slug NULL và trùng")
    private String slug;

    @Lob
    private String description;

    @Size(max = 100)
    private String phone;

    @Size(max = 100)
    private String mobile;

    @Size(max = 255)
    private String email;

    @Size(max = 500)
    private String website;

    @Lob
    private String address;

    @Schema(description = "JSON gốc: province, ward, street, province_code, ward_code, coordinate dạng lat:long (giữ nguyên dạng text)")
    @Lob
    private String locationJson;

    @Size(max = 100)
    private String taxCode;

    @Size(max = 255)
    private String representative;

    @Size(max = 100)
    private String capital;

    @Size(max = 50)
    @Schema(description = "Dạng text gốc (vd 2024-01-11), không ép kiểu ngày")
    private String foundedYear;

    @Size(max = 100)
    private String businessType;

    @Size(max = 50)
    private String businessStatus;

    @Size(max = 100)
    private String industryCode;

    @Size(max = 255)
    private String managedBy;

    @Size(max = 500)
    private String thumbnail;

    @Lob
    private String images;

    private Integer viewCount;

    private Boolean isFeatured;

    @Size(max = 50)
    @Schema(description = "publish, draft")
    private String status;

    private Instant publishedAt;

    private Instant modifiedAt;

    private Instant createdAt;

    private Instant updatedAt;

    private Boolean esIndexed;

    private Set<CategoryDTO> categories = new HashSet<>();

    private Set<LocationDTO> locations = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWpId() {
        return wpId;
    }

    public void setWpId(Long wpId) {
        this.wpId = wpId;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public String getNameAlias() {
        return nameAlias;
    }

    public void setNameAlias(String nameAlias) {
        this.nameAlias = nameAlias;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLocationJson() {
        return locationJson;
    }

    public void setLocationJson(String locationJson) {
        this.locationJson = locationJson;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getRepresentative() {
        return representative;
    }

    public void setRepresentative(String representative) {
        this.representative = representative;
    }

    public String getCapital() {
        return capital;
    }

    public void setCapital(String capital) {
        this.capital = capital;
    }

    public String getFoundedYear() {
        return foundedYear;
    }

    public void setFoundedYear(String foundedYear) {
        this.foundedYear = foundedYear;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getBusinessStatus() {
        return businessStatus;
    }

    public void setBusinessStatus(String businessStatus) {
        this.businessStatus = businessStatus;
    }

    public String getIndustryCode() {
        return industryCode;
    }

    public void setIndustryCode(String industryCode) {
        this.industryCode = industryCode;
    }

    public String getManagedBy() {
        return managedBy;
    }

    public void setManagedBy(String managedBy) {
        this.managedBy = managedBy;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Boolean getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Instant modifiedAt) {
        this.modifiedAt = modifiedAt;
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

    public Boolean getEsIndexed() {
        return esIndexed;
    }

    public void setEsIndexed(Boolean esIndexed) {
        this.esIndexed = esIndexed;
    }

    public Set<CategoryDTO> getCategories() {
        return categories;
    }

    public void setCategories(Set<CategoryDTO> categories) {
        this.categories = categories;
    }

    public Set<LocationDTO> getLocations() {
        return locations;
    }

    public void setLocations(Set<LocationDTO> locations) {
        this.locations = locations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ListingDTO)) {
            return false;
        }

        ListingDTO listingDTO = (ListingDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, listingDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ListingDTO{" +
            "id=" + getId() +
            ", wpId=" + getWpId() +
            ", apiId='" + getApiId() + "'" +
            ", name='" + getName() + "'" +
            ", nameEn='" + getNameEn() + "'" +
            ", nameAlias='" + getNameAlias() + "'" +
            ", slug='" + getSlug() + "'" +
            ", description='" + getDescription() + "'" +
            ", phone='" + getPhone() + "'" +
            ", mobile='" + getMobile() + "'" +
            ", email='" + getEmail() + "'" +
            ", website='" + getWebsite() + "'" +
            ", address='" + getAddress() + "'" +
            ", locationJson='" + getLocationJson() + "'" +
            ", taxCode='" + getTaxCode() + "'" +
            ", representative='" + getRepresentative() + "'" +
            ", capital='" + getCapital() + "'" +
            ", foundedYear='" + getFoundedYear() + "'" +
            ", businessType='" + getBusinessType() + "'" +
            ", businessStatus='" + getBusinessStatus() + "'" +
            ", industryCode='" + getIndustryCode() + "'" +
            ", managedBy='" + getManagedBy() + "'" +
            ", thumbnail='" + getThumbnail() + "'" +
            ", images='" + getImages() + "'" +
            ", viewCount=" + getViewCount() +
            ", isFeatured='" + getIsFeatured() + "'" +
            ", status='" + getStatus() + "'" +
            ", publishedAt='" + getPublishedAt() + "'" +
            ", modifiedAt='" + getModifiedAt() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            ", esIndexed='" + getEsIndexed() + "'" +
            ", categories=" + getCategories() +
            ", locations=" + getLocations() +
            "}";
    }
}
