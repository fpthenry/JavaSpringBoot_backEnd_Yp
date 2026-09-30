package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.StaticPageRepository;
import com.mycompany.myapp.service.StaticPageService;
import com.mycompany.myapp.service.dto.StaticPageDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.StaticPage}.
 */
@RestController
@RequestMapping("/api/static-pages")
public class StaticPageResource {

    private static final Logger LOG = LoggerFactory.getLogger(StaticPageResource.class);

    private static final String ENTITY_NAME = "staticPage";

    @Value("${jhipster.clientApp.name:javaSpringBootBackEnd}")
    private String applicationName;

    private final StaticPageService staticPageService;

    private final StaticPageRepository staticPageRepository;

    public StaticPageResource(StaticPageService staticPageService, StaticPageRepository staticPageRepository) {
        this.staticPageService = staticPageService;
        this.staticPageRepository = staticPageRepository;
    }

    /**
     * {@code POST  /static-pages} : Create a new staticPage.
     *
     * @param staticPageDTO the staticPageDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new staticPageDTO, or with status {@code 400 (Bad Request)} if the staticPage has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<StaticPageDTO> createStaticPage(@Valid @RequestBody StaticPageDTO staticPageDTO) throws URISyntaxException {
        LOG.debug("REST request to save StaticPage : {}", staticPageDTO);
        if (staticPageDTO.getId() != null) {
            throw new BadRequestAlertException("A new staticPage cannot already have an ID", ENTITY_NAME, "idexists");
        }
        staticPageDTO = staticPageService.save(staticPageDTO);
        return ResponseEntity.created(new URI("/api/static-pages/" + staticPageDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, staticPageDTO.getId().toString()))
            .body(staticPageDTO);
    }

    /**
     * {@code PUT  /static-pages/:id} : Updates an existing staticPage.
     *
     * @param id the id of the staticPageDTO to save.
     * @param staticPageDTO the staticPageDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated staticPageDTO,
     * or with status {@code 400 (Bad Request)} if the staticPageDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the staticPageDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<StaticPageDTO> updateStaticPage(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody StaticPageDTO staticPageDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update StaticPage : {}, {}", id, staticPageDTO);
        if (staticPageDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, staticPageDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!staticPageRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        staticPageDTO = staticPageService.update(staticPageDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, staticPageDTO.getId().toString()))
            .body(staticPageDTO);
    }

    /**
     * {@code PATCH  /static-pages/:id} : Partial updates given fields of an existing staticPage, field will ignore if it is null
     *
     * @param id the id of the staticPageDTO to save.
     * @param staticPageDTO the staticPageDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated staticPageDTO,
     * or with status {@code 400 (Bad Request)} if the staticPageDTO is not valid,
     * or with status {@code 404 (Not Found)} if the staticPageDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the staticPageDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<StaticPageDTO> partialUpdateStaticPage(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody StaticPageDTO staticPageDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update StaticPage partially : {}, {}", id, staticPageDTO);
        if (staticPageDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, staticPageDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!staticPageRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<StaticPageDTO> result = staticPageService.partialUpdate(staticPageDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, staticPageDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /static-pages} : get all the Static Pages.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Static Pages in body.
     */
    @GetMapping("")
    public List<StaticPageDTO> getAllStaticPages(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all StaticPages");
        if (eagerload) {
            return staticPageService.findAllWithEagerRelationships();
        } else {
            return staticPageService.findAll();
        }
    }

    /**
     * {@code GET  /static-pages/:id} : get the "id" staticPage.
     *
     * @param id the id of the staticPageDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the staticPageDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<StaticPageDTO> getStaticPage(@PathVariable("id") Long id) {
        LOG.debug("REST request to get StaticPage : {}", id);
        Optional<StaticPageDTO> staticPageDTO = staticPageService.findOne(id);
        return ResponseUtil.wrapOrNotFound(staticPageDTO);
    }

    /**
     * {@code DELETE  /static-pages/:id} : delete the "id" staticPage.
     *
     * @param id the id of the staticPageDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStaticPage(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete StaticPage : {}", id);
        staticPageService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code SEARCH  /static-pages/_search?query=:query} : search for the staticPage corresponding
     * to the query.
     *
     * @param query the query of the staticPage search.
     * @return the result of the search.
     */
    @GetMapping("/_search")
    public List<StaticPageDTO> searchStaticPages(@RequestParam("query") String query) {
        LOG.debug("REST request to search StaticPages for query {}", query);
        try {
            return staticPageService.search(query);
        } catch (RuntimeException e) {
            throw ElasticsearchExceptionMapper.mapException(e);
        }
    }
}
