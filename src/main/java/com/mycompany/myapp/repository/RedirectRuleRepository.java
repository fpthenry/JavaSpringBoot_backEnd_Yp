package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.RedirectRule;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the RedirectRule entity.
 */
@SuppressWarnings("unused")
@Repository
public interface RedirectRuleRepository extends JpaRepository<RedirectRule, Long>, JpaSpecificationExecutor<RedirectRule> {}
