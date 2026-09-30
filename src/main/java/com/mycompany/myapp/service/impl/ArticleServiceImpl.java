package com.mycompany.myapp.service.impl;

import com.mycompany.myapp.domain.Article;
import com.mycompany.myapp.repository.ArticleRepository;
import com.mycompany.myapp.repository.search.ArticleSearchRepository;
import com.mycompany.myapp.service.ArticleService;
import com.mycompany.myapp.service.dto.ArticleDTO;
import com.mycompany.myapp.service.mapper.ArticleMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.Article}.
 */
@Service
@Transactional
public class ArticleServiceImpl implements ArticleService {

    private static final Logger LOG = LoggerFactory.getLogger(ArticleServiceImpl.class);

    private final ArticleRepository articleRepository;

    private final ArticleMapper articleMapper;

    private final ArticleSearchRepository articleSearchRepository;

    public ArticleServiceImpl(
        ArticleRepository articleRepository,
        ArticleMapper articleMapper,
        ArticleSearchRepository articleSearchRepository
    ) {
        this.articleRepository = articleRepository;
        this.articleMapper = articleMapper;
        this.articleSearchRepository = articleSearchRepository;
    }

    @Override
    public ArticleDTO save(ArticleDTO articleDTO) {
        LOG.debug("Request to save Article : {}", articleDTO);
        Article article = articleMapper.toEntity(articleDTO);
        article = articleRepository.save(article);
        articleSearchRepository.index(article);
        return articleMapper.toDto(article);
    }

    @Override
    public ArticleDTO update(ArticleDTO articleDTO) {
        LOG.debug("Request to update Article : {}", articleDTO);
        Article article = articleMapper.toEntity(articleDTO);
        article = articleRepository.save(article);
        articleSearchRepository.index(article);
        return articleMapper.toDto(article);
    }

    @Override
    public Optional<ArticleDTO> partialUpdate(ArticleDTO articleDTO) {
        LOG.debug("Request to partially update Article : {}", articleDTO);

        return articleRepository
            .findById(articleDTO.getId())
            .map(existingArticle -> {
                articleMapper.partialUpdate(existingArticle, articleDTO);

                return existingArticle;
            })
            .map(articleRepository::save)
            .map(savedArticle -> {
                articleSearchRepository.index(savedArticle);
                return savedArticle;
            })
            .map(articleMapper::toDto);
    }

    public Page<ArticleDTO> findAllWithEagerRelationships(Pageable pageable) {
        return articleRepository.findAllWithEagerRelationships(pageable).map(articleMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ArticleDTO> findOne(Long id) {
        LOG.debug("Request to get Article : {}", id);
        return articleRepository.findOneWithEagerRelationships(id).map(articleMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete Article : {}", id);
        articleRepository.deleteById(id);
        articleSearchRepository.deleteFromIndexById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ArticleDTO> search(String query, Pageable pageable) {
        LOG.debug("Request to search for a page of Articles for query {}", query);
        return articleSearchRepository.search(query, pageable).map(articleMapper::toDto);
    }
}
