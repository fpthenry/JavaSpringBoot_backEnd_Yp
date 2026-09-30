package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.ArticleCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ArticleCategory entity.
 */
@Repository
public interface ArticleCategoryRepository extends JpaRepository<ArticleCategory, Long> {
    default Optional<ArticleCategory> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<ArticleCategory> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<ArticleCategory> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select articleCategory from ArticleCategory articleCategory left join fetch articleCategory.parent",
        countQuery = "select count(articleCategory) from ArticleCategory articleCategory"
    )
    Page<ArticleCategory> findAllWithToOneRelationships(Pageable pageable);

    @Query("select articleCategory from ArticleCategory articleCategory left join fetch articleCategory.parent")
    List<ArticleCategory> findAllWithToOneRelationships();

    @Query(
        "select articleCategory from ArticleCategory articleCategory left join fetch articleCategory.parent where articleCategory.id =:id"
    )
    Optional<ArticleCategory> findOneWithToOneRelationships(@Param("id") Long id);
}
