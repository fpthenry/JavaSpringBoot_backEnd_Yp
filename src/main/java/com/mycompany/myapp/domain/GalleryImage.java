package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Ảnh quảng cáo trong một vị trí banner
 */
@Entity
@Table(name = "gallery_image")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class GalleryImage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Size(max = 255)
    @Column(name = "title", length = 255)
    private String title;

    /**
     * Ảnh upload lên (lưu trong database)
     */
    @Lob
    @Column(name = "image")
    private byte[] image;

    @Column(name = "image_content_type")
    private String imageContentType;

    /**
     * Hoặc link ảnh có sẵn (CDN, wp-content/uploads...); dùng khi không upload
     */
    @Size(max = 1000)
    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    /**
     * Bấm vào ảnh thì mở link này
     */
    @Size(max = 1000)
    @Column(name = "link_url", length = 1000)
    private String linkUrl;

    @Size(max = 255)
    @Column(name = "alt_text", length = 255)
    private String altText;

    /**
     * Thứ tự hiển thị, nhỏ đứng trước
     */
    @NotNull
    @Min(value = 0)
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    /**
     * Hẹn giờ: chỉ hiển thị từ thời điểm này (bỏ trống = ngay)
     */
    @Column(name = "start_at")
    private Instant startAt;

    /**
     * Hẹn giờ: ngừng hiển thị sau thời điểm này (bỏ trống = không hết hạn)
     */
    @Column(name = "end_at")
    private Instant endAt;

    @Column(name = "open_in_new_tab")
    private Boolean openInNewTab;

    @ManyToOne(optional = false)
    @NotNull
    private Gallery gallery;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public GalleryImage id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public GalleryImage title(String title) {
        this.setTitle(title);
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public byte[] getImage() {
        return this.image;
    }

    public GalleryImage image(byte[] image) {
        this.setImage(image);
        return this;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public String getImageContentType() {
        return this.imageContentType;
    }

    public GalleryImage imageContentType(String imageContentType) {
        this.imageContentType = imageContentType;
        return this;
    }

    public void setImageContentType(String imageContentType) {
        this.imageContentType = imageContentType;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    public GalleryImage imageUrl(String imageUrl) {
        this.setImageUrl(imageUrl);
        return this;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLinkUrl() {
        return this.linkUrl;
    }

    public GalleryImage linkUrl(String linkUrl) {
        this.setLinkUrl(linkUrl);
        return this;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public String getAltText() {
        return this.altText;
    }

    public GalleryImage altText(String altText) {
        this.setAltText(altText);
        return this;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public Integer getDisplayOrder() {
        return this.displayOrder;
    }

    public GalleryImage displayOrder(Integer displayOrder) {
        this.setDisplayOrder(displayOrder);
        return this;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getActive() {
        return this.active;
    }

    public GalleryImage active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Instant getStartAt() {
        return this.startAt;
    }

    public GalleryImage startAt(Instant startAt) {
        this.setStartAt(startAt);
        return this;
    }

    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }

    public Instant getEndAt() {
        return this.endAt;
    }

    public GalleryImage endAt(Instant endAt) {
        this.setEndAt(endAt);
        return this;
    }

    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }

    public Boolean getOpenInNewTab() {
        return this.openInNewTab;
    }

    public GalleryImage openInNewTab(Boolean openInNewTab) {
        this.setOpenInNewTab(openInNewTab);
        return this;
    }

    public void setOpenInNewTab(Boolean openInNewTab) {
        this.openInNewTab = openInNewTab;
    }

    public Gallery getGallery() {
        return this.gallery;
    }

    public void setGallery(Gallery gallery) {
        this.gallery = gallery;
    }

    public GalleryImage gallery(Gallery gallery) {
        this.setGallery(gallery);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GalleryImage)) {
            return false;
        }
        return getId() != null && getId().equals(((GalleryImage) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "GalleryImage{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", image='" + getImage() + "'" +
            ", imageContentType='" + getImageContentType() + "'" +
            ", imageUrl='" + getImageUrl() + "'" +
            ", linkUrl='" + getLinkUrl() + "'" +
            ", altText='" + getAltText() + "'" +
            ", displayOrder=" + getDisplayOrder() +
            ", active='" + getActive() + "'" +
            ", startAt='" + getStartAt() + "'" +
            ", endAt='" + getEndAt() + "'" +
            ", openInNewTab='" + getOpenInNewTab() + "'" +
            "}";
    }
}
