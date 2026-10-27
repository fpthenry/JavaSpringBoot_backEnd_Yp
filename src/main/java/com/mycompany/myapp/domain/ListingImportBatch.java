package com.mycompany.myapp.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;

/**
 * Một đợt nhập (clone) doanh nghiệp từ nguồn ngoài: file Excel/CSV, API bên thứ ba, JSON gửi thẳng.
 * Viết tay (changelog 20261026090000_listing_import.xml), không sinh từ JDL. Đếm kết quả cập nhật sau mỗi lô khi job chạy.
 */
@Entity
@Table(name = "listing_import_batch")
public class ListingImportBatch implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String RUNNING = "RUNNING";
    public static final String DONE = "DONE";
    public static final String FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ghi vào listing.source của doanh nghiệp được tạo: {@code file}, {@code api:<mã>}, {@code json:<mã>}. */
    @Column(name = "source", length = 50, nullable = false)
    private String source;

    /** Tên file hoặc URL API. */
    @Column(name = "source_name", length = 500)
    private String sourceName;

    /** Chạy thử: chỉ phân loại (tạo mới / bổ sung / nghi trùng…), không ghi vào listing. */
    @Column(name = "dry_run", nullable = false)
    private boolean dryRun;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "total_rows")
    private int totalRows;

    @Column(name = "created_count")
    private int createdCount;

    @Column(name = "merged_count")
    private int mergedCount;

    @Column(name = "unchanged_count")
    private int unchangedCount;

    @Column(name = "suspect_count")
    private int suspectCount;

    @Column(name = "duplicate_in_batch_count")
    private int duplicateInBatchCount;

    @Column(name = "invalid_count")
    private int invalidCount;

    @Column(name = "error", length = 2000)
    private String error;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    /** Cộng kết quả của một dòng vào bộ đếm của đợt. */
    public void count(ListingImportRow.Status status) {
        totalRows++;
        switch (status) {
            case CREATED -> createdCount++;
            case MERGED -> mergedCount++;
            case UNCHANGED -> unchangedCount++;
            case SUSPECT -> suspectCount++;
            case DUPLICATE_IN_BATCH -> duplicateInBatchCount++;
            case INVALID -> invalidCount++;
            case SKIPPED -> {
                // chỉ có khi duyệt tay, không đếm lúc nhập
            }
        }
    }

    /** Duyệt tay một dòng nghi trùng: chuyển bộ đếm từ SUSPECT sang kết quả mới. */
    public void resolveSuspect(ListingImportRow.Status to) {
        suspectCount = Math.max(0, suspectCount - 1);
        switch (to) {
            case CREATED -> createdCount++;
            case MERGED -> mergedCount++;
            case UNCHANGED -> unchangedCount++;
            default -> {
                // SKIPPED: chỉ bớt nghi trùng
            }
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getCreatedCount() {
        return createdCount;
    }

    public int getMergedCount() {
        return mergedCount;
    }

    public int getUnchangedCount() {
        return unchangedCount;
    }

    public int getSuspectCount() {
        return suspectCount;
    }

    public int getDuplicateInBatchCount() {
        return duplicateInBatchCount;
    }

    public int getInvalidCount() {
        return invalidCount;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error == null ? null : error.substring(0, Math.min(error.length(), 2000));
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof ListingImportBatch other && id != null && id.equals(other.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return (
            "ListingImportBatch{id=" +
            id +
            ", source='" +
            source +
            "', status='" +
            status +
            "', total=" +
            totalRows +
            ", created=" +
            createdCount +
            ", merged=" +
            mergedCount +
            ", suspect=" +
            suspectCount +
            "}"
        );
    }
}
