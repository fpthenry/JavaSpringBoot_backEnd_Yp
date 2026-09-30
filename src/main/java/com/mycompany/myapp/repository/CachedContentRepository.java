package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.CachedContent;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the CachedContent entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CachedContentRepository extends JpaRepository<CachedContent, Long> {}
