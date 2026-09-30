package com.mycompany.myapp.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.Listing} entity.
 */
@Schema(description = "Doanh nghiệp (wp_posts.post_type = 'listing' + wp_postmeta)")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ListingDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 500)
    private String title;

    @NotNull
    @Size(max = 500)
    private String slug;

    @Lob
    private String content;

    @Size(max = 20)
    private String status;

    @Size(max = 255)
    private String email;

    @Size(max = 50)
    private String telephone;

    @Size(max = 50)
    private String mobile;

    @Size(max = 255)
    private String website;

    @Size(max = 50)
    private String fax;

    @Size(max = 50)
    private String taxCode;

    @Size(max = 500)
    private String nameAlias;

    @Size(max = 500)
    private String nameEn;

    @Size(max = 500)
    private String representative;

    @Size(max = 500)
    private String mainIndustry;

    @Size(max = 500)
    private String managedBy;

    @Size(max = 100)
    private String businessType;

    @Size(max = 50)
    private String statusYp;

    private Instant foundedDate;

    private Instant licenseModifiedDate;

    @Lob
    private String address;

    @Lob
    private String addressAlternative;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private Long apiId;

    private Instant createdAt;

    private Instant updatedAt;

    private UserDTO author;

    private Set<CategoryDTO> categorieses = new HashSet<>();

    private Set<LocationDTO> locationses = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getNameAlias() {
        return nameAlias;
    }

    public void setNameAlias(String nameAlias) {
        this.nameAlias = nameAlias;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public String getRepresentative() {
        return representative;
    }

    public void setRepresentative(String representative) {
        this.representative = representative;
    }

    public String getMainIndustry() {
        return mainIndustry;
    }

    public void setMainIndustry(String mainIndustry) {
        this.mainIndustry = mainIndustry;
    }

    public String getManagedBy() {
        return managedBy;
    }

    public void setManagedBy(String managedBy) {
        this.managedBy = managedBy;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getStatusYp() {
        return statusYp;
    }

    public void setStatusYp(String statusYp) {
        this.statusYp = statusYp;
    }

    public Instant getFoundedDate() {
        return foundedDate;
    }

    public void setFoundedDate(Instant foundedDate) {
        this.foundedDate = foundedDate;
    }

    public Instant getLicenseModifiedDate() {
        return licenseModifiedDate;
    }

    public void setLicenseModifiedDate(Instant licenseModifiedDate) {
        this.licenseModifiedDate = licenseModifiedDate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddressAlternative() {
        return addressAlternative;
    }

    public void setAddressAlternative(String addressAlternative) {
        this.addressAlternative = addressAlternative;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public Long getApiId() {
        return apiId;
    }

    public void setApiId(Long apiId) {
        this.apiId = apiId;
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

    public UserDTO getAuthor() {
        return author;
    }

    public void setAuthor(UserDTO author) {
        this.author = author;
    }

    public Set<CategoryDTO> getCategorieses() {
        return categorieses;
    }

    public void setCategorieses(Set<CategoryDTO> categorieses) {
        this.categorieses = categorieses;
    }

    public Set<LocationDTO> getLocationses() {
        return locationses;
    }

    public void setLocationses(Set<LocationDTO> locationses) {
        this.locationses = locationses;
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
            ", author=" + getAuthor() +
            ", categorieses=" + getCategorieses() +
            ", locationses=" + getLocationses() +
            "}";
    }
}
