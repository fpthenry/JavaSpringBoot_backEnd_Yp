package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.repository.ArticleTagRepository;
import com.mycompany.myapp.service.ArticleTagService;
import com.mycompany.myapp.service.dto.ArticleTagDTO;
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
 * REST controller for managing {@link com.mycompany.myapp.domain.ArticleTag}.
 */
@RestController
@RequestMapping("/api/article-tags")
public class ArticleTagResource {

    private static final Logger LOG = LoggerFactory.getLogger(ArticleTagResource.class);

    private static final String ENTITY_NAME = "articleTag";

    @Value("${jhipster.clientApp.name:javaSpringBootBackEnd}")
    private String applicationName;

    private final ArticleTagService articleTagService;

    private final ArticleTagRepository articleTagRepository;

    public ArticleTagResource(ArticleTagService articleTagService, ArticleTagRepository articleTagRepository) {
        this.articleTagService = articleTagService;
        this.articleTagRepository = articleTagRepository;
    }

    /**
     * {@code POST  /article-tags} : Create a new articleTag.
     *
     * @param articleTagDTO the articleTagDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new articleTagDTO, or with status {@code 400 (Bad Request)} if the articleTag has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ArticleTagDTO> createArticleTag(@Valid @RequestBody ArticleTagDTO articleTagDTO) throws URISyntaxException {
        LOG.debug("REST request to save ArticleTag : {}", articleTagDTO);
        if (articleTagDTO.getId() != null) {
            throw new BadRequestAlertException("A new articleTag cannot already have an ID", ENTITY_NAME, "idexists");
        }
        articleTagDTO = articleTagService.save(articleTagDTO);
        return ResponseEntity.created(new URI("/api/article-tags/" + articleTagDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, articleTagDTO.getId().toString()))
            .body(articleTagDTO);
    }

    /**
     * {@code PUT  /article-tags/:id} : Updates an existing articleTag.
     *
     * @param id the id of the articleTagDTO to save.
     * @param articleTagDTO the articleTagDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated articleTagDTO,
     * or with status {@code 400 (Bad Request)} if the articleTagDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the articleTagDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ArticleTagDTO> updateArticleTag(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody ArticleTagDTO articleTagDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update ArticleTag : {}, {}", id, articleTagDTO);
        if (articleTagDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, articleTagDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!articleTagRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        articleTagDTO = articleTagService.update(articleTagDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, articleTagDTO.getId().toString()))
            .body(articleTagDTO);
    }

    /**
     * {@code PATCH  /article-tags/:id} : Partial updates given fields of an existing articleTag, field will ignore if it is null
     *
     * @param id the id of the articleTagDTO to save.
     * @param articleTagDTO the articleTagDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated articleTagDTO,
     * or with status {@code 400 (Bad Request)} if the articleTagDTO is not valid,
     * or with status {@code 404 (Not Found)} if the articleTagDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the articleTagDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ArticleTagDTO> partialUpdateArticleTag(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody ArticleTagDTO articleTagDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update ArticleTag partially : {}, {}", id, articleTagDTO);
        if (articleTagDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, articleTagDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!articleTagRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ArticleTagDTO> result = articleTagService.partialUpdate(articleTagDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, articleTagDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /article-tags} : get all the Article Tags.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Article Tags in body.
     */
    @GetMapping("")
    public List<ArticleTagDTO> getAllArticleTags() {
        LOG.debug("REST request to get all ArticleTags");
        return articleTagService.findAll();
    }

    /**
     * {@code GET  /article-tags/:id} : get the "id" articleTag.
     *
     * @param id the id of the articleTagDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the articleTagDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ArticleTagDTO> getArticleTag(@PathVariable("id") Long id) {
        LOG.debug("REST request to get ArticleTag : {}", id);
        Optional<ArticleTagDTO> articleTagDTO = articleTagService.findOne(id);
        return ResponseUtil.wrapOrNotFound(articleTagDTO);
    }

    /**
     * {@code DELETE  /article-tags/:id} : delete the "id" articleTag.
     *
     * @param id the id of the articleTagDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticleTag(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete ArticleTag : {}", id);
        articleTagService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code SEARCH  /article-tags/_search?query=:query} : search for the articleTag corresponding
     * to the query.
     *
     * @param query the query of the articleTag search.
     * @return the result of the search.
     */
    @GetMapping("/_search")
    public List<ArticleTagDTO> searchArticleTags(@RequestParam("query") String query) {
        LOG.debug("REST request to search ArticleTags for query {}", query);
        try {
            return articleTagService.search(query);
        } catch (RuntimeException e) {
            throw ElasticsearchExceptionMapper.mapException(e);
        }
    }
}
