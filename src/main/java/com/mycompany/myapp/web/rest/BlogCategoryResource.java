package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.BlogCategoryRepository;
import com.mycompany.myapp.service.BlogCategoryQueryService;
import com.mycompany.myapp.service.BlogCategoryService;
import com.mycompany.myapp.service.criteria.BlogCategoryCriteria;
import com.mycompany.myapp.service.dto.BlogCategoryDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.BlogCategory}.
 */
@RestController
@RequestMapping("/api/blog-categories")
public class BlogCategoryResource {

    private static final Logger LOG = LoggerFactory.getLogger(BlogCategoryResource.class);

    private static final String ENTITY_NAME = "blogCategory";

    @Value("${jhipster.clientApp.name:javaSpringBootBackEnd}")
    private String applicationName;

    private final BlogCategoryService blogCategoryService;

    private final BlogCategoryRepository blogCategoryRepository;

    private final BlogCategoryQueryService blogCategoryQueryService;

    public BlogCategoryResource(
        BlogCategoryService blogCategoryService,
        BlogCategoryRepository blogCategoryRepository,
        BlogCategoryQueryService blogCategoryQueryService
    ) {
        this.blogCategoryService = blogCategoryService;
        this.blogCategoryRepository = blogCategoryRepository;
        this.blogCategoryQueryService = blogCategoryQueryService;
    }

    /**
     * {@code POST  /blog-categories} : Create a new blogCategory.
     *
     * @param blogCategoryDTO the blogCategoryDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new blogCategoryDTO, or with status {@code 400 (Bad Request)} if the blogCategory has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<BlogCategoryDTO> createBlogCategory(@Valid @RequestBody BlogCategoryDTO blogCategoryDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save BlogCategory : {}", blogCategoryDTO);
        if (blogCategoryDTO.getId() != null) {
            throw new BadRequestAlertException("A new blogCategory cannot already have an ID", ENTITY_NAME, "idexists");
        }
        blogCategoryDTO = blogCategoryService.save(blogCategoryDTO);
        return ResponseEntity.created(new URI("/api/blog-categories/" + blogCategoryDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, blogCategoryDTO.getId().toString()))
            .body(blogCategoryDTO);
    }

    /**
     * {@code PUT  /blog-categories/:id} : Updates an existing blogCategory.
     *
     * @param id the id of the blogCategoryDTO to save.
     * @param blogCategoryDTO the blogCategoryDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated blogCategoryDTO,
     * or with status {@code 400 (Bad Request)} if the blogCategoryDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the blogCategoryDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<BlogCategoryDTO> updateBlogCategory(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody BlogCategoryDTO blogCategoryDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update BlogCategory : {}, {}", id, blogCategoryDTO);
        if (blogCategoryDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, blogCategoryDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!blogCategoryRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        blogCategoryDTO = blogCategoryService.update(blogCategoryDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, blogCategoryDTO.getId().toString()))
            .body(blogCategoryDTO);
    }

    /**
     * {@code PATCH  /blog-categories/:id} : Partial updates given fields of an existing blogCategory, field will ignore if it is null
     *
     * @param id the id of the blogCategoryDTO to save.
     * @param blogCategoryDTO the blogCategoryDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated blogCategoryDTO,
     * or with status {@code 400 (Bad Request)} if the blogCategoryDTO is not valid,
     * or with status {@code 404 (Not Found)} if the blogCategoryDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the blogCategoryDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<BlogCategoryDTO> partialUpdateBlogCategory(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody BlogCategoryDTO blogCategoryDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update BlogCategory partially : {}, {}", id, blogCategoryDTO);
        if (blogCategoryDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, blogCategoryDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!blogCategoryRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<BlogCategoryDTO> result = blogCategoryService.partialUpdate(blogCategoryDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, blogCategoryDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /blog-categories} : get all the Blog Categories.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Blog Categories in body.
     */
    @GetMapping("")
    public ResponseEntity<List<BlogCategoryDTO>> getAllBlogCategories(
        BlogCategoryCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get BlogCategories by criteria: {}", criteria);

        Page<BlogCategoryDTO> page = blogCategoryQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /blog-categories/count} : count all the blogCategories.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countBlogCategories(BlogCategoryCriteria criteria) {
        LOG.debug("REST request to count BlogCategories by criteria: {}", criteria);
        return ResponseEntity.ok().body(blogCategoryQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /blog-categories/:id} : get the "id" blogCategory.
     *
     * @param id the id of the blogCategoryDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the blogCategoryDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BlogCategoryDTO> getBlogCategory(@PathVariable("id") Long id) {
        LOG.debug("REST request to get BlogCategory : {}", id);
        Optional<BlogCategoryDTO> blogCategoryDTO = blogCategoryService.findOne(id);
        return ResponseUtil.wrapOrNotFound(blogCategoryDTO);
    }

    /**
     * {@code DELETE  /blog-categories/:id} : delete the "id" blogCategory.
     *
     * @param id the id of the blogCategoryDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBlogCategory(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete BlogCategory : {}", id);
        blogCategoryService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code SEARCH  /blog-categories/_search?query=:query} : search for the blogCategory corresponding
     * to the query.
     *
     * @param query the query of the blogCategory search.
     * @param pageable the pagination information.
     * @return the result of the search.
     */
    @GetMapping("/_search")
    public ResponseEntity<List<BlogCategoryDTO>> searchBlogCategories(
        @RequestParam("query") String query,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to search for a page of BlogCategories for query {}", query);
        try {
            Page<BlogCategoryDTO> page = blogCategoryService.search(query, pageable);
            HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
            return ResponseEntity.ok().headers(headers).body(page.getContent());
        } catch (RuntimeException e) {
            throw ElasticsearchExceptionMapper.mapException(e);
        }
    }
}
