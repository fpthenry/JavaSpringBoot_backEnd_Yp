package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Location;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Location entity.
 */
@Repository
public interface LocationRepository extends JpaRepository<Location, Long>, JpaSpecificationExecutor<Location> {
    default Optional<Location> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Location> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Location> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select location from Location location left join fetch location.parent",
        countQuery = "select count(location) from Location location"
    )
    Page<Location> findAllWithToOneRelationships(Pageable pageable);

    @Query("select location from Location location left join fetch location.parent")
    List<Location> findAllWithToOneRelationships();

    @Query("select location from Location location left join fetch location.parent where location.id =:id")
    Optional<Location> findOneWithToOneRelationships(@Param("id") Long id);

    /**
     * Sửa tay: id của địa phương và mọi cấp con bên dưới (tỉnh -> quận/huyện -> phường/xã, tối đa 3 cấp).
     */
    @Query(
        "select location.id from Location location left join location.parent parent left join parent.parent grandParent " +
            "where location.id = :id or parent.id = :id or grandParent.id = :id"
    )
    List<Long> findSubtreeIds(@Param("id") Long id);
}
