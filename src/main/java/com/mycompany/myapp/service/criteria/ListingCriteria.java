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

    private LongFilter wpId;

    private StringFilter apiId;

    private StringFilter name;

    private StringFilter nameEn;

    private StringFilter nameAlias;

    private StringFilter slug;

    private StringFilter phone;

    private StringFilter mobile;

    private StringFilter email;

    private StringFilter website;

    private StringFilter taxCode;

    private StringFilter representative;

    private StringFilter capital;

    private StringFilter foundedYear;

    private StringFilter businessType;

    private StringFilter businessStatus;

    private StringFilter industryCode;

    private StringFilter managedBy;

    private StringFilter thumbnail;

    private IntegerFilter viewCount;

    private BooleanFilter isFeatured;

    private StringFilter status;

    private InstantFilter publishedAt;

    private InstantFilter modifiedAt;

    private InstantFilter createdAt;

    private InstantFilter updatedAt;

    private BooleanFilter esIndexed;

    private LongFilter categoryId;

    private LongFilter locationId;

    /** Sửa tay: lọc theo cả cây địa phương (địa phương được chọn + quận/huyện + phường/xã bên dưới). */
    private LongFilter locationTreeId;

    private Boolean distinct;

    public ListingCriteria() {}

    public ListingCriteria(ListingCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.wpId = other.optionalWpId().map(LongFilter::copy).orElse(null);
        this.apiId = other.optionalApiId().map(StringFilter::copy).orElse(null);
        this.name = other.optionalName().map(StringFilter::copy).orElse(null);
        this.nameEn = other.optionalNameEn().map(StringFilter::copy).orElse(null);
        this.nameAlias = other.optionalNameAlias().map(StringFilter::copy).orElse(null);
        this.slug = other.optionalSlug().map(StringFilter::copy).orElse(null);
        this.phone = other.optionalPhone().map(StringFilter::copy).orElse(null);
        this.mobile = other.optionalMobile().map(StringFilter::copy).orElse(null);
        this.email = other.optionalEmail().map(StringFilter::copy).orElse(null);
        this.website = other.optionalWebsite().map(StringFilter::copy).orElse(null);
        this.taxCode = other.optionalTaxCode().map(StringFilter::copy).orElse(null);
        this.representative = other.optionalRepresentative().map(StringFilter::copy).orElse(null);
        this.capital = other.optionalCapital().map(StringFilter::copy).orElse(null);
        this.foundedYear = other.optionalFoundedYear().map(StringFilter::copy).orElse(null);
        this.businessType = other.optionalBusinessType().map(StringFilter::copy).orElse(null);
        this.businessStatus = other.optionalBusinessStatus().map(StringFilter::copy).orElse(null);
        this.industryCode = other.optionalIndustryCode().map(StringFilter::copy).orElse(null);
        this.managedBy = other.optionalManagedBy().map(StringFilter::copy).orElse(null);
        this.thumbnail = other.optionalThumbnail().map(StringFilter::copy).orElse(null);
        this.viewCount = other.optionalViewCount().map(IntegerFilter::copy).orElse(null);
        this.isFeatured = other.optionalIsFeatured().map(BooleanFilter::copy).orElse(null);
        this.status = other.optionalStatus().map(StringFilter::copy).orElse(null);
        this.publishedAt = other.optionalPublishedAt().map(InstantFilter::copy).orElse(null);
        this.modifiedAt = other.optionalModifiedAt().map(InstantFilter::copy).orElse(null);
        this.createdAt = other.optionalCreatedAt().map(InstantFilter::copy).orElse(null);
        this.updatedAt = other.optionalUpdatedAt().map(InstantFilter::copy).orElse(null);
        this.esIndexed = other.optionalEsIndexed().map(BooleanFilter::copy).orElse(null);
        this.categoryId = other.optionalCategoryId().map(LongFilter::copy).orElse(null);
        this.locationId = other.optionalLocationId().map(LongFilter::copy).orElse(null);
        this.locationTreeId = other.optionalLocationTreeId().map(LongFilter::copy).orElse(null);
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

    public LongFilter getWpId() {
        return wpId;
    }

    public Optional<LongFilter> optionalWpId() {
        return Optional.ofNullable(wpId);
    }

    public LongFilter wpId() {
        if (wpId == null) {
            setWpId(new LongFilter());
        }
        return wpId;
    }

    public void setWpId(LongFilter wpId) {
        this.wpId = wpId;
    }

    public StringFilter getApiId() {
        return apiId;
    }

    public Optional<StringFilter> optionalApiId() {
        return Optional.ofNullable(apiId);
    }

    public StringFilter apiId() {
        if (apiId == null) {
            setApiId(new StringFilter());
        }
        return apiId;
    }

    public void setApiId(StringFilter apiId) {
        this.apiId = apiId;
    }

    public StringFilter getName() {
        return name;
    }

    public Optional<StringFilter> optionalName() {
        return Optional.ofNullable(name);
    }

    public StringFilter name() {
        if (name == null) {
            setName(new StringFilter());
        }
        return name;
    }

    public void setName(StringFilter name) {
        this.name = name;
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

    public StringFilter getPhone() {
        return phone;
    }

    public Optional<StringFilter> optionalPhone() {
        return Optional.ofNullable(phone);
    }

    public StringFilter phone() {
        if (phone == null) {
            setPhone(new StringFilter());
        }
        return phone;
    }

    public void setPhone(StringFilter phone) {
        this.phone = phone;
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

    public StringFilter getCapital() {
        return capital;
    }

    public Optional<StringFilter> optionalCapital() {
        return Optional.ofNullable(capital);
    }

    public StringFilter capital() {
        if (capital == null) {
            setCapital(new StringFilter());
        }
        return capital;
    }

    public void setCapital(StringFilter capital) {
        this.capital = capital;
    }

    public StringFilter getFoundedYear() {
        return foundedYear;
    }

    public Optional<StringFilter> optionalFoundedYear() {
        return Optional.ofNullable(foundedYear);
    }

    public StringFilter foundedYear() {
        if (foundedYear == null) {
            setFoundedYear(new StringFilter());
        }
        return foundedYear;
    }

    public void setFoundedYear(StringFilter foundedYear) {
        this.foundedYear = foundedYear;
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

    public StringFilter getBusinessStatus() {
        return businessStatus;
    }

    public Optional<StringFilter> optionalBusinessStatus() {
        return Optional.ofNullable(businessStatus);
    }

    public StringFilter businessStatus() {
        if (businessStatus == null) {
            setBusinessStatus(new StringFilter());
        }
        return businessStatus;
    }

    public void setBusinessStatus(StringFilter businessStatus) {
        this.businessStatus = businessStatus;
    }

    public StringFilter getIndustryCode() {
        return industryCode;
    }

    public Optional<StringFilter> optionalIndustryCode() {
        return Optional.ofNullable(industryCode);
    }

    public StringFilter industryCode() {
        if (industryCode == null) {
            setIndustryCode(new StringFilter());
        }
        return industryCode;
    }

    public void setIndustryCode(StringFilter industryCode) {
        this.industryCode = industryCode;
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

    public StringFilter getThumbnail() {
        return thumbnail;
    }

    public Optional<StringFilter> optionalThumbnail() {
        return Optional.ofNullable(thumbnail);
    }

    public StringFilter thumbnail() {
        if (thumbnail == null) {
            setThumbnail(new StringFilter());
        }
        return thumbnail;
    }

    public void setThumbnail(StringFilter thumbnail) {
        this.thumbnail = thumbnail;
    }

    public IntegerFilter getViewCount() {
        return viewCount;
    }

    public Optional<IntegerFilter> optionalViewCount() {
        return Optional.ofNullable(viewCount);
    }

    public IntegerFilter viewCount() {
        if (viewCount == null) {
            setViewCount(new IntegerFilter());
        }
        return viewCount;
    }

    public void setViewCount(IntegerFilter viewCount) {
        this.viewCount = viewCount;
    }

    public BooleanFilter getIsFeatured() {
        return isFeatured;
    }

    public Optional<BooleanFilter> optionalIsFeatured() {
        return Optional.ofNullable(isFeatured);
    }

    public BooleanFilter isFeatured() {
        if (isFeatured == null) {
            setIsFeatured(new BooleanFilter());
        }
        return isFeatured;
    }

    public void setIsFeatured(BooleanFilter isFeatured) {
        this.isFeatured = isFeatured;
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

    public InstantFilter getPublishedAt() {
        return publishedAt;
    }

    public Optional<InstantFilter> optionalPublishedAt() {
        return Optional.ofNullable(publishedAt);
    }

    public InstantFilter publishedAt() {
        if (publishedAt == null) {
            setPublishedAt(new InstantFilter());
        }
        return publishedAt;
    }

    public void setPublishedAt(InstantFilter publishedAt) {
        this.publishedAt = publishedAt;
    }

    public InstantFilter getModifiedAt() {
        return modifiedAt;
    }

    public Optional<InstantFilter> optionalModifiedAt() {
        return Optional.ofNullable(modifiedAt);
    }

    public InstantFilter modifiedAt() {
        if (modifiedAt == null) {
            setModifiedAt(new InstantFilter());
        }
        return modifiedAt;
    }

    public void setModifiedAt(InstantFilter modifiedAt) {
        this.modifiedAt = modifiedAt;
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

    public BooleanFilter getEsIndexed() {
        return esIndexed;
    }

    public Optional<BooleanFilter> optionalEsIndexed() {
        return Optional.ofNullable(esIndexed);
    }

    public BooleanFilter esIndexed() {
        if (esIndexed == null) {
            setEsIndexed(new BooleanFilter());
        }
        return esIndexed;
    }

    public void setEsIndexed(BooleanFilter esIndexed) {
        this.esIndexed = esIndexed;
    }

    public LongFilter getCategoryId() {
        return categoryId;
    }

    public Optional<LongFilter> optionalCategoryId() {
        return Optional.ofNullable(categoryId);
    }

    public LongFilter categoryId() {
        if (categoryId == null) {
            setCategoryId(new LongFilter());
        }
        return categoryId;
    }

    public void setCategoryId(LongFilter categoryId) {
        this.categoryId = categoryId;
    }

    public LongFilter getLocationId() {
        return locationId;
    }

    public Optional<LongFilter> optionalLocationId() {
        return Optional.ofNullable(locationId);
    }

    public LongFilter locationId() {
        if (locationId == null) {
            setLocationId(new LongFilter());
        }
        return locationId;
    }

    public void setLocationId(LongFilter locationId) {
        this.locationId = locationId;
    }

    public LongFilter getLocationTreeId() {
        return locationTreeId;
    }

    public Optional<LongFilter> optionalLocationTreeId() {
        return Optional.ofNullable(locationTreeId);
    }

    public LongFilter locationTreeId() {
        if (locationTreeId == null) {
            setLocationTreeId(new LongFilter());
        }
        return locationTreeId;
    }

    public void setLocationTreeId(LongFilter locationTreeId) {
        this.locationTreeId = locationTreeId;
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
            Objects.equals(wpId, that.wpId) &&
            Objects.equals(apiId, that.apiId) &&
            Objects.equals(name, that.name) &&
            Objects.equals(nameEn, that.nameEn) &&
            Objects.equals(nameAlias, that.nameAlias) &&
            Objects.equals(slug, that.slug) &&
            Objects.equals(phone, that.phone) &&
            Objects.equals(mobile, that.mobile) &&
            Objects.equals(email, that.email) &&
            Objects.equals(website, that.website) &&
            Objects.equals(taxCode, that.taxCode) &&
            Objects.equals(representative, that.representative) &&
            Objects.equals(capital, that.capital) &&
            Objects.equals(foundedYear, that.foundedYear) &&
            Objects.equals(businessType, that.businessType) &&
            Objects.equals(businessStatus, that.businessStatus) &&
            Objects.equals(industryCode, that.industryCode) &&
            Objects.equals(managedBy, that.managedBy) &&
            Objects.equals(thumbnail, that.thumbnail) &&
            Objects.equals(viewCount, that.viewCount) &&
            Objects.equals(isFeatured, that.isFeatured) &&
            Objects.equals(status, that.status) &&
            Objects.equals(publishedAt, that.publishedAt) &&
            Objects.equals(modifiedAt, that.modifiedAt) &&
            Objects.equals(createdAt, that.createdAt) &&
            Objects.equals(updatedAt, that.updatedAt) &&
            Objects.equals(esIndexed, that.esIndexed) &&
            Objects.equals(categoryId, that.categoryId) &&
            Objects.equals(locationId, that.locationId) &&
            Objects.equals(locationTreeId, that.locationTreeId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            wpId,
            apiId,
            name,
            nameEn,
            nameAlias,
            slug,
            phone,
            mobile,
            email,
            website,
            taxCode,
            representative,
            capital,
            foundedYear,
            businessType,
            businessStatus,
            industryCode,
            managedBy,
            thumbnail,
            viewCount,
            isFeatured,
            status,
            publishedAt,
            modifiedAt,
            createdAt,
            updatedAt,
            esIndexed,
            categoryId,
            locationId,
            locationTreeId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ListingCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalWpId().map(f -> "wpId=" + f + ", ").orElse("") +
            optionalApiId().map(f -> "apiId=" + f + ", ").orElse("") +
            optionalName().map(f -> "name=" + f + ", ").orElse("") +
            optionalNameEn().map(f -> "nameEn=" + f + ", ").orElse("") +
            optionalNameAlias().map(f -> "nameAlias=" + f + ", ").orElse("") +
            optionalSlug().map(f -> "slug=" + f + ", ").orElse("") +
            optionalPhone().map(f -> "phone=" + f + ", ").orElse("") +
            optionalMobile().map(f -> "mobile=" + f + ", ").orElse("") +
            optionalEmail().map(f -> "email=" + f + ", ").orElse("") +
            optionalWebsite().map(f -> "website=" + f + ", ").orElse("") +
            optionalTaxCode().map(f -> "taxCode=" + f + ", ").orElse("") +
            optionalRepresentative().map(f -> "representative=" + f + ", ").orElse("") +
            optionalCapital().map(f -> "capital=" + f + ", ").orElse("") +
            optionalFoundedYear().map(f -> "foundedYear=" + f + ", ").orElse("") +
            optionalBusinessType().map(f -> "businessType=" + f + ", ").orElse("") +
            optionalBusinessStatus().map(f -> "businessStatus=" + f + ", ").orElse("") +
            optionalIndustryCode().map(f -> "industryCode=" + f + ", ").orElse("") +
            optionalManagedBy().map(f -> "managedBy=" + f + ", ").orElse("") +
            optionalThumbnail().map(f -> "thumbnail=" + f + ", ").orElse("") +
            optionalViewCount().map(f -> "viewCount=" + f + ", ").orElse("") +
            optionalIsFeatured().map(f -> "isFeatured=" + f + ", ").orElse("") +
            optionalStatus().map(f -> "status=" + f + ", ").orElse("") +
            optionalPublishedAt().map(f -> "publishedAt=" + f + ", ").orElse("") +
            optionalModifiedAt().map(f -> "modifiedAt=" + f + ", ").orElse("") +
            optionalCreatedAt().map(f -> "createdAt=" + f + ", ").orElse("") +
            optionalUpdatedAt().map(f -> "updatedAt=" + f + ", ").orElse("") +
            optionalEsIndexed().map(f -> "esIndexed=" + f + ", ").orElse("") +
            optionalCategoryId().map(f -> "categoryId=" + f + ", ").orElse("") +
            optionalLocationId().map(f -> "locationId=" + f + ", ").orElse("") +
            optionalLocationTreeId().map(f -> "locationTreeId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
