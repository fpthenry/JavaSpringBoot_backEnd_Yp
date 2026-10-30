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
 * A DTO for the {@link com.mycompany.myapp.domain.Category} entity.
 */
@Schema(description = "Ngành nghề (bảng category)")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CategoryDTO implements Serializable {

    private Long id;

    // Sửa tay: mã ngành VSIC 2025 thay wpTermId; cấp do máy chủ suy ra từ mã
    @Schema(description = "Mã ngành VSIC 2025: chữ cái A–V (cấp 1) hoặc 2–5 chữ số (cấp 2–5)", example = "4632")
    @NotNull
    @Size(max = 10)
    @Pattern(regexp = "^([A-Z]|\\d{2,5})$", message = "Mã ngành là một chữ cái A–Z hoặc 2–5 chữ số")
    private String code;

    @Schema(description = "Cấp trong hệ thống ngành (1–5), suy ra từ mã khi lưu", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer level;

    @NotNull
    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String slug;

    @Lob
    private String description;

    private Integer listingCount;

    private Instant createdAt;

    private Instant updatedAt;

    private CategoryDTO parent;

    private Set<ListingDTO> listings = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Integer getListingCount() {
        return listingCount;
    }

    public void setListingCount(Integer listingCount) {
        this.listingCount = listingCount;
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

    public CategoryDTO getParent() {
        return parent;
    }

    public void setParent(CategoryDTO parent) {
        this.parent = parent;
    }

    public Set<ListingDTO> getListings() {
        return listings;
    }

    public void setListings(Set<ListingDTO> listings) {
        this.listings = listings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CategoryDTO)) {
            return false;
        }

        CategoryDTO categoryDTO = (CategoryDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, categoryDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CategoryDTO{" +
            "id=" + getId() +
            ", code='" + getCode() + "'" +
            ", level=" + getLevel() +
            ", name='" + getName() + "'" +
            ", slug='" + getSlug() + "'" +
            ", description='" + getDescription() + "'" +
            ", listingCount=" + getListingCount() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            ", parent=" + getParent() +
            ", listings=" + getListings() +
            "}";
    }
}
