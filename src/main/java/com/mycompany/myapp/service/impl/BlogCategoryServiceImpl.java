package com.mycompany.myapp.service.impl;

import com.mycompany.myapp.domain.BlogCategory;
import com.mycompany.myapp.repository.BlogCategoryRepository;
import com.mycompany.myapp.repository.search.BlogCategorySearchRepository;
import com.mycompany.myapp.service.BlogCategoryService;
import com.mycompany.myapp.service.dto.BlogCategoryDTO;
import com.mycompany.myapp.service.mapper.BlogCategoryMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.BlogCategory}.
 */
@Service
@Transactional
public class BlogCategoryServiceImpl implements BlogCategoryService {

    private static final Logger LOG = LoggerFactory.getLogger(BlogCategoryServiceImpl.class);

    private final BlogCategoryRepository blogCategoryRepository;

    private final BlogCategoryMapper blogCategoryMapper;

    private final BlogCategorySearchRepository blogCategorySearchRepository;

    public BlogCategoryServiceImpl(
        BlogCategoryRepository blogCategoryRepository,
        BlogCategoryMapper blogCategoryMapper,
        BlogCategorySearchRepository blogCategorySearchRepository
    ) {
        this.blogCategoryRepository = blogCategoryRepository;
        this.blogCategoryMapper = blogCategoryMapper;
        this.blogCategorySearchRepository = blogCategorySearchRepository;
    }

    @Override
    public BlogCategoryDTO save(BlogCategoryDTO blogCategoryDTO) {
        LOG.debug("Request to save BlogCategory : {}", blogCategoryDTO);
        BlogCategory blogCategory = blogCategoryMapper.toEntity(blogCategoryDTO);
        blogCategory = blogCategoryRepository.save(blogCategory);
        blogCategorySearchRepository.index(blogCategory);
        return blogCategoryMapper.toDto(blogCategory);
    }

    @Override
    public BlogCategoryDTO update(BlogCategoryDTO blogCategoryDTO) {
        LOG.debug("Request to update BlogCategory : {}", blogCategoryDTO);
        BlogCategory blogCategory = blogCategoryMapper.toEntity(blogCategoryDTO);
        blogCategory = blogCategoryRepository.save(blogCategory);
        blogCategorySearchRepository.index(blogCategory);
        return blogCategoryMapper.toDto(blogCategory);
    }

    @Override
    public Optional<BlogCategoryDTO> partialUpdate(BlogCategoryDTO blogCategoryDTO) {
        LOG.debug("Request to partially update BlogCategory : {}", blogCategoryDTO);

        return blogCategoryRepository
            .findById(blogCategoryDTO.getId())
            .map(existingBlogCategory -> {
                blogCategoryMapper.partialUpdate(existingBlogCategory, blogCategoryDTO);

                return existingBlogCategory;
            })
            .map(blogCategoryRepository::save)
            .map(savedBlogCategory -> {
                blogCategorySearchRepository.index(savedBlogCategory);
                return savedBlogCategory;
            })
            .map(blogCategoryMapper::toDto);
    }

    public Page<BlogCategoryDTO> findAllWithEagerRelationships(Pageable pageable) {
        return blogCategoryRepository.findAllWithEagerRelationships(pageable).map(blogCategoryMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BlogCategoryDTO> findOne(Long id) {
        LOG.debug("Request to get BlogCategory : {}", id);
        return blogCategoryRepository.findOneWithEagerRelationships(id).map(blogCategoryMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete BlogCategory : {}", id);
        blogCategoryRepository.deleteById(id);
        blogCategorySearchRepository.deleteFromIndexById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BlogCategoryDTO> search(String query, Pageable pageable) {
        LOG.debug("Request to search for a page of BlogCategories for query {}", query);
        return blogCategorySearchRepository.search(query, pageable).map(blogCategoryMapper::toDto);
    }
}
