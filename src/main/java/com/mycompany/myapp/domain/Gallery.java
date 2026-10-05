package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;

/**
 * Vị trí banner (gallery)
 */
@Entity
@Table(name = "gallery")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Gallery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Tên hiển thị, vd: footer banner
     */
    @NotNull
    @Size(max = 255)
    @Column(name = "name", length = 255, nullable = false)
    private String name;

    /**
     * Mã duy nhất FE dùng để lấy, chữ thường, số và gạch ngang, vd: footer-banner
     */
    @NotNull
    @Size(max = 100)
    @Pattern(regexp = "^[a-z0-9-]+$")
    @Column(name = "code", length = 100, nullable = false, unique = true)
    private String code;

    @Lob
    @Column(name = "description")
    private String description;

    /**
     * Tắt thì API công khai không trả vị trí này
     */
    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    /**
     * ID gallery cũ trên WordPress (wp_posts.ID), để đối chiếu
     */
    @Column(name = "wp_id", unique = true)
    private Long wpId;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Gallery id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public Gallery name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return this.code;
    }

    public Gallery code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return this.description;
    }

    public Gallery description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return this.active;
    }

    public Gallery active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Long getWpId() {
        return this.wpId;
    }

    public Gallery wpId(Long wpId) {
        this.setWpId(wpId);
        return this;
    }

    public void setWpId(Long wpId) {
        this.wpId = wpId;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Gallery)) {
            return false;
        }
        return getId() != null && getId().equals(((Gallery) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Gallery{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", code='" + getCode() + "'" +
            ", description='" + getDescription() + "'" +
            ", active='" + getActive() + "'" +
            ", wpId=" + getWpId() +
            "}";
    }
}
