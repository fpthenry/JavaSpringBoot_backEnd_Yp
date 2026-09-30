package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.ListingImageRepository;
import com.mycompany.myapp.service.ListingImageQueryService;
import com.mycompany.myapp.service.ListingImageService;
import com.mycompany.myapp.service.criteria.ListingImageCriteria;
import com.mycompany.myapp.service.dto.ListingImageDTO;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import com.mycompany.myapp.web.rest.errors.ElasticsearchExceptionMapper;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.ListingImage}.
 */
@RestController
@RequestMapping("/api/listing-images")
public class ListingImageResource {

    private static final Logger LOG = LoggerFactory.getLogger(ListingImageResource.class);

    private static final String ENTITY_NAME = "listingImage";

    @Value("${jhipster.clientApp.name:javaSpringBootBackEnd}")
    private String applicationName;

    private final ListingImageService listingImageService;

    private final ListingImageRepository listingImageRepository;

    private final ListingImageQueryService listingImageQueryService;

    public ListingImageResource(
        ListingImageService listingImageService,
        ListingImageRepository listingImageRepository,
        ListingImageQueryService listingImageQueryService
    ) {
        this.listingImageService = listingImageService;
        this.listingImageRepository = listingImageRepository;
        this.listingImageQueryService = listingImageQueryService;
    }

    /**
     * {@code POST  /listing-images} : Create a new listingImage.
     *
     * @param listingImageDTO the listingImageDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new listingImageDTO, or with status {@code 400 (Bad Request)} if the listingImage has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ListingImageDTO> createListingImage(@Valid @RequestBody ListingImageDTO listingImageDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save ListingImage : {}", listingImageDTO);
        if (listingImageDTO.getId() != null) {
            throw new BadRequestAlertException("A new listingImage cannot already have an ID", ENTITY_NAME, "idexists");
        }
        listingImageDTO = listingImageService.save(listingImageDTO);
        return ResponseEntity.created(new URI("/api/listing-images/" + listingImageDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, listingImageDTO.getId().toString()))
            .body(listingImageDTO);
    }

    /**
     * {@code PUT  /listing-images/:id} : Updates an existing listingImage.
     *
     * @param id the id of the listingImageDTO to save.
     * @param listingImageDTO the listingImageDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated listingImageDTO,
     * or with status {@code 400 (Bad Request)} if the listingImageDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the listingImageDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ListingImageDTO> updateListingImage(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody ListingImageDTO listingImageDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update ListingImage : {}, {}", id, listingImageDTO);
        if (listingImageDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, listingImageDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!listingImageRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        listingImageDTO = listingImageService.update(listingImageDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, listingImageDTO.getId().toString()))
            .body(listingImageDTO);
    }

    /**
     * {@code PATCH  /listing-images/:id} : Partial updates given fields of an existing listingImage, field will ignore if it is null
     *
     * @param id the id of the listingImageDTO to save.
     * @param listingImageDTO the listingImageDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated listingImageDTO,
     * or with status {@code 400 (Bad Request)} if the listingImageDTO is not valid,
     * or with status {@code 404 (Not Found)} if the listingImageDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the listingImageDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ListingImageDTO> partialUpdateListingImage(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody ListingImageDTO listingImageDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update ListingImage partially : {}, {}", id, listingImageDTO);
        if (listingImageDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, listingImageDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!listingImageRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ListingImageDTO> result = listingImageService.partialUpdate(listingImageDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, listingImageDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /listing-images} : get all the Listing Images.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Listing Images in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ListingImageDTO>> getAllListingImages(
        ListingImageCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get ListingImages by criteria: {}", criteria);

        Page<ListingImageDTO> page = listingImageQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /listing-images/count} : count all the listingImages.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countListingImages(ListingImageCriteria criteria) {
        LOG.debug("REST request to count ListingImages by criteria: {}", criteria);
        return ResponseEntity.ok().body(listingImageQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /listing-images/:id} : get the "id" listingImage.
     *
     * @param id the id of the listingImageDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the listingImageDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ListingImageDTO> getListingImage(@PathVariable("id") Long id) {
        LOG.debug("REST request to get ListingImage : {}", id);
        Optional<ListingImageDTO> listingImageDTO = listingImageService.findOne(id);
        return ResponseUtil.wrapOrNotFound(listingImageDTO);
    }

    /**
     * {@code DELETE  /listing-images/:id} : delete the "id" listingImage.
     *
     * @param id the id of the listingImageDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteListingImage(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete ListingImage : {}", id);
        listingImageService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code SEARCH  /listing-images/_search?query=:query} : search for the listingImage corresponding
     * to the query.
     *
     * @param query the query of the listingImage search.
     * @param pageable the pagination information.
     * @return the result of the search.
     */
    @GetMapping("/_search")
    public ResponseEntity<List<ListingImageDTO>> searchListingImages(
        @RequestParam("query") String query,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to search for a page of ListingImages for query {}", query);
        try {
            Page<ListingImageDTO> page = listingImageService.search(query, pageable);
            HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
            return ResponseEntity.ok().headers(headers).body(page.getContent());
        } catch (RuntimeException e) {
            throw ElasticsearchExceptionMapper.mapException(e);
        }
    }
}
