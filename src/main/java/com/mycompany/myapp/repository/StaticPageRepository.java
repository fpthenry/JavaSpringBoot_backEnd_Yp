package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.StaticPage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the StaticPage entity.
 */
@Repository
public interface StaticPageRepository extends JpaRepository<StaticPage, Long> {
    @Query("select staticPage from StaticPage staticPage where staticPage.author.login = ?#{authentication.name}")
    List<StaticPage> findByAuthorIsCurrentUser();

    default Optional<StaticPage> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<StaticPage> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<StaticPage> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select staticPage from StaticPage staticPage left join fetch staticPage.author left join fetch staticPage.parent",
        countQuery = "select count(staticPage) from StaticPage staticPage"
    )
    Page<StaticPage> findAllWithToOneRelationships(Pageable pageable);

    @Query("select staticPage from StaticPage staticPage left join fetch staticPage.author left join fetch staticPage.parent")
    List<StaticPage> findAllWithToOneRelationships();

    @Query(
        "select staticPage from StaticPage staticPage left join fetch staticPage.author left join fetch staticPage.parent where staticPage.id =:id"
    )
    Optional<StaticPage> findOneWithToOneRelationships(@Param("id") Long id);
}
