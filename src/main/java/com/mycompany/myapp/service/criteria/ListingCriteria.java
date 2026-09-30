package com.mycompany.myapp.service.criteria;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.mycompany.myapp.domain.Listing} entity. This class is used
 * in {@link com.mycompany.myapp.web.rest.ListingResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /listings?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ListingCriteria implements Serializable, Criteria {

    @Serial
    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter title;

    private StringFilter slug;

    private StringFilter status;

    private StringFilter email;

    private StringFilter telephone;

    private StringFilter mobile;

    private StringFilter website;

    private StringFilter fax;

    private StringFilter taxCode;

    private StringFilter nameAlias;

    private StringFilter nameEn;

    private StringFilter representative;

    private StringFilter mainIndustry;

    private StringFilter managedBy;

    private StringFilter businessType;

    private StringFilter statusYp;

    private InstantFilter foundedDate;

    private InstantFilter licenseModifiedDate;

    private BigDecimalFilter latitude;

    private BigDecimalFilter longitude;

    private LongFilter apiId;

    private InstantFilter createdAt;

    private InstantFilter updatedAt;

    private LongFilter imagesId;

    private LongFilter galleriesId;

    private LongFilter authorId;

    private LongFilter categoriesId;

    private LongFilter locationsId;

    private Boolean distinct;

    public ListingCriteria() {}

    public ListingCriteria(ListingCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.title = other.optionalTitle().map(StringFilter::copy).orElse(null);
        this.slug = other.optionalSlug().map(StringFilter::copy).orElse(null);
        this.status = other.optionalStatus().map(StringFilter::copy).orElse(null);
        this.email = other.optionalEmail().map(StringFilter::copy).orElse(null);
        this.telephone = other.optionalTelephone().map(StringFilter::copy).orElse(null);
        this.mobile = other.optionalMobile().map(StringFilter::copy).orElse(null);
        this.website = other.optionalWebsite().map(StringFilter::copy).orElse(null);
        this.fax = other.optionalFax().map(StringFilter::copy).orElse(null);
        this.taxCode = other.optionalTaxCode().map(StringFilter::copy).orElse(null);
        this.nameAlias = other.optionalNameAlias().map(StringFilter::copy).orElse(null);
        this.nameEn = other.optionalNameEn().map(StringFilter::copy).orElse(null);
        this.representative = other.optionalRepresentative().map(StringFilter::copy).orElse(null);
        this.mainIndustry = other.optionalMainIndustry().map(StringFilter::copy).orElse(null);
        this.managedBy = other.optionalManagedBy().map(StringFilter::copy).orElse(null);
        this.businessType = other.optionalBusinessType().map(StringFilter::copy).orElse(null);
        this.statusYp = other.optionalStatusYp().map(StringFilter::copy).orElse(null);
        this.foundedDate = other.optionalFoundedDate().map(InstantFilter::copy).orElse(null);
        this.licenseModifiedDate = other.optionalLicenseModifiedDate().map(InstantFilter::copy).orElse(null);
        this.latitude = other.optionalLatitude().map(BigDecimalFilter::copy).orElse(null);
        this.longitude = other.optionalLongitude().map(BigDecimalFilter::copy).orElse(null);
        this.apiId = other.optionalApiId().map(LongFilter::copy).orElse(null);
        this.createdAt = other.optionalCreatedAt().map(InstantFilter::copy).orElse(null);
        this.updatedAt = other.optionalUpdatedAt().map(InstantFilter::copy).orElse(null);
        this.imagesId = other.optionalImagesId().map(LongFilter::copy).orElse(null);
        this.galleriesId = other.optionalGalleriesId().map(LongFilter::copy).orElse(null);
        this.authorId = other.optionalAuthorId().map(LongFilter::copy).orElse(null);
        this.categoriesId = other.optionalCategoriesId().map(LongFilter::copy).orElse(null);
        this.locationsId = other.optionalLocationsId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public ListingCriteria copy() {
        return new ListingCriteria(this);
    }

    public LongFilter getId() {
        return id;
    }

    public Optional<LongFilter> optionalId() {
        return Optional.ofNullable(id);
    }

    public LongFilter id() {
        if (id == null) {
            setId(new LongFilter());
        }
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public StringFilter getTitle() {
        return title;
    }

    public Optional<StringFilter> optionalTitle() {
        return Optional.ofNullable(title);
    }

    public StringFilter title() {
        if (title == null) {
            setTitle(new StringFilter());
        }
        return title;
    }

    public void setTitle(StringFilter title) {
        this.title = title;
    }

    public StringFilter getSlug() {
        return slug;
    }

    public Optional<StringFilter> optionalSlug() {
        return Optional.ofNullable(slug);
    }

    public StringFilter slug() {
        if (slug == null) {
            setSlug(new StringFilter());
        }
        return slug;
    }

    public void setSlug(StringFilter slug) {
        this.slug = slug;
    }

    public StringFilter getStatus() {
        return status;
    }

    public Optional<StringFilter> optionalStatus() {
        return Optional.ofNullable(status);
    }

    public StringFilter status() {
        if (status == null) {
            setStatus(new StringFilter());
        }
        return status;
    }

    public void setStatus(StringFilter status) {
        this.status = status;
    }

    public StringFilter getEmail() {
        return email;
    }

    public Optional<StringFilter> optionalEmail() {
        return Optional.ofNullable(email);
    }

    public StringFilter email() {
        if (email == null) {
            setEmail(new StringFilter());
        }
        return email;
    }

    public void setEmail(StringFilter email) {
        this.email = email;
    }

    public StringFilter getTelephone() {
        return telephone;
    }

    public Optional<StringFilter> optionalTelephone() {
        return Optional.ofNullable(telephone);
    }

    public StringFilter telephone() {
        if (telephone == null) {
            setTelephone(new StringFilter());
        }
        return telephone;
    }

    public void setTelephone(StringFilter telephone) {
        this.telephone = telephone;
    }

    public StringFilter getMobile() {
        return mobile;
    }

    public Optional<StringFilter> optionalMobile() {
        return Optional.ofNullable(mobile);
    }

    public StringFilter mobile() {
        if (mobile == null) {
            setMobile(new StringFilter());
        }
        return mobile;
    }

    public void setMobile(StringFilter mobile) {
        this.mobile = mobile;
    }

    public StringFilter getWebsite() {
        return website;
    }

    public Optional<StringFilter> optionalWebsite() {
        return Optional.ofNullable(website);
    }

    public StringFilter website() {
        if (website == null) {
            setWebsite(new StringFilter());
        }
        return website;
    }

    public void setWebsite(StringFilter website) {
        this.website = website;
    }

    public StringFilter getFax() {
        return fax;
    }

    public Optional<StringFilter> optionalFax() {
        return Optional.ofNullable(fax);
    }

    public StringFilter fax() {
        if (fax == null) {
            setFax(new StringFilter());
        }
        return fax;
    }

    public void setFax(StringFilter fax) {
        this.fax = fax;
    }

    public StringFilter getTaxCode() {
        return taxCode;
    }

    public Optional<StringFilter> optionalTaxCode() {
        return Optional.ofNullable(taxCode);
    }

    public StringFilter taxCode() {
        if (taxCode == null) {
            setTaxCode(new StringFilter());
        }
        return taxCode;
    }

    public void setTaxCode(StringFilter taxCode) {
        this.taxCode = taxCode;
    }

    public StringFilter getNameAlias() {
        return nameAlias;
    }

    public Optional<StringFilter> optionalNameAlias() {
        return Optional.ofNullable(nameAlias);
    }

    public StringFilter nameAlias() {
        if (nameAlias == null) {
            setNameAlias(new StringFilter());
        }
        return nameAlias;
    }

    public void setNameAlias(StringFilter nameAlias) {
        this.nameAlias = nameAlias;
    }

    public StringFilter getNameEn() {
        return nameEn;
    }

    public Optional<StringFilter> optionalNameEn() {
        return Optional.ofNullable(nameEn);
    }

    public StringFilter nameEn() {
        if (nameEn == null) {
            setNameEn(new StringFilter());
        }
        return nameEn;
    }

    public void setNameEn(StringFilter nameEn) {
        this.nameEn = nameEn;
    }

    public StringFilter getRepresentative() {
        return representative;
    }

    public Optional<StringFilter> optionalRepresentative() {
        return Optional.ofNullable(representative);
    }

    public StringFilter representative() {
        if (representative == null) {
            setRepresentative(new StringFilter());
        }
        return representative;
    }

    public void setRepresentative(StringFilter representative) {
        this.representative = representative;
    }

    public StringFilter getMainIndustry() {
        return mainIndustry;
    }

    public Optional<StringFilter> optionalMainIndustry() {
        return Optional.ofNullable(mainIndustry);
    }

    public StringFilter mainIndustry() {
        if (mainIndustry == null) {
            setMainIndustry(new StringFilter());
        }
        return mainIndustry;
    }

    public void setMainIndustry(StringFilter mainIndustry) {
        this.mainIndustry = mainIndustry;
    }

    public StringFilter getManagedBy() {
        return managedBy;
    }

    public Optional<StringFilter> optionalManagedBy() {
        return Optional.ofNullable(managedBy);
    }

    public StringFilter managedBy() {
        if (managedBy == null) {
            setManagedBy(new StringFilter());
        }
        return managedBy;
    }

    public void setManagedBy(StringFilter managedBy) {
        this.managedBy = managedBy;
    }

    public StringFilter getBusinessType() {
        return businessType;
    }

    public Optional<StringFilter> optionalBusinessType() {
        return Optional.ofNullable(businessType);
    }

    public StringFilter businessType() {
        if (businessType == null) {
            setBusinessType(new StringFilter());
        }
        return businessType;
    }

    public void setBusinessType(StringFilter businessType) {
        this.businessType = businessType;
    }

    public StringFilter getStatusYp() {
        return statusYp;
    }

    public Optional<StringFilter> optionalStatusYp() {
        return Optional.ofNullable(statusYp);
    }

    public StringFilter statusYp() {
        if (statusYp == null) {
            setStatusYp(new StringFilter());
        }
        return statusYp;
    }

    public void setStatusYp(StringFilter statusYp) {
        this.statusYp = statusYp;
    }

    public InstantFilter getFoundedDate() {
        return foundedDate;
    }

    public Optional<InstantFilter> optionalFoundedDate() {
        return Optional.ofNullable(foundedDate);
    }

    public InstantFilter foundedDate() {
        if (foundedDate == null) {
            setFoundedDate(new InstantFilter());
        }
        return foundedDate;
    }

    public void setFoundedDate(InstantFilter foundedDate) {
        this.foundedDate = foundedDate;
    }

    public InstantFilter getLicenseModifiedDate() {
        return licenseModifiedDate;
    }

    public Optional<InstantFilter> optionalLicenseModifiedDate() {
        return Optional.ofNullable(licenseModifiedDate);
    }

    public InstantFilter licenseModifiedDate() {
        if (licenseModifiedDate == null) {
            setLicenseModifiedDate(new InstantFilter());
        }
        return licenseModifiedDate;
    }

    public void setLicenseModifiedDate(InstantFilter licenseModifiedDate) {
        this.licenseModifiedDate = licenseModifiedDate;
    }

    public BigDecimalFilter getLatitude() {
        return latitude;
    }

    public Optional<BigDecimalFilter> optionalLatitude() {
        return Optional.ofNullable(latitude);
    }

    public BigDecimalFilter latitude() {
        if (latitude == null) {
            setLatitude(new BigDecimalFilter());
        }
        return latitude;
    }

    public void setLatitude(BigDecimalFilter latitude) {
        this.latitude = latitude;
    }

    public BigDecimalFilter getLongitude() {
        return longitude;
    }

    public Optional<BigDecimalFilter> optionalLongitude() {
        return Optional.ofNullable(longitude);
    }

    public BigDecimalFilter longitude() {
        if (longitude == null) {
            setLongitude(new BigDecimalFilter());
        }
        return longitude;
    }

    public void setLongitude(BigDecimalFilter longitude) {
        this.longitude = longitude;
    }

    public LongFilter getApiId() {
        return apiId;
    }

    public Optional<LongFilter> optionalApiId() {
        return Optional.ofNullable(apiId);
    }

    public LongFilter apiId() {
        if (apiId == null) {
            setApiId(new LongFilter());
        }
        return apiId;
    }

    public void setApiId(LongFilter apiId) {
        this.apiId = apiId;
    }

    public InstantFilter getCreatedAt() {
        return createdAt;
    }

    public Optional<InstantFilter> optionalCreatedAt() {
        return Optional.ofNullable(createdAt);
    }

    public InstantFilter createdAt() {
        if (createdAt == null) {
            setCreatedAt(new InstantFilter());
        }
        return createdAt;
    }

    public void setCreatedAt(InstantFilter createdAt) {
        this.createdAt = createdAt;
    }

    public InstantFilter getUpdatedAt() {
        return updatedAt;
    }

    public Optional<InstantFilter> optionalUpdatedAt() {
        return Optional.ofNullable(updatedAt);
    }

    public InstantFilter updatedAt() {
        if (updatedAt == null) {
            setUpdatedAt(new InstantFilter());
        }
        return updatedAt;
    }

    public void setUpdatedAt(InstantFilter updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LongFilter getImagesId() {
        return imagesId;
    }

    public Optional<LongFilter> optionalImagesId() {
        return Optional.ofNullable(imagesId);
    }

    public LongFilter imagesId() {
        if (imagesId == null) {
            setImagesId(new LongFilter());
        }
        return imagesId;
    }

    public void setImagesId(LongFilter imagesId) {
        this.imagesId = imagesId;
    }

    public LongFilter getGalleriesId() {
        return galleriesId;
    }

    public Optional<LongFilter> optionalGalleriesId() {
        return Optional.ofNullable(galleriesId);
    }

    public LongFilter galleriesId() {
        if (galleriesId == null) {
            setGalleriesId(new LongFilter());
        }
        return galleriesId;
    }

    public void setGalleriesId(LongFilter galleriesId) {
        this.galleriesId = galleriesId;
    }

    public LongFilter getAuthorId() {
        return authorId;
    }

    public Optional<LongFilter> optionalAuthorId() {
        return Optional.ofNullable(authorId);
    }

    public LongFilter authorId() {
        if (authorId == null) {
            setAuthorId(new LongFilter());
        }
        return authorId;
    }

    public void setAuthorId(LongFilter authorId) {
        this.authorId = authorId;
    }

    public LongFilter getCategoriesId() {
        return categoriesId;
    }

    public Optional<LongFilter> optionalCategoriesId() {
        return Optional.ofNullable(categoriesId);
    }

    public LongFilter categoriesId() {
        if (categoriesId == null) {
            setCategoriesId(new LongFilter());
        }
        return categoriesId;
    }

    public void setCategoriesId(LongFilter categoriesId) {
        this.categoriesId = categoriesId;
    }

    public LongFilter getLocationsId() {
        return locationsId;
    }

    public Optional<LongFilter> optionalLocationsId() {
        return Optional.ofNullable(locationsId);
    }

    public LongFilter locationsId() {
        if (locationsId == null) {
            setLocationsId(new LongFilter());
        }
        return locationsId;
    }

    public void setLocationsId(LongFilter locationsId) {
        this.locationsId = locationsId;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public Optional<Boolean> optionalDistinct() {
        return Optional.ofNullable(distinct);
    }

    public Boolean distinct() {
        if (distinct == null) {
            setDistinct(true);
        }
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ListingCriteria that = (ListingCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(title, that.title) &&
            Objects.equals(slug, that.slug) &&
            Objects.equals(status, that.status) &&
            Objects.equals(email, that.email) &&
            Objects.equals(telephone, that.telephone) &&
            Objects.equals(mobile, that.mobile) &&
            Objects.equals(website, that.website) &&
            Objects.equals(fax, that.fax) &&
            Objects.equals(taxCode, that.taxCode) &&
            Objects.equals(nameAlias, that.nameAlias) &&
            Objects.equals(nameEn, that.nameEn) &&
            Objects.equals(representative, that.representative) &&
            Objects.equals(mainIndustry, that.mainIndustry) &&
            Objects.equals(managedBy, that.managedBy) &&
            Objects.equals(businessType, that.businessType) &&
            Objects.equals(statusYp, that.statusYp) &&
            Objects.equals(foundedDate, that.foundedDate) &&
            Objects.equals(licenseModifiedDate, that.licenseModifiedDate) &&
            Objects.equals(latitude, that.latitude) &&
            Objects.equals(longitude, that.longitude) &&
            Objects.equals(apiId, that.apiId) &&
            Objects.equals(createdAt, that.createdAt) &&
            Objects.equals(updatedAt, that.updatedAt) &&
            Objects.equals(imagesId, that.imagesId) &&
            Objects.equals(galleriesId, that.galleriesId) &&
            Objects.equals(authorId, that.authorId) &&
            Objects.equals(categoriesId, that.categoriesId) &&
            Objects.equals(locationsId, that.locationsId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            title,
            slug,
            status,
            email,
            telephone,
            mobile,
            website,
            fax,
            taxCode,
            nameAlias,
            nameEn,
            representative,
            mainIndustry,
            managedBy,
            businessType,
            statusYp,
            foundedDate,
            licenseModifiedDate,
            latitude,
            longitude,
            apiId,
            createdAt,
            updatedAt,
            imagesId,
            galleriesId,
            authorId,
            categoriesId,
            locationsId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ListingCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalTitle().map(f -> "title=" + f + ", ").orElse("") +
            optionalSlug().map(f -> "slug=" + f + ", ").orElse("") +
            optionalStatus().map(f -> "status=" + f + ", ").orElse("") +
            optionalEmail().map(f -> "email=" + f + ", ").orElse("") +
            optionalTelephone().map(f -> "telephone=" + f + ", ").orElse("") +
            optionalMobile().map(f -> "mobile=" + f + ", ").orElse("") +
            optionalWebsite().map(f -> "website=" + f + ", ").orElse("") +
            optionalFax().map(f -> "fax=" + f + ", ").orElse("") +
            optionalTaxCode().map(f -> "taxCode=" + f + ", ").orElse("") +
            optionalNameAlias().map(f -> "nameAlias=" + f + ", ").orElse("") +
            optionalNameEn().map(f -> "nameEn=" + f + ", ").orElse("") +
            optionalRepresentative().map(f -> "representative=" + f + ", ").orElse("") +
            optionalMainIndustry().map(f -> "mainIndustry=" + f + ", ").orElse("") +
            optionalManagedBy().map(f -> "managedBy=" + f + ", ").orElse("") +
            optionalBusinessType().map(f -> "businessType=" + f + ", ").orElse("") +
            optionalStatusYp().map(f -> "statusYp=" + f + ", ").orElse("") +
            optionalFoundedDate().map(f -> "foundedDate=" + f + ", ").orElse("") +
            optionalLicenseModifiedDate().map(f -> "licenseModifiedDate=" + f + ", ").orElse("") +
            optionalLatitude().map(f -> "latitude=" + f + ", ").orElse("") +
            optionalLongitude().map(f -> "longitude=" + f + ", ").orElse("") +
            optionalApiId().map(f -> "apiId=" + f + ", ").orElse("") +
            optionalCreatedAt().map(f -> "createdAt=" + f + ", ").orElse("") +
            optionalUpdatedAt().map(f -> "updatedAt=" + f + ", ").orElse("") +
            optionalImagesId().map(f -> "imagesId=" + f + ", ").orElse("") +
            optionalGalleriesId().map(f -> "galleriesId=" + f + ", ").orElse("") +
            optionalAuthorId().map(f -> "authorId=" + f + ", ").orElse("") +
            optionalCategoriesId().map(f -> "categoriesId=" + f + ", ").orElse("") +
            optionalLocationsId().map(f -> "locationsId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
