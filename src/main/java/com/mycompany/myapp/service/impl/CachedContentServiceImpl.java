package com.mycompany.myapp.service.impl;

import com.mycompany.myapp.domain.CachedContent;
import com.mycompany.myapp.repository.CachedContentRepository;
import com.mycompany.myapp.repository.search.CachedContentSearchRepository;
import com.mycompany.myapp.service.CachedContentService;
import com.mycompany.myapp.service.dto.CachedContentDTO;
import com.mycompany.myapp.service.mapper.CachedContentMapper;
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
 * Service Implementation for managing {@link com.mycompany.myapp.domain.CachedContent}.
 */
@Service
@Transactional
public class CachedContentServiceImpl implements CachedContentService {

    private static final Logger LOG = LoggerFactory.getLogger(CachedContentServiceImpl.class);

    private final CachedContentRepository cachedContentRepository;

    private final CachedContentMapper cachedContentMapper;

    private final CachedContentSearchRepository cachedContentSearchRepository;

    public CachedContentServiceImpl(
        CachedContentRepository cachedContentRepository,
        CachedContentMapper cachedContentMapper,
        CachedContentSearchRepository cachedContentSearchRepository
    ) {
        this.cachedContentRepository = cachedContentRepository;
        this.cachedContentMapper = cachedContentMapper;
        this.cachedContentSearchRepository = cachedContentSearchRepository;
    }

    @Override
    public CachedContentDTO save(CachedContentDTO cachedContentDTO) {
        LOG.debug("Request to save CachedContent : {}", cachedContentDTO);
        CachedContent cachedContent = cachedContentMapper.toEntity(cachedContentDTO);
        cachedContent = cachedContentRepository.save(cachedContent);
        cachedContentSearchRepository.index(cachedContent);
        return cachedContentMapper.toDto(cachedContent);
    }

    @Override
    public CachedContentDTO update(CachedContentDTO cachedContentDTO) {
        LOG.debug("Request to update CachedContent : {}", cachedContentDTO);
        CachedContent cachedContent = cachedContentMapper.toEntity(cachedContentDTO);
        cachedContent = cachedContentRepository.save(cachedContent);
        cachedContentSearchRepository.index(cachedContent);
        return cachedContentMapper.toDto(cachedContent);
    }

    @Override
    public Optional<CachedContentDTO> partialUpdate(CachedContentDTO cachedContentDTO) {
        LOG.debug("Request to partially update CachedContent : {}", cachedContentDTO);

        return cachedContentRepository
            .findById(cachedContentDTO.getId())
            .map(existingCachedContent -> {
                cachedContentMapper.partialUpdate(existingCachedContent, cachedContentDTO);

                return existingCachedContent;
            })
            .map(cachedContentRepository::save)
            .map(savedCachedContent -> {
                cachedContentSearchRepository.index(savedCachedContent);
                return savedCachedContent;
            })
            .map(cachedContentMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CachedContentDTO> findAll() {
        LOG.debug("Request to get all CachedContents");
        return cachedContentRepository.findAll().stream().map(cachedContentMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CachedContentDTO> findOne(Long id) {
        LOG.debug("Request to get CachedContent : {}", id);
        return cachedContentRepository.findById(id).map(cachedContentMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete CachedContent : {}", id);
        cachedContentRepository.deleteById(id);
        cachedContentSearchRepository.deleteFromIndexById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CachedContentDTO> search(String query) {
        LOG.debug("Request to search CachedContents for query {}", query);
        try {
            return StreamSupport.stream(cachedContentSearchRepository.search(query).spliterator(), false)
                .map(cachedContentMapper::toDto)
                .toList();
        } catch (RuntimeException e) {
            throw e;
        }
    }
}
