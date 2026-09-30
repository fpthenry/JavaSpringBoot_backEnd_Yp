package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;

/**
 * Redirect 301 (bảng redirect_301)
 */
@Entity
@Table(name = "redirect_rule")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "redirectrule")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RedirectRule implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Long)
    private Long id;

    @Column(name = "source_id")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Long)
    private Long sourceId;

    @NotNull
    @Size(max = 500)
    @Column(name = "source_slug", length = 500, nullable = false)
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
    private String sourceSlug;

    @Column(name = "destination_id")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Long)
    private Long destinationId;

    @NotNull
    @Size(max = 500)
    @Column(name = "destination_slug", length = 500, nullable = false)
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
    private String destinationSlug;

    @NotNull
    @Size(max = 50)
    @Column(name = "object_type", length = 50, nullable = false)
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
    private String objectType;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public RedirectRule id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSourceId() {
        return this.sourceId;
    }

    public RedirectRule sourceId(Long sourceId) {
        this.setSourceId(sourceId);
        return this;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceSlug() {
        return this.sourceSlug;
    }

    public RedirectRule sourceSlug(String sourceSlug) {
        this.setSourceSlug(sourceSlug);
        return this;
    }

    public void setSourceSlug(String sourceSlug) {
        this.sourceSlug = sourceSlug;
    }

    public Long getDestinationId() {
        return this.destinationId;
    }

    public RedirectRule destinationId(Long destinationId) {
        this.setDestinationId(destinationId);
        return this;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    public String getDestinationSlug() {
        return this.destinationSlug;
    }

    public RedirectRule destinationSlug(String destinationSlug) {
        this.setDestinationSlug(destinationSlug);
        return this;
    }

    public void setDestinationSlug(String destinationSlug) {
        this.destinationSlug = destinationSlug;
    }

    public String getObjectType() {
        return this.objectType;
    }

    public RedirectRule objectType(String objectType) {
        this.setObjectType(objectType);
        return this;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RedirectRule)) {
            return false;
        }
        return getId() != null && getId().equals(((RedirectRule) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RedirectRule{" +
            "id=" + getId() +
            ", sourceId=" + getSourceId() +
            ", sourceSlug='" + getSourceSlug() + "'" +
            ", destinationId=" + getDestinationId() +
            ", destinationSlug='" + getDestinationSlug() + "'" +
            ", objectType='" + getObjectType() + "'" +
            "}";
    }
}
