package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.ListingImportBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ListingImportBatchRepository extends JpaRepository<ListingImportBatch, Long> {
    Page<ListingImportBatch> findByDryRun(boolean dryRun, Pageable pageable);
}
