package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.ListingImportRow;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ListingImportRowRepository extends JpaRepository<ListingImportRow, Long>, JpaSpecificationExecutor<ListingImportRow> {
    /** Duyệt lùi theo id: hoàn tác theo thứ tự ngược lúc nhập. */
    List<ListingImportRow> findByBatchIdAndStatusInAndIdLessThanOrderByIdDesc(
        Long batchId,
        Collection<ListingImportRow.Status> statuses,
        Long id,
        Pageable pageable
    );
}
