package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Category entity.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {
    default Optional<Category> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Category> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Category> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select category from Category category left join fetch category.parent",
        countQuery = "select count(category) from Category category"
    )
    Page<Category> findAllWithToOneRelationships(Pageable pageable);

    @Query("select category from Category category left join fetch category.parent")
    List<Category> findAllWithToOneRelationships();

    @Query("select category from Category category left join fetch category.parent where category.id =:id")
    Optional<Category> findOneWithToOneRelationships(@Param("id") Long id);

    /**
     * Sửa tay: id của ngành và mọi cấp con bên dưới (cây ngành nghề sâu tối đa 4 cấp).
     */
    @Query(
        "select category.id from Category category left join category.parent parent left join parent.parent grandParent " +
            "left join grandParent.parent greatGrandParent " +
            "where category.id = :id or parent.id = :id or grandParent.id = :id or greatGrandParent.id = :id"
    )
    List<Long> findSubtreeIds(@Param("id") Long id);
}
