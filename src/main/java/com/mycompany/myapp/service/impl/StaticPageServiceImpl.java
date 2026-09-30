package com.mycompany.myapp.service.impl;

import com.mycompany.myapp.domain.StaticPage;
import com.mycompany.myapp.repository.StaticPageRepository;
import com.mycompany.myapp.repository.search.StaticPageSearchRepository;
import com.mycompany.myapp.service.StaticPageService;
import com.mycompany.myapp.service.dto.StaticPageDTO;
import com.mycompany.myapp.service.mapper.StaticPageMapper;
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
 * Service Implementation for managing {@link com.mycompany.myapp.domain.StaticPage}.
 */
@Service
@Transactional
public class StaticPageServiceImpl implements StaticPageService {

    private static final Logger LOG = LoggerFactory.getLogger(StaticPageServiceImpl.class);

    private final StaticPageRepository staticPageRepository;

    private final StaticPageMapper staticPageMapper;

    private final StaticPageSearchRepository staticPageSearchRepository;

    public StaticPageServiceImpl(
        StaticPageRepository staticPageRepository,
        StaticPageMapper staticPageMapper,
        StaticPageSearchRepository staticPageSearchRepository
    ) {
        this.staticPageRepository = staticPageRepository;
        this.staticPageMapper = staticPageMapper;
        this.staticPageSearchRepository = staticPageSearchRepository;
    }

    @Override
    public StaticPageDTO save(StaticPageDTO staticPageDTO) {
        LOG.debug("Request to save StaticPage : {}", staticPageDTO);
        StaticPage staticPage = staticPageMapper.toEntity(staticPageDTO);
        staticPage = staticPageRepository.save(staticPage);
        staticPageSearchRepository.index(staticPage);
        return staticPageMapper.toDto(staticPage);
    }

    @Override
    public StaticPageDTO update(StaticPageDTO staticPageDTO) {
        LOG.debug("Request to update StaticPage : {}", staticPageDTO);
        StaticPage staticPage = staticPageMapper.toEntity(staticPageDTO);
        staticPage = staticPageRepository.save(staticPage);
        staticPageSearchRepository.index(staticPage);
        return staticPageMapper.toDto(staticPage);
    }

    @Override
    public Optional<StaticPageDTO> partialUpdate(StaticPageDTO staticPageDTO) {
        LOG.debug("Request to partially update StaticPage : {}", staticPageDTO);

        return staticPageRepository
            .findById(staticPageDTO.getId())
            .map(existingStaticPage -> {
                staticPageMapper.partialUpdate(existingStaticPage, staticPageDTO);

                return existingStaticPage;
            })
            .map(staticPageRepository::save)
            .map(savedStaticPage -> {
                staticPageSearchRepository.index(savedStaticPage);
                return savedStaticPage;
            })
            .map(staticPageMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaticPageDTO> findAll() {
        LOG.debug("Request to get all StaticPages");
        return staticPageRepository.findAll().stream().map(staticPageMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    public List<StaticPageDTO> findAllWithEagerRelationships() {
        return staticPageRepository
            .findAllWithEagerRelationships()
            .stream()
            .map(staticPageMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StaticPageDTO> findOne(Long id) {
        LOG.debug("Request to get StaticPage : {}", id);
        return staticPageRepository.findOneWithEagerRelationships(id).map(staticPageMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete StaticPage : {}", id);
        staticPageRepository.deleteById(id);
        staticPageSearchRepository.deleteFromIndexById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaticPageDTO> search(String query) {
        LOG.debug("Request to search StaticPages for query {}", query);
        try {
            return StreamSupport.stream(staticPageSearchRepository.search(query).spliterator(), false)
                .map(staticPageMapper::toDto)
                .toList();
        } catch (RuntimeException e) {
            throw e;
        }
    }
}
