package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.CachedContentRepository;
import com.mycompany.myapp.service.CachedContentService;
import com.mycompany.myapp.service.dto.CachedContentDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.mycompany.myapp.domain.CachedContent}.
 */
@RestController
@RequestMapping("/api/cached-contents")
public class CachedContentResource {

    private static final Logger LOG = LoggerFactory.getLogger(CachedContentResource.class);

    private static final String ENTITY_NAME = "cachedContent";

    @Value("${jhipster.clientApp.name:javaSpringBootBackEnd}")
    private String applicationName;

    private final CachedContentService cachedContentService;

    private final CachedContentRepository cachedContentRepository;

    public CachedContentResource(CachedContentService cachedContentService, CachedContentRepository cachedContentRepository) {
        this.cachedContentService = cachedContentService;
        this.cachedContentRepository = cachedContentRepository;
    }

    /**
     * {@code POST  /cached-contents} : Create a new cachedContent.
     *
     * @param cachedContentDTO the cachedContentDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new cachedContentDTO, or with status {@code 400 (Bad Request)} if the cachedContent has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<CachedContentDTO> createCachedContent(@Valid @RequestBody CachedContentDTO cachedContentDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save CachedContent : {}", cachedContentDTO);
        if (cachedContentDTO.getId() != null) {
            throw new BadRequestAlertException("A new cachedContent cannot already have an ID", ENTITY_NAME, "idexists");
        }
        cachedContentDTO = cachedContentService.save(cachedContentDTO);
        return ResponseEntity.created(new URI("/api/cached-contents/" + cachedContentDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, cachedContentDTO.getId().toString()))
            .body(cachedContentDTO);
    }

    /**
     * {@code PUT  /cached-contents/:id} : Updates an existing cachedContent.
     *
     * @param id the id of the cachedContentDTO to save.
     * @param cachedContentDTO the cachedContentDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated cachedContentDTO,
     * or with status {@code 400 (Bad Request)} if the cachedContentDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the cachedContentDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CachedContentDTO> updateCachedContent(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody CachedContentDTO cachedContentDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update CachedContent : {}, {}", id, cachedContentDTO);
        if (cachedContentDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, cachedContentDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!cachedContentRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        cachedContentDTO = cachedContentService.update(cachedContentDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, cachedContentDTO.getId().toString()))
            .body(cachedContentDTO);
    }

    /**
     * {@code PATCH  /cached-contents/:id} : Partial updates given fields of an existing cachedContent, field will ignore if it is null
     *
     * @param id the id of the cachedContentDTO to save.
     * @param cachedContentDTO the cachedContentDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated cachedContentDTO,
     * or with status {@code 400 (Bad Request)} if the cachedContentDTO is not valid,
     * or with status {@code 404 (Not Found)} if the cachedContentDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the cachedContentDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<CachedContentDTO> partialUpdateCachedContent(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody CachedContentDTO cachedContentDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update CachedContent partially : {}, {}", id, cachedContentDTO);
        if (cachedContentDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, cachedContentDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!cachedContentRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<CachedContentDTO> result = cachedContentService.partialUpdate(cachedContentDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, cachedContentDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /cached-contents} : get all the Cached Contents.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Cached Contents in body.
     */
    @GetMapping("")
    public List<CachedContentDTO> getAllCachedContents() {
        LOG.debug("REST request to get all CachedContents");
        return cachedContentService.findAll();
    }

    /**
     * {@code GET  /cached-contents/:id} : get the "id" cachedContent.
     *
     * @param id the id of the cachedContentDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the cachedContentDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CachedContentDTO> getCachedContent(@PathVariable("id") Long id) {
        LOG.debug("REST request to get CachedContent : {}", id);
        Optional<CachedContentDTO> cachedContentDTO = cachedContentService.findOne(id);
        return ResponseUtil.wrapOrNotFound(cachedContentDTO);
    }

    /**
     * {@code DELETE  /cached-contents/:id} : delete the "id" cachedContent.
     *
     * @param id the id of the cachedContentDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCachedContent(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete CachedContent : {}", id);
        cachedContentService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code SEARCH  /cached-contents/_search?query=:query} : search for the cachedContent corresponding
     * to the query.
     *
     * @param query the query of the cachedContent search.
     * @return the result of the search.
     */
    @GetMapping("/_search")
    public List<CachedContentDTO> searchCachedContents(@RequestParam("query") String query) {
        LOG.debug("REST request to search CachedContents for query {}", query);
        try {
            return cachedContentService.search(query);
        } catch (RuntimeException e) {
            throw ElasticsearchExceptionMapper.mapException(e);
        }
    }
}
