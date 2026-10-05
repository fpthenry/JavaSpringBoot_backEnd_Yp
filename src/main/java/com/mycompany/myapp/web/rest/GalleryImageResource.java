package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.GalleryImageRepository;
import com.mycompany.myapp.service.GalleryImageQueryService;
import com.mycompany.myapp.service.GalleryImageService;
import com.mycompany.myapp.service.criteria.GalleryImageCriteria;
import com.mycompany.myapp.service.dto.GalleryImageDTO;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.mycompany.myapp.domain.GalleryImage}.
 */
@RestController
@RequestMapping("/api/gallery-images")
public class GalleryImageResource {

    private static final Logger LOG = LoggerFactory.getLogger(GalleryImageResource.class);

    private static final String ENTITY_NAME = "galleryImage";

    @Value("${jhipster.clientApp.name:javaSpringBootBackEnd}")
    private String applicationName;

    private final GalleryImageService galleryImageService;

    private final GalleryImageRepository galleryImageRepository;

    private final GalleryImageQueryService galleryImageQueryService;

    public GalleryImageResource(
        GalleryImageService galleryImageService,
        GalleryImageRepository galleryImageRepository,
        GalleryImageQueryService galleryImageQueryService
    ) {
        this.galleryImageService = galleryImageService;
        this.galleryImageRepository = galleryImageRepository;
        this.galleryImageQueryService = galleryImageQueryService;
    }

    /**
     * {@code POST  /gallery-images} : Create a new galleryImage.
     *
     * @param galleryImageDTO the galleryImageDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new galleryImageDTO, or with status {@code 400 (Bad Request)} if the galleryImage has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<GalleryImageDTO> createGalleryImage(@Valid @RequestBody GalleryImageDTO galleryImageDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save GalleryImage : {}", galleryImageDTO);
        if (galleryImageDTO.getId() != null) {
            throw new BadRequestAlertException("A new galleryImage cannot already have an ID", ENTITY_NAME, "idexists");
        }
        galleryImageDTO = galleryImageService.save(galleryImageDTO);
        return ResponseEntity.created(new URI("/api/gallery-images/" + galleryImageDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, galleryImageDTO.getId().toString()))
            .body(galleryImageDTO);
    }

    /**
     * {@code PUT  /gallery-images/:id} : Updates an existing galleryImage.
     *
     * @param id the id of the galleryImageDTO to save.
     * @param galleryImageDTO the galleryImageDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated galleryImageDTO,
     * or with status {@code 400 (Bad Request)} if the galleryImageDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the galleryImageDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<GalleryImageDTO> updateGalleryImage(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody GalleryImageDTO galleryImageDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update GalleryImage : {}, {}", id, galleryImageDTO);
        if (galleryImageDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, galleryImageDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!galleryImageRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        galleryImageDTO = galleryImageService.update(galleryImageDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, galleryImageDTO.getId().toString()))
            .body(galleryImageDTO);
    }

    /**
     * {@code PATCH  /gallery-images/:id} : Partial updates given fields of an existing galleryImage, field will ignore if it is null
     *
     * @param id the id of the galleryImageDTO to save.
     * @param galleryImageDTO the galleryImageDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated galleryImageDTO,
     * or with status {@code 400 (Bad Request)} if the galleryImageDTO is not valid,
     * or with status {@code 404 (Not Found)} if the galleryImageDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the galleryImageDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<GalleryImageDTO> partialUpdateGalleryImage(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody GalleryImageDTO galleryImageDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update GalleryImage partially : {}, {}", id, galleryImageDTO);
        if (galleryImageDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, galleryImageDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!galleryImageRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<GalleryImageDTO> result = galleryImageService.partialUpdate(galleryImageDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, galleryImageDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /gallery-images} : get all the Gallery Images.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Gallery Images in body.
     */
    @GetMapping("")
    public ResponseEntity<List<GalleryImageDTO>> getAllGalleryImages(
        GalleryImageCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get GalleryImages by criteria: {}", criteria);

        Page<GalleryImageDTO> page = galleryImageQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /gallery-images/count} : count all the galleryImages.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countGalleryImages(GalleryImageCriteria criteria) {
        LOG.debug("REST request to count GalleryImages by criteria: {}", criteria);
        return ResponseEntity.ok().body(galleryImageQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /gallery-images/:id} : get the "id" galleryImage.
     *
     * @param id the id of the galleryImageDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the galleryImageDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<GalleryImageDTO> getGalleryImage(@PathVariable("id") Long id) {
        LOG.debug("REST request to get GalleryImage : {}", id);
        Optional<GalleryImageDTO> galleryImageDTO = galleryImageService.findOne(id);
        return ResponseUtil.wrapOrNotFound(galleryImageDTO);
    }

    /**
     * {@code DELETE  /gallery-images/:id} : delete the "id" galleryImage.
     *
     * @param id the id of the galleryImageDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGalleryImage(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete GalleryImage : {}", id);
        galleryImageService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
