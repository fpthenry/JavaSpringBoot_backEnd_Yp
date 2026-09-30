package com.mycompany.myapp.service;

import com.mycompany.myapp.service.dto.GalleryDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.mycompany.myapp.domain.Gallery}.
 */
public interface GalleryService {
    /**
     * Save a gallery.
     *
     * @param galleryDTO the entity to save.
     * @return the persisted entity.
     */
    GalleryDTO save(GalleryDTO galleryDTO);

    /**
     * Updates a gallery.
     *
     * @param galleryDTO the entity to update.
     * @return the persisted entity.
     */
    GalleryDTO update(GalleryDTO galleryDTO);

    /**
     * Partially updates a gallery.
     *
     * @param galleryDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<GalleryDTO> partialUpdate(GalleryDTO galleryDTO);

    /**
     * Get all the galleries.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<GalleryDTO> findAll(Pageable pageable);

    /**
     * Get all the galleries with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<GalleryDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" gallery.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<GalleryDTO> findOne(Long id);

    /**
     * Delete the "id" gallery.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);

    /**
     * Search for the gallery corresponding to the query.
     *
     * @param query the query of the search.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<GalleryDTO> search(String query, Pageable pageable);
}
