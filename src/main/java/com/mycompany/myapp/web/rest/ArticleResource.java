package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.ArticleRepository;
import com.mycompany.myapp.service.ArticleQueryService;
import com.mycompany.myapp.service.ArticleService;
import com.mycompany.myapp.service.criteria.ArticleCriteria;
import com.mycompany.myapp.service.dto.ArticleDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.Article}.
 */
@RestController
@RequestMapping("/api/articles")
public class ArticleResource {

    private static final Logger LOG = LoggerFactory.getLogger(ArticleResource.class);

    private static final String ENTITY_NAME = "article";

    @Value("${jhipster.clientApp.name:javaSpringBootBackEnd}")
    private String applicationName;

    private final ArticleService articleService;

    private final ArticleRepository articleRepository;

    private final ArticleQueryService articleQueryService;

    public ArticleResource(ArticleService articleService, ArticleRepository articleRepository, ArticleQueryService articleQueryService) {
        this.articleService = articleService;
        this.articleRepository = articleRepository;
        this.articleQueryService = articleQueryService;
    }

    /**
     * {@code POST  /articles} : Create a new article.
     *
     * @param articleDTO the articleDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new articleDTO, or with status {@code 400 (Bad Request)} if the article has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ArticleDTO> createArticle(@Valid @RequestBody ArticleDTO articleDTO) throws URISyntaxException {
        LOG.debug("REST request to save Article : {}", articleDTO);
        if (articleDTO.getId() != null) {
            throw new BadRequestAlertException("A new article cannot already have an ID", ENTITY_NAME, "idexists");
        }
        articleDTO = articleService.save(articleDTO);
        return ResponseEntity.created(new URI("/api/articles/" + articleDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, articleDTO.getId().toString()))
            .body(articleDTO);
    }

    /**
     * {@code PUT  /articles/:id} : Updates an existing article.
     *
     * @param id the id of the articleDTO to save.
     * @param articleDTO the articleDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated articleDTO,
     * or with status {@code 400 (Bad Request)} if the articleDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the articleDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ArticleDTO> updateArticle(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody ArticleDTO articleDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Article : {}, {}", id, articleDTO);
        if (articleDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, articleDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!articleRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        articleDTO = articleService.update(articleDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, articleDTO.getId().toString()))
            .body(articleDTO);
    }

    /**
     * {@code PATCH  /articles/:id} : Partial updates given fields of an existing article, field will ignore if it is null
     *
     * @param id the id of the articleDTO to save.
     * @param articleDTO the articleDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated articleDTO,
     * or with status {@code 400 (Bad Request)} if the articleDTO is not valid,
     * or with status {@code 404 (Not Found)} if the articleDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the articleDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ArticleDTO> partialUpdateArticle(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody ArticleDTO articleDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Article partially : {}, {}", id, articleDTO);
        if (articleDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, articleDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!articleRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ArticleDTO> result = articleService.partialUpdate(articleDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, articleDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /articles} : get all the Articles.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Articles in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ArticleDTO>> getAllArticles(
        ArticleCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Articles by criteria: {}", criteria);

        Page<ArticleDTO> page = articleQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /articles/count} : count all the articles.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countArticles(ArticleCriteria criteria) {
        LOG.debug("REST request to count Articles by criteria: {}", criteria);
        return ResponseEntity.ok().body(articleQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /articles/:id} : get the "id" article.
     *
     * @param id the id of the articleDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the articleDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ArticleDTO> getArticle(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Article : {}", id);
        Optional<ArticleDTO> articleDTO = articleService.findOne(id);
        return ResponseUtil.wrapOrNotFound(articleDTO);
    }

    /**
     * {@code DELETE  /articles/:id} : delete the "id" article.
     *
     * @param id the id of the articleDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Article : {}", id);
        articleService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code SEARCH  /articles/_search?query=:query} : search for the article corresponding
     * to the query.
     *
     * @param query the query of the article search.
     * @param pageable the pagination information.
     * @return the result of the search.
     */
    @GetMapping("/_search")
    public ResponseEntity<List<ArticleDTO>> searchArticles(
        @RequestParam("query") String query,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to search for a page of Articles for query {}", query);
        try {
            Page<ArticleDTO> page = articleService.search(query, pageable);
            HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
            return ResponseEntity.ok().headers(headers).body(page.getContent());
        } catch (RuntimeException e) {
            throw ElasticsearchExceptionMapper.mapException(e);
        }
    }
}
