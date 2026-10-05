package com.mycompany.myapp.service;

import com.mycompany.myapp.service.dto.GalleryDTO;
import java.util.Optional;

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
}
