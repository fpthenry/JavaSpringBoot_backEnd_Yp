package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Category;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Truy vấn chỉ đọc cho API công khai ngành nghề (FE Next.js).
 */
public interface CategoryPublicRepository extends org.springframework.data.repository.Repository<Category, Long> {
    /** [id, tên, slug, id cha, tên cha, số doanh nghiệp, mã ngành, cấp] của mọi ngành (VSIC 2025: 1.606 dòng). */
    @Query("select c.id, c.name, c.slug, p.id, p.name, c.listingCount, c.code, c.level from Category c left join c.parent p")
    List<Object[]> findAllRows();

    /**
     * Slug cũ (trước VSIC 2025) → slug ngành mới: [slug mới] (null nếu ngành cũ không có ngành tương ứng). Không có
     * dòng nào = không phải slug cũ.
     */
    @Query(
        value = "select c.slug from category_slug_redirect r left join category c on c.id = r.category_id where r.old_slug = :slug",
        nativeQuery = true
    )
    List<Object> findRedirectSlug(@Param("slug") String oldSlug);

    /** Mô tả một ngành (cột dài, không đọc trong {@link #findAllRows()}). */
    @Query("select c.description from Category c where c.id = :id")
    String findDescription(@Param("id") Long id);
}
