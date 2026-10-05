package com.mycompany.myapp.service;

import com.mycompany.myapp.service.dto.GalleryImageDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.mycompany.myapp.domain.GalleryImage}.
 */
public interface GalleryImageService {
    /**
     * Save a galleryImage.
     *
     * @param galleryImageDTO the entity to save.
     * @return the persisted entity.
     */
    GalleryImageDTO save(GalleryImageDTO galleryImageDTO);

    /**
     * Updates a galleryImage.
     *
     * @param galleryImageDTO the entity to update.
     * @return the persisted entity.
     */
    GalleryImageDTO update(GalleryImageDTO galleryImageDTO);

    /**
     * Partially updates a galleryImage.
     *
     * @param galleryImageDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<GalleryImageDTO> partialUpdate(GalleryImageDTO galleryImageDTO);

    /**
     * Get all the galleryImages with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<GalleryImageDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" galleryImage.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<GalleryImageDTO> findOne(Long id);

    /**
     * Delete the "id" galleryImage.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
