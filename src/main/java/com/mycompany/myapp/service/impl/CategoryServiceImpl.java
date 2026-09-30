package com.mycompany.myapp.service.impl;

import com.mycompany.myapp.domain.Category;
import com.mycompany.myapp.repository.CategoryRepository;
import com.mycompany.myapp.repository.search.CategorySearchRepository;
import com.mycompany.myapp.service.CategoryService;
import com.mycompany.myapp.service.dto.CategoryDTO;
import com.mycompany.myapp.service.mapper.CategoryMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.Category}.
 */
@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private static final Logger LOG = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryRepository categoryRepository;

    private final CategoryMapper categoryMapper;

    private final CategorySearchRepository categorySearchRepository;

    public CategoryServiceImpl(
        CategoryRepository categoryRepository,
        CategoryMapper categoryMapper,
        CategorySearchRepository categorySearchRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
        this.categorySearchRepository = categorySearchRepository;
    }

    @Override
    public CategoryDTO save(CategoryDTO categoryDTO) {
        LOG.debug("Request to save Category : {}", categoryDTO);
        Category category = categoryMapper.toEntity(categoryDTO);
        category = categoryRepository.save(category);
        categorySearchRepository.index(category);
        return categoryMapper.toDto(category);
    }

    @Override
    public CategoryDTO update(CategoryDTO categoryDTO) {
        LOG.debug("Request to update Category : {}", categoryDTO);
        Category category = categoryMapper.toEntity(categoryDTO);
        category = categoryRepository.save(category);
        categorySearchRepository.index(category);
        return categoryMapper.toDto(category);
    }

    @Override
    public Optional<CategoryDTO> partialUpdate(CategoryDTO categoryDTO) {
        LOG.debug("Request to partially update Category : {}", categoryDTO);

        return categoryRepository
            .findById(categoryDTO.getId())
            .map(existingCategory -> {
                categoryMapper.partialUpdate(existingCategory, categoryDTO);

                return existingCategory;
            })
            .map(categoryRepository::save)
            .map(savedCategory -> {
                categorySearchRepository.index(savedCategory);
                return savedCategory;
            })
            .map(categoryMapper::toDto);
    }

    public Page<CategoryDTO> findAllWithEagerRelationships(Pageable pageable) {
        return categoryRepository.findAllWithEagerRelationships(pageable).map(categoryMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CategoryDTO> findOne(Long id) {
        LOG.debug("Request to get Category : {}", id);
        return categoryRepository.findOneWithEagerRelationships(id).map(categoryMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete Category : {}", id);
        categoryRepository.deleteById(id);
        categorySearchRepository.deleteFromIndexById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryDTO> search(String query, Pageable pageable) {
        LOG.debug("Request to search for a page of Categories for query {}", query);
        return categorySearchRepository.search(query, pageable).map(categoryMapper::toDto);
    }
}
