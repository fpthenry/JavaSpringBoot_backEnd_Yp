package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.ArticleCategoryRepository;
import com.mycompany.myapp.service.ArticleCategoryService;
import com.mycompany.myapp.service.dto.ArticleCategoryDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.ArticleCategory}.
 */
@RestController
@RequestMapping("/api/article-categories")
public class ArticleCategoryResource {

    private static final Logger LOG = LoggerFactory.getLogger(ArticleCategoryResource.class);

    private static final String ENTITY_NAME = "articleCategory";

    @Value("${jhipster.clientApp.name:javaSpringBootBackEnd}")
    private String applicationName;

    private final ArticleCategoryService articleCategoryService;

    private final ArticleCategoryRepository articleCategoryRepository;

    public ArticleCategoryResource(ArticleCategoryService articleCategoryService, ArticleCategoryRepository articleCategoryRepository) {
        this.articleCategoryService = articleCategoryService;
        this.articleCategoryRepository = articleCategoryRepository;
    }

    /**
     * {@code POST  /article-categories} : Create a new articleCategory.
     *
     * @param articleCategoryDTO the articleCategoryDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new articleCategoryDTO, or with status {@code 400 (Bad Request)} if the articleCategory has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ArticleCategoryDTO> createArticleCategory(@Valid @RequestBody ArticleCategoryDTO articleCategoryDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save ArticleCategory : {}", articleCategoryDTO);
        if (articleCategoryDTO.getId() != null) {
            throw new BadRequestAlertException("A new articleCategory cannot already have an ID", ENTITY_NAME, "idexists");
        }
        articleCategoryDTO = articleCategoryService.save(articleCategoryDTO);
        return ResponseEntity.created(new URI("/api/article-categories/" + articleCategoryDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, articleCategoryDTO.getId().toString()))
            .body(articleCategoryDTO);
    }

    /**
     * {@code PUT  /article-categories/:id} : Updates an existing articleCategory.
     *
     * @param id the id of the articleCategoryDTO to save.
     * @param articleCategoryDTO the articleCategoryDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated articleCategoryDTO,
     * or with status {@code 400 (Bad Request)} if the articleCategoryDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the articleCategoryDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ArticleCategoryDTO> updateArticleCategory(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody ArticleCategoryDTO articleCategoryDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update ArticleCategory : {}, {}", id, articleCategoryDTO);
        if (articleCategoryDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, articleCategoryDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!articleCategoryRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        articleCategoryDTO = articleCategoryService.update(articleCategoryDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, articleCategoryDTO.getId().toString()))
            .body(articleCategoryDTO);
    }

    /**
     * {@code PATCH  /article-categories/:id} : Partial updates given fields of an existing articleCategory, field will ignore if it is null
     *
     * @param id the id of the articleCategoryDTO to save.
     * @param articleCategoryDTO the articleCategoryDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated articleCategoryDTO,
     * or with status {@code 400 (Bad Request)} if the articleCategoryDTO is not valid,
     * or with status {@code 404 (Not Found)} if the articleCategoryDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the articleCategoryDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ArticleCategoryDTO> partialUpdateArticleCategory(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody ArticleCategoryDTO articleCategoryDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update ArticleCategory partially : {}, {}", id, articleCategoryDTO);
        if (articleCategoryDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, articleCategoryDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!articleCategoryRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ArticleCategoryDTO> result = articleCategoryService.partialUpdate(articleCategoryDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, articleCategoryDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /article-categories} : get all the Article Categories.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Article Categories in body.
     */
    @GetMapping("")
    public List<ArticleCategoryDTO> getAllArticleCategories(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all ArticleCategories");
        if (eagerload) {
            return articleCategoryService.findAllWithEagerRelationships();
        } else {
            return articleCategoryService.findAll();
        }
    }

    /**
     * {@code GET  /article-categories/:id} : get the "id" articleCategory.
     *
     * @param id the id of the articleCategoryDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the articleCategoryDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ArticleCategoryDTO> getArticleCategory(@PathVariable("id") Long id) {
        LOG.debug("REST request to get ArticleCategory : {}", id);
        Optional<ArticleCategoryDTO> articleCategoryDTO = articleCategoryService.findOne(id);
        return ResponseUtil.wrapOrNotFound(articleCategoryDTO);
    }

    /**
     * {@code DELETE  /article-categories/:id} : delete the "id" articleCategory.
     *
     * @param id the id of the articleCategoryDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticleCategory(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete ArticleCategory : {}", id);
        articleCategoryService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code SEARCH  /article-categories/_search?query=:query} : search for the articleCategory corresponding
     * to the query.
     *
     * @param query the query of the articleCategory search.
     * @return the result of the search.
     */
    @GetMapping("/_search")
    public List<ArticleCategoryDTO> searchArticleCategories(@RequestParam("query") String query) {
        LOG.debug("REST request to search ArticleCategories for query {}", query);
        try {
            return articleCategoryService.search(query);
        } catch (RuntimeException e) {
            throw ElasticsearchExceptionMapper.mapException(e);
        }
    }
}
