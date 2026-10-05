package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.BlogPost;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the BlogPost entity.
 *
 * When extending this class, extend BlogPostRepositoryWithBagRelationships too.
 * For more information refer to https://github.com/jhipster/generator-jhipster/issues/17990.
 */
@Repository
public interface BlogPostRepository
    extends BlogPostRepositoryWithBagRelationships, JpaRepository<BlogPost, Long>, JpaSpecificationExecutor<BlogPost>
{
    default Optional<BlogPost> findOneWithEagerRelationships(Long id) {
        return this.fetchBagRelationships(this.findById(id));
    }

    default List<BlogPost> findAllWithEagerRelationships() {
        return this.fetchBagRelationships(this.findAll());
    }

    default Page<BlogPost> findAllWithEagerRelationships(Pageable pageable) {
        return this.fetchBagRelationships(this.findAll(pageable));
    }
}
