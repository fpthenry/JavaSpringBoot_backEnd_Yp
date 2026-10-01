package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.ListingRepository;
import com.mycompany.myapp.service.ListingQueryService;
import com.mycompany.myapp.service.ListingService;
import com.mycompany.myapp.service.criteria.ListingCriteria;
import com.mycompany.myapp.service.dto.ListingDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.Listing}.
 */
@RestController
@RequestMapping("/api/listings")
public class ListingResource {

    private static final Logger LOG = LoggerFactory.getLogger(ListingResource.class);

    private static final String ENTITY_NAME = "listing";

    @Value("${jhipster.clientApp.name:javaSpringBootBackEnd}")
    private String applicationName;

    private final ListingService listingService;

    private final ListingRepository listingRepository;

    private final ListingQueryService listingQueryService;

    public ListingResource(ListingService listingService, ListingRepository listingRepository, ListingQueryService listingQueryService) {
        this.listingService = listingService;
        this.listingRepository = listingRepository;
        this.listingQueryService = listingQueryService;
    }

    /**
     * {@code POST  /listings} : Create a new listing.
     *
     * @param listingDTO the listingDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new listingDTO, or with status {@code 400 (Bad Request)} if the listing has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ListingDTO> createListing(@Valid @RequestBody ListingDTO listingDTO) throws URISyntaxException {
        LOG.debug("REST request to save Listing : {}", listingDTO);
        if (listingDTO.getId() != null) {
            throw new BadRequestAlertException("A new listing cannot already have an ID", ENTITY_NAME, "idexists");
        }
        listingDTO = listingService.save(listingDTO);
        return ResponseEntity.created(new URI("/api/listings/" + listingDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, listingDTO.getId().toString()))
            .body(listingDTO);
    }

    /**
     * {@code PUT  /listings/:id} : Updates an existing listing.
     *
     * @param id the id of the listingDTO to save.
     * @param listingDTO the listingDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated listingDTO,
     * or with status {@code 400 (Bad Request)} if the listingDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the listingDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ListingDTO> updateListing(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody ListingDTO listingDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Listing : {}, {}", id, listingDTO);
        if (listingDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, listingDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!listingRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        listingDTO = listingService.update(listingDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, listingDTO.getId().toString()))
            .body(listingDTO);
    }

    /**
     * {@code PATCH  /listings/:id} : Partial updates given fields of an existing listing, field will ignore if it is null
     *
     * @param id the id of the listingDTO to save.
     * @param listingDTO the listingDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated listingDTO,
     * or with status {@code 400 (Bad Request)} if the listingDTO is not valid,
     * or with status {@code 404 (Not Found)} if the listingDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the listingDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ListingDTO> partialUpdateListing(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody ListingDTO listingDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Listing partially : {}, {}", id, listingDTO);
        if (listingDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, listingDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!listingRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ListingDTO> result = listingService.partialUpdate(listingDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, listingDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /listings} : get all the Listings.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Listings in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ListingDTO>> getAllListings(
        ListingCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Listings by criteria: {}", criteria);

        Page<ListingDTO> page = listingQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /listings/count} : count all the listings.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countListings(ListingCriteria criteria) {
        LOG.debug("REST request to count Listings by criteria: {}", criteria);
        return ResponseEntity.ok().body(listingQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /listings/:id} : get the "id" listing.
     *
     * @param id the id of the listingDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the listingDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ListingDTO> getListing(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Listing : {}", id);
        Optional<ListingDTO> listingDTO = listingService.findOne(id);
        return ResponseUtil.wrapOrNotFound(listingDTO);
    }

    /**
     * {@code DELETE  /listings/:id} : delete the "id" listing.
     *
     * @param id the id of the listingDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteListing(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Listing : {}", id);
        listingService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code SEARCH  /listings/_search?query=:query} : search for the listing corresponding
     * to the query.
     *
     * @param query the query of the listing search.
     * @param pageable the pagination information.
     * @return the result of the search.
     */
    @GetMapping("/_search")
    public ResponseEntity<List<ListingDTO>> searchListings(
        @RequestParam("query") String query,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to search for a page of Listings for query {}", query);
        try {
            Page<ListingDTO> page = listingService.search(query, pageable);
            HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
            return ResponseEntity.ok().headers(headers).body(page.getContent());
        } catch (RuntimeException e) {
            throw ElasticsearchExceptionMapper.mapException(e);
        }
    }
}
