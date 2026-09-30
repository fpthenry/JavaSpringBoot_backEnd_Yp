package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Article;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Article entity.
 *
 * When extending this class, extend ArticleRepositoryWithBagRelationships too.
 * For more information refer to https://github.com/jhipster/generator-jhipster/issues/17990.
 */
@Repository
public interface ArticleRepository
    extends ArticleRepositoryWithBagRelationships, JpaRepository<Article, Long>, JpaSpecificationExecutor<Article>
{
    @Query("select article from Article article where article.author.login = ?#{authentication.name}")
    List<Article> findByAuthorIsCurrentUser();

    default Optional<Article> findOneWithEagerRelationships(Long id) {
        return this.fetchBagRelationships(this.findOneWithToOneRelationships(id));
    }

    default List<Article> findAllWithEagerRelationships() {
        return this.fetchBagRelationships(this.findAllWithToOneRelationships());
    }

    default Page<Article> findAllWithEagerRelationships(Pageable pageable) {
        return this.fetchBagRelationships(this.findAllWithToOneRelationships(pageable));
    }

    @Query(
        value = "select article from Article article left join fetch article.author",
        countQuery = "select count(article) from Article article"
    )
    Page<Article> findAllWithToOneRelationships(Pageable pageable);

    @Query("select article from Article article left join fetch article.author")
    List<Article> findAllWithToOneRelationships();

    @Query("select article from Article article left join fetch article.author where article.id =:id")
    Optional<Article> findOneWithToOneRelationships(@Param("id") Long id);
}
