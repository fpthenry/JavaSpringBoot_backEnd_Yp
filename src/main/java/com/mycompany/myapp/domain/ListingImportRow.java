package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;

/**
 * Một dòng dữ liệu của đợt nhập và kết quả kiểm tra trùng: khớp doanh nghiệp nào, vì sao, đã bổ sung trường nào.
 * Dữ liệu gốc giữ ở {@link #data} (JSON) để duyệt dòng nghi trùng và xử lý lại sau.
 */
@Entity
@Table(name = "listing_import_row")
public class ListingImportRow implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Kết quả xử lý một dòng. Ở đợt chạy thử: kết quả SẼ xảy ra, không ghi gì vào listing. */
    public enum Status {
        /** Không trùng → tạo doanh nghiệp mới (nháp, chờ duyệt). */
        CREATED,
        /** Trùng chắc chắn (MST / id nguồn) → đã bổ sung các ô còn trống. */
        MERGED,
        /** Trùng chắc chắn nhưng không có gì để bổ sung. */
        UNCHANGED,
        /** Nghi trùng (tên gần giống + SĐT / địa chỉ) → chờ admin duyệt. */
        SUSPECT,
        /** Trùng một dòng trước đó trong cùng đợt. */
        DUPLICATE_IN_BATCH,
        /** Thiếu tên doanh nghiệp. */
        INVALID,
        /** Admin duyệt: bỏ qua dòng nghi trùng. */
        SKIPPED,
    }

    /** Căn cứ khớp. */
    public enum MatchReason {
        TAX_CODE,
        SOURCE_REF,
        NAME_PHONE,
        NAME_ADDRESS,
        NAME,
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "row_no")
    private Integer rowNo;

    @Column(name = "source_ref", length = 255)
    private String sourceRef;

    @Column(name = "name", length = 500)
    private String name;

    @Column(name = "tax_code", length = 20)
    private String taxCode;

    @Column(name = "phone", length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_reason", length = 30)
    private MatchReason matchReason;

    @Column(name = "match_score")
    private Double matchScore;

    @Column(name = "matched_listing_id")
    private Long matchedListingId;

    @Column(name = "listing_id")
    private Long listingId;

    @Column(name = "filled_fields", length = 500)
    private String filledFields;

    @Column(name = "message", length = 1000)
    private String message;

    @Lob
    @Column(name = "data")
    private String data;

    @Column(name = "resolved_by", length = 50)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public Integer getRowNo() {
        return rowNo;
    }

    public void setRowNo(Integer rowNo) {
        this.rowNo = rowNo;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = cut(sourceRef, 255);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = cut(name, 500);
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = cut(phone, 30);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public MatchReason getMatchReason() {
        return matchReason;
    }

    public void setMatchReason(MatchReason matchReason) {
        this.matchReason = matchReason;
    }

    public Double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Double matchScore) {
        this.matchScore = matchScore;
    }

    public Long getMatchedListingId() {
        return matchedListingId;
    }

    public void setMatchedListingId(Long matchedListingId) {
        this.matchedListingId = matchedListingId;
    }

    public Long getListingId() {
        return listingId;
    }

    public void setListingId(Long listingId) {
        this.listingId = listingId;
    }

    public String getFilledFields() {
        return filledFields;
    }

    public void setFilledFields(String filledFields) {
        this.filledFields = cut(filledFields, 500);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = cut(message, 1000);
    }

    /** Thêm một ghi chú (nối bằng "; "). */
    public void addMessage(String note) {
        setMessage(message == null || message.isEmpty() ? note : message + "; " + note);
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof ListingImportRow other && id != null && id.equals(other.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ListingImportRow{id=" + id + ", batchId=" + batchId + ", rowNo=" + rowNo + ", status=" + status + "}";
    }
}
