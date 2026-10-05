package com.mycompany.myapp.service.impl;

import com.mycompany.myapp.domain.BlogPost;
import com.mycompany.myapp.repository.BlogPostRepository;
import com.mycompany.myapp.repository.search.BlogPostSearchFields;
import com.mycompany.myapp.repository.search.BlogPostSearchFilter;
import com.mycompany.myapp.repository.search.BlogPostSearchRepository;
import com.mycompany.myapp.service.BlogPostService;
import com.mycompany.myapp.service.dto.BlogPostDTO;
import com.mycompany.myapp.service.mapper.BlogPostMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.BlogPost}.
 */
@Service
@Transactional
public class BlogPostServiceImpl implements BlogPostService {

    private static final Logger LOG = LoggerFactory.getLogger(BlogPostServiceImpl.class);

    private final BlogPostRepository blogPostRepository;

    private final BlogPostMapper blogPostMapper;

    private final BlogPostSearchRepository blogPostSearchRepository;

    private final BlogPostSearchFields blogPostSearchFields;

    public BlogPostServiceImpl(
        BlogPostRepository blogPostRepository,
        BlogPostMapper blogPostMapper,
        BlogPostSearchRepository blogPostSearchRepository,
        BlogPostSearchFields blogPostSearchFields
    ) {
        this.blogPostRepository = blogPostRepository;
        this.blogPostMapper = blogPostMapper;
        this.blogPostSearchRepository = blogPostSearchRepository;
        this.blogPostSearchFields = blogPostSearchFields;
    }

    @Override
    public BlogPostDTO save(BlogPostDTO blogPostDTO) {
        LOG.debug("Request to save BlogPost : {}", blogPostDTO);
        BlogPost blogPost = blogPostMapper.toEntity(blogPostDTO);
        blogPost = blogPostRepository.save(blogPost);
        // Sửa tay: điền field tìm kiếm (danh mục, thẻ) ngay trong transaction này, index chạy nền chỉ ghi lên ES
        blogPostSearchRepository.index(blogPostSearchFields.fill(blogPost));
        return blogPostMapper.toDto(blogPost);
    }

    @Override
    public BlogPostDTO update(BlogPostDTO blogPostDTO) {
        LOG.debug("Request to update BlogPost : {}", blogPostDTO);
        BlogPost blogPost = blogPostMapper.toEntity(blogPostDTO);
        blogPost = blogPostRepository.save(blogPost);
        // Sửa tay: điền field tìm kiếm (danh mục, thẻ) ngay trong transaction này, index chạy nền chỉ ghi lên ES
        blogPostSearchRepository.index(blogPostSearchFields.fill(blogPost));
        return blogPostMapper.toDto(blogPost);
    }

    @Override
    public Optional<BlogPostDTO> partialUpdate(BlogPostDTO blogPostDTO) {
        LOG.debug("Request to partially update BlogPost : {}", blogPostDTO);

        return blogPostRepository
            .findById(blogPostDTO.getId())
            .map(existingBlogPost -> {
                blogPostMapper.partialUpdate(existingBlogPost, blogPostDTO);

                return existingBlogPost;
            })
            .map(blogPostRepository::save)
            .map(savedBlogPost -> {
                blogPostSearchRepository.index(blogPostSearchFields.fill(savedBlogPost));
                return savedBlogPost;
            })
            .map(blogPostMapper::toDto);
    }

    public Page<BlogPostDTO> findAllWithEagerRelationships(Pageable pageable) {
        return blogPostRepository.findAllWithEagerRelationships(pageable).map(blogPostMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BlogPostDTO> findOne(Long id) {
        LOG.debug("Request to get BlogPost : {}", id);
        return blogPostRepository.findOneWithEagerRelationships(id).map(blogPostMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete BlogPost : {}", id);
        blogPostRepository.deleteById(id);
        blogPostSearchRepository.deleteFromIndexById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BlogPostDTO> search(String query, Pageable pageable) {
        LOG.debug("Request to search for a page of BlogPosts for query {}", query);
        return blogPostSearchRepository.search(query, pageable).map(blogPostMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BlogPostDTO> search(BlogPostSearchFilter filter, Pageable pageable) {
        LOG.debug("Request to search for a page of BlogPosts for filter {}", filter);
        return blogPostSearchRepository.search(filter, pageable).map(blogPostMapper::toDto);
    }
}
