package com.mycompany.myapp.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.RedirectRule} entity.
 */
@Schema(description = "Redirect 301 (bảng redirect_301)")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RedirectRuleDTO implements Serializable {

    private Long id;

    private Long sourceId;

    @NotNull
    @Size(max = 500)
    private String sourceSlug;

    private Long destinationId;

    @NotNull
    @Size(max = 500)
    private String destinationSlug;

    @NotNull
    @Size(max = 50)
    private String objectType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceSlug() {
        return sourceSlug;
    }

    public void setSourceSlug(String sourceSlug) {
        this.sourceSlug = sourceSlug;
    }

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    public String getDestinationSlug() {
        return destinationSlug;
    }

    public void setDestinationSlug(String destinationSlug) {
        this.destinationSlug = destinationSlug;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RedirectRuleDTO)) {
            return false;
        }

        RedirectRuleDTO redirectRuleDTO = (RedirectRuleDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, redirectRuleDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RedirectRuleDTO{" +
            "id=" + getId() +
            ", sourceId=" + getSourceId() +
            ", sourceSlug='" + getSourceSlug() + "'" +
            ", destinationId=" + getDestinationId() +
            ", destinationSlug='" + getDestinationSlug() + "'" +
            ", objectType='" + getObjectType() + "'" +
            "}";
    }
}
