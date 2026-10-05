package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.BlogCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the BlogCategory entity.
 */
@Repository
public interface BlogCategoryRepository extends JpaRepository<BlogCategory, Long>, JpaSpecificationExecutor<BlogCategory> {
    default Optional<BlogCategory> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<BlogCategory> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<BlogCategory> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select blogCategory from BlogCategory blogCategory left join fetch blogCategory.parent",
        countQuery = "select count(blogCategory) from BlogCategory blogCategory"
    )
    Page<BlogCategory> findAllWithToOneRelationships(Pageable pageable);

    @Query("select blogCategory from BlogCategory blogCategory left join fetch blogCategory.parent")
    List<BlogCategory> findAllWithToOneRelationships();

    @Query("select blogCategory from BlogCategory blogCategory left join fetch blogCategory.parent where blogCategory.id =:id")
    Optional<BlogCategory> findOneWithToOneRelationships(@Param("id") Long id);

    /**
     * Sửa tay: [id, id danh mục cha, tên] của mọi danh mục (bảng nhỏ), để điền danh mục cha và tên khi index bài viết vào Elasticsearch.
     */
    @Query("select c.id, p.id, c.name from BlogCategory c left join c.parent p")
    List<Object[]> findAllTreeRows();
}
