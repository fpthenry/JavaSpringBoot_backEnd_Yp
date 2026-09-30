package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.RedirectRuleRepository;
import com.mycompany.myapp.service.RedirectRuleQueryService;
import com.mycompany.myapp.service.RedirectRuleService;
import com.mycompany.myapp.service.criteria.RedirectRuleCriteria;
import com.mycompany.myapp.service.dto.RedirectRuleDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.RedirectRule}.
 */
@RestController
@RequestMapping("/api/redirect-rules")
public class RedirectRuleResource {

    private static final Logger LOG = LoggerFactory.getLogger(RedirectRuleResource.class);

    private static final String ENTITY_NAME = "redirectRule";

    @Value("${jhipster.clientApp.name:javaSpringBootBackEnd}")
    private String applicationName;

    private final RedirectRuleService redirectRuleService;

    private final RedirectRuleRepository redirectRuleRepository;

    private final RedirectRuleQueryService redirectRuleQueryService;

    public RedirectRuleResource(
        RedirectRuleService redirectRuleService,
        RedirectRuleRepository redirectRuleRepository,
        RedirectRuleQueryService redirectRuleQueryService
    ) {
        this.redirectRuleService = redirectRuleService;
        this.redirectRuleRepository = redirectRuleRepository;
        this.redirectRuleQueryService = redirectRuleQueryService;
    }

    /**
     * {@code POST  /redirect-rules} : Create a new redirectRule.
     *
     * @param redirectRuleDTO the redirectRuleDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new redirectRuleDTO, or with status {@code 400 (Bad Request)} if the redirectRule has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<RedirectRuleDTO> createRedirectRule(@Valid @RequestBody RedirectRuleDTO redirectRuleDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save RedirectRule : {}", redirectRuleDTO);
        if (redirectRuleDTO.getId() != null) {
            throw new BadRequestAlertException("A new redirectRule cannot already have an ID", ENTITY_NAME, "idexists");
        }
        redirectRuleDTO = redirectRuleService.save(redirectRuleDTO);
        return ResponseEntity.created(new URI("/api/redirect-rules/" + redirectRuleDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, redirectRuleDTO.getId().toString()))
            .body(redirectRuleDTO);
    }

    /**
     * {@code PUT  /redirect-rules/:id} : Updates an existing redirectRule.
     *
     * @param id the id of the redirectRuleDTO to save.
     * @param redirectRuleDTO the redirectRuleDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated redirectRuleDTO,
     * or with status {@code 400 (Bad Request)} if the redirectRuleDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the redirectRuleDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RedirectRuleDTO> updateRedirectRule(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody RedirectRuleDTO redirectRuleDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update RedirectRule : {}, {}", id, redirectRuleDTO);
        if (redirectRuleDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, redirectRuleDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!redirectRuleRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        redirectRuleDTO = redirectRuleService.update(redirectRuleDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, redirectRuleDTO.getId().toString()))
            .body(redirectRuleDTO);
    }

    /**
     * {@code PATCH  /redirect-rules/:id} : Partial updates given fields of an existing redirectRule, field will ignore if it is null
     *
     * @param id the id of the redirectRuleDTO to save.
     * @param redirectRuleDTO the redirectRuleDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated redirectRuleDTO,
     * or with status {@code 400 (Bad Request)} if the redirectRuleDTO is not valid,
     * or with status {@code 404 (Not Found)} if the redirectRuleDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the redirectRuleDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<RedirectRuleDTO> partialUpdateRedirectRule(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody RedirectRuleDTO redirectRuleDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update RedirectRule partially : {}, {}", id, redirectRuleDTO);
        if (redirectRuleDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, redirectRuleDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!redirectRuleRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<RedirectRuleDTO> result = redirectRuleService.partialUpdate(redirectRuleDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, redirectRuleDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /redirect-rules} : get all the Redirect Rules.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Redirect Rules in body.
     */
    @GetMapping("")
    public ResponseEntity<List<RedirectRuleDTO>> getAllRedirectRules(
        RedirectRuleCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get RedirectRules by criteria: {}", criteria);

        Page<RedirectRuleDTO> page = redirectRuleQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /redirect-rules/count} : count all the redirectRules.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countRedirectRules(RedirectRuleCriteria criteria) {
        LOG.debug("REST request to count RedirectRules by criteria: {}", criteria);
        return ResponseEntity.ok().body(redirectRuleQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /redirect-rules/:id} : get the "id" redirectRule.
     *
     * @param id the id of the redirectRuleDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the redirectRuleDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RedirectRuleDTO> getRedirectRule(@PathVariable("id") Long id) {
        LOG.debug("REST request to get RedirectRule : {}", id);
        Optional<RedirectRuleDTO> redirectRuleDTO = redirectRuleService.findOne(id);
        return ResponseUtil.wrapOrNotFound(redirectRuleDTO);
    }

    /**
     * {@code DELETE  /redirect-rules/:id} : delete the "id" redirectRule.
     *
     * @param id the id of the redirectRuleDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRedirectRule(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete RedirectRule : {}", id);
        redirectRuleService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code SEARCH  /redirect-rules/_search?query=:query} : search for the redirectRule corresponding
     * to the query.
     *
     * @param query the query of the redirectRule search.
     * @param pageable the pagination information.
     * @return the result of the search.
     */
    @GetMapping("/_search")
    public ResponseEntity<List<RedirectRuleDTO>> searchRedirectRules(
        @RequestParam("query") String query,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to search for a page of RedirectRules for query {}", query);
        try {
            Page<RedirectRuleDTO> page = redirectRuleService.search(query, pageable);
            HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
            return ResponseEntity.ok().headers(headers).body(page.getContent());
        } catch (RuntimeException e) {
            throw ElasticsearchExceptionMapper.mapException(e);
        }
    }
}
