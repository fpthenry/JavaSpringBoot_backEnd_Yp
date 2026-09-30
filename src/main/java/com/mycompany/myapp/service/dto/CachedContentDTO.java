package com.mycompany.myapp.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.mycompany.myapp.domain.CachedContent} entity.
 */
@Schema(description = "Cache HTML (bảng wp_vnyp_relate_locations)")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CachedContentDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 255)
    private String termSlug;

    private Instant expired;

    @Lob
    private String content;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTermSlug() {
        return termSlug;
    }

    public void setTermSlug(String termSlug) {
        this.termSlug = termSlug;
    }

    public Instant getExpired() {
        return expired;
    }

    public void setExpired(Instant expired) {
        this.expired = expired;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CachedContentDTO)) {
            return false;
        }

        CachedContentDTO cachedContentDTO = (CachedContentDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, cachedContentDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CachedContentDTO{" +
            "id=" + getId() +
            ", termSlug='" + getTermSlug() + "'" +
            ", expired='" + getExpired() + "'" +
            ", content='" + getContent() + "'" +
            "}";
    }
}
