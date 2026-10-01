package com.mycompany.myapp.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.Location} entity.
 */
@Schema(description = "Địa phương (bảng location)")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class LocationDTO implements Serializable {

    private Long id;

    @Schema(description = "wp_terms.term_id")
    private Long wpTermId;

    @NotNull
    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String slug;

    @Size(max = 50)
    @Schema(description = "province, district, ward")
    private String type;

    @Size(max = 50)
    private String code;

    private Double latitude;

    private Double longitude;

    private Instant createdAt;

    private LocationDTO parent;

    private Set<ListingDTO> listings = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWpTermId() {
        return wpTermId;
    }

    public void setWpTermId(Long wpTermId) {
        this.wpTermId = wpTermId;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public LocationDTO getParent() {
        return parent;
    }

    public void setParent(LocationDTO parent) {
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
        if (!(o instanceof LocationDTO)) {
            return false;
        }

        LocationDTO locationDTO = (LocationDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, locationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "LocationDTO{" +
            "id=" + getId() +
            ", wpTermId=" + getWpTermId() +
            ", name='" + getName() + "'" +
            ", slug='" + getSlug() + "'" +
            ", type='" + getType() + "'" +
            ", code='" + getCode() + "'" +
            ", latitude=" + getLatitude() +
            ", longitude=" + getLongitude() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", parent=" + getParent() +
            ", listings=" + getListings() +
            "}";
    }
}
