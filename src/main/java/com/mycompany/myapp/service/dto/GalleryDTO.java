package com.mycompany.myapp.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.Gallery} entity.
 */
@Schema(description = "Vị trí banner (gallery)")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class GalleryDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 255)
    @Schema(description = "Tên hiển thị, vd: footer banner", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull
    @Size(max = 100)
    @Pattern(regexp = "^[a-z0-9-]+$")
    @Schema(
        description = "Mã duy nhất FE dùng để lấy, chữ thường, số và gạch ngang, vd: footer-banner",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String code;

    @Lob
    private String description;

    @NotNull
    @Schema(description = "Tắt thì API công khai không trả vị trí này", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean active;

    @Schema(description = "ID gallery cũ trên WordPress (wp_posts.ID), để đối chiếu")
    private Long wpId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Long getWpId() {
        return wpId;
    }

    public void setWpId(Long wpId) {
        this.wpId = wpId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GalleryDTO)) {
            return false;
        }

        GalleryDTO galleryDTO = (GalleryDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, galleryDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "GalleryDTO{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", code='" + getCode() + "'" +
            ", description='" + getDescription() + "'" +
            ", active='" + getActive() + "'" +
            ", wpId=" + getWpId() +
            "}";
    }
}
