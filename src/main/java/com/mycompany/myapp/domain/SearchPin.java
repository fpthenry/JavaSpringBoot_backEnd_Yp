package com.mycompany.myapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Viết tay (2026-11-17): một doanh nghiệp được ghim lên đầu kết quả khi khách tìm đúng từ khóa (như "Custom Search Results"
 * của ElasticPress trên WordPress cũ). So khớp từ khóa không phân biệt dấu, hoa thường (xem SearchPinService.key).
 */
@Entity
@Table(name = "search_pin")
public class SearchPin implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Từ khóa như người quản trị nhập (giữ dấu để hiển thị). */
    @Column(name = "keyword", length = 255, nullable = false)
    private String keyword;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    /** Thứ tự trong cùng từ khóa, nhỏ đứng trước. */
    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getListingId() {
        return listingId;
    }

    public void setListingId(Long listingId) {
        this.listingId = listingId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
