package com.mycompany.myapp.service.impl;

import com.mycompany.myapp.domain.ArticleCategory;
import com.mycompany.myapp.repository.ArticleCategoryRepository;
import com.mycompany.myapp.repository.search.ArticleCategorySearchRepository;
import com.mycompany.myapp.service.ArticleCategoryService;
import com.mycompany.myapp.service.dto.ArticleCategoryDTO;
import com.mycompany.myapp.service.mapper.ArticleCategoryMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.ArticleCategory}.
 */
@Service
@Transactional
public class ArticleCategoryServiceImpl implements ArticleCategoryService {

    private static final Logger LOG = LoggerFactory.getLogger(ArticleCategoryServiceImpl.class);

    private final ArticleCategoryRepository articleCategoryRepository;

    private final ArticleCategoryMapper articleCategoryMapper;

    private final ArticleCategorySearchRepository articleCategorySearchRepository;

    public ArticleCategoryServiceImpl(
        ArticleCategoryRepository articleCategoryRepository,
        ArticleCategoryMapper articleCategoryMapper,
        ArticleCategorySearchRepository articleCategorySearchRepository
    ) {
        this.articleCategoryRepository = articleCategoryRepository;
        this.articleCategoryMapper = articleCategoryMapper;
        this.articleCategorySearchRepository = articleCategorySearchRepository;
    }

    @Override
    public ArticleCategoryDTO save(ArticleCategoryDTO articleCategoryDTO) {
        LOG.debug("Request to save ArticleCategory : {}", articleCategoryDTO);
        ArticleCategory articleCategory = articleCategoryMapper.toEntity(articleCategoryDTO);
        articleCategory = articleCategoryRepository.save(articleCategory);
        articleCategorySearchRepository.index(articleCategory);
        return articleCategoryMapper.toDto(articleCategory);
    }

    @Override
    public ArticleCategoryDTO update(ArticleCategoryDTO articleCategoryDTO) {
        LOG.debug("Request to update ArticleCategory : {}", articleCategoryDTO);
        ArticleCategory articleCategory = articleCategoryMapper.toEntity(articleCategoryDTO);
        articleCategory = articleCategoryRepository.save(articleCategory);
        articleCategorySearchRepository.index(articleCategory);
        return articleCategoryMapper.toDto(articleCategory);
    }

    @Override
    public Optional<ArticleCategoryDTO> partialUpdate(ArticleCategoryDTO articleCategoryDTO) {
        LOG.debug("Request to partially update ArticleCategory : {}", articleCategoryDTO);

        return articleCategoryRepository
            .findById(articleCategoryDTO.getId())
            .map(existingArticleCategory -> {
                articleCategoryMapper.partialUpdate(existingArticleCategory, articleCategoryDTO);

                return existingArticleCategory;
            })
            .map(articleCategoryRepository::save)
            .map(savedArticleCategory -> {
                articleCategorySearchRepository.index(savedArticleCategory);
                return savedArticleCategory;
            })
            .map(articleCategoryMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleCategoryDTO> findAll() {
        LOG.debug("Request to get all ArticleCategories");
        return articleCategoryRepository
            .findAll()
            .stream()
            .map(articleCategoryMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    public List<ArticleCategoryDTO> findAllWithEagerRelationships() {
        return articleCategoryRepository
            .findAllWithEagerRelationships()
            .stream()
            .map(articleCategoryMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ArticleCategoryDTO> findOne(Long id) {
        LOG.debug("Request to get ArticleCategory : {}", id);
        return articleCategoryRepository.findOneWithEagerRelationships(id).map(articleCategoryMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete ArticleCategory : {}", id);
        articleCategoryRepository.deleteById(id);
        articleCategorySearchRepository.deleteFromIndexById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleCategoryDTO> search(String query) {
        LOG.debug("Request to search ArticleCategories for query {}", query);
        try {
            return StreamSupport.stream(articleCategorySearchRepository.search(query).spliterator(), false)
                .map(articleCategoryMapper::toDto)
                .toList();
        } catch (RuntimeException e) {
            throw e;
        }
    }
}
