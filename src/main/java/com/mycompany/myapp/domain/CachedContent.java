package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Cache HTML (bảng wp_vnyp_relate_locations)
 */
@Entity
@Table(name = "cached_content")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "cachedcontent")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CachedContent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Long)
    private Long id;

    @NotNull
    @Size(max = 255)
    @Column(name = "term_slug", length = 255, nullable = false, unique = true)
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
    private String termSlug;

    @Column(name = "expired")
    @org.springframework.data.elasticsearch.annotations.Field(
        type = org.springframework.data.elasticsearch.annotations.FieldType.Date,
        format = org.springframework.data.elasticsearch.annotations.DateFormat.date_time
    )
    private Instant expired;

    @Lob
    @Column(name = "content")
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
    private String content;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public CachedContent id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTermSlug() {
        return this.termSlug;
    }

    public CachedContent termSlug(String termSlug) {
        this.setTermSlug(termSlug);
        return this;
    }

    public void setTermSlug(String termSlug) {
        this.termSlug = termSlug;
    }

    public Instant getExpired() {
        return this.expired;
    }

    public CachedContent expired(Instant expired) {
        this.setExpired(expired);
        return this;
    }

    public void setExpired(Instant expired) {
        this.expired = expired;
    }

    public String getContent() {
        return this.content;
    }

    public CachedContent content(String content) {
        this.setContent(content);
        return this;
    }

    public void setContent(String content) {
        this.content = content;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CachedContent)) {
            return false;
        }
        return getId() != null && getId().equals(((CachedContent) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CachedContent{" +
            "id=" + getId() +
            ", termSlug='" + getTermSlug() + "'" +
            ", expired='" + getExpired() + "'" +
            ", content='" + getContent() + "'" +
            "}";
    }
}
