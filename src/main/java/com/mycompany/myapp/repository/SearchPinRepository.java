package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.SearchPin;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Viết tay: doanh nghiệp ghim lên đầu kết quả tìm kiếm theo từ khóa. */
@Repository
public interface SearchPinRepository extends JpaRepository<SearchPin, Long> {
    List<SearchPin> findAllByOrderByPositionAscIdAsc();
}
