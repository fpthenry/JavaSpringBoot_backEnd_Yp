package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Category;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Truy vấn chỉ đọc cho API công khai ngành nghề (FE Next.js).
 */
public interface CategoryPublicRepository extends org.springframework.data.repository.Repository<Category, Long> {
    /** [id, tên, slug, id cha, tên cha, số doanh nghiệp] của mọi ngành (~2.400 dòng). */
    @Query("select c.id, c.name, c.slug, p.id, p.name, c.listingCount from Category c left join c.parent p")
    List<Object[]> findAllRows();

    /** Mô tả một ngành (cột dài, không đọc trong {@link #findAllRows()}). */
    @Query("select c.description from Category c where c.id = :id")
    String findDescription(@Param("id") Long id);
}
