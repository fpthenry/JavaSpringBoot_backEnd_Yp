package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.ListingImportRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ListingImportRowRepository extends JpaRepository<ListingImportRow, Long> {
    Page<ListingImportRow> findByBatchId(Long batchId, Pageable pageable);

    Page<ListingImportRow> findByBatchIdAndStatus(Long batchId, ListingImportRow.Status status, Pageable pageable);
}
