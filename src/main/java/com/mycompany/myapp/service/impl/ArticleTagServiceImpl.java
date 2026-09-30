package com.mycompany.myapp.service.impl;

import com.mycompany.myapp.domain.ArticleTag;
import com.mycompany.myapp.repository.ArticleTagRepository;
import com.mycompany.myapp.repository.search.ArticleTagSearchRepository;
import com.mycompany.myapp.service.ArticleTagService;
import com.mycompany.myapp.service.dto.ArticleTagDTO;
import com.mycompany.myapp.service.mapper.ArticleTagMapper;
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
 * Service Implementation for managing {@link com.mycompany.myapp.domain.ArticleTag}.
 */
@Service
@Transactional
public class ArticleTagServiceImpl implements ArticleTagService {

    private static final Logger LOG = LoggerFactory.getLogger(ArticleTagServiceImpl.class);

    private final ArticleTagRepository articleTagRepository;

    private final ArticleTagMapper articleTagMapper;

    private final ArticleTagSearchRepository articleTagSearchRepository;

    public ArticleTagServiceImpl(
        ArticleTagRepository articleTagRepository,
        ArticleTagMapper articleTagMapper,
        ArticleTagSearchRepository articleTagSearchRepository
    ) {
        this.articleTagRepository = articleTagRepository;
        this.articleTagMapper = articleTagMapper;
        this.articleTagSearchRepository = articleTagSearchRepository;
    }

    @Override
    public ArticleTagDTO save(ArticleTagDTO articleTagDTO) {
        LOG.debug("Request to save ArticleTag : {}", articleTagDTO);
        ArticleTag articleTag = articleTagMapper.toEntity(articleTagDTO);
        articleTag = articleTagRepository.save(articleTag);
        articleTagSearchRepository.index(articleTag);
        return articleTagMapper.toDto(articleTag);
    }

    @Override
    public ArticleTagDTO update(ArticleTagDTO articleTagDTO) {
        LOG.debug("Request to update ArticleTag : {}", articleTagDTO);
        ArticleTag articleTag = articleTagMapper.toEntity(articleTagDTO);
        articleTag = articleTagRepository.save(articleTag);
        articleTagSearchRepository.index(articleTag);
        return articleTagMapper.toDto(articleTag);
    }

    @Override
    public Optional<ArticleTagDTO> partialUpdate(ArticleTagDTO articleTagDTO) {
        LOG.debug("Request to partially update ArticleTag : {}", articleTagDTO);

        return articleTagRepository
            .findById(articleTagDTO.getId())
            .map(existingArticleTag -> {
                articleTagMapper.partialUpdate(existingArticleTag, articleTagDTO);

                return existingArticleTag;
            })
            .map(articleTagRepository::save)
            .map(savedArticleTag -> {
                articleTagSearchRepository.index(savedArticleTag);
                return savedArticleTag;
            })
            .map(articleTagMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleTagDTO> findAll() {
        LOG.debug("Request to get all ArticleTags");
        return articleTagRepository.findAll().stream().map(articleTagMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ArticleTagDTO> findOne(Long id) {
        LOG.debug("Request to get ArticleTag : {}", id);
        return articleTagRepository.findById(id).map(articleTagMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete ArticleTag : {}", id);
        articleTagRepository.deleteById(id);
        articleTagSearchRepository.deleteFromIndexById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleTagDTO> search(String query) {
        LOG.debug("Request to search ArticleTags for query {}", query);
        try {
            return StreamSupport.stream(articleTagSearchRepository.search(query).spliterator(), false)
                .map(articleTagMapper::toDto)
                .toList();
        } catch (RuntimeException e) {
            throw e;
        }
    }
}
