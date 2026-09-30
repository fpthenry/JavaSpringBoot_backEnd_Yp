package com.mycompany.myapp.service.impl;

import com.mycompany.myapp.domain.Gallery;
import com.mycompany.myapp.repository.GalleryRepository;
import com.mycompany.myapp.repository.search.GallerySearchRepository;
import com.mycompany.myapp.service.GalleryService;
import com.mycompany.myapp.service.dto.GalleryDTO;
import com.mycompany.myapp.service.mapper.GalleryMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.Gallery}.
 */
@Service
@Transactional
public class GalleryServiceImpl implements GalleryService {

    private static final Logger LOG = LoggerFactory.getLogger(GalleryServiceImpl.class);

    private final GalleryRepository galleryRepository;

    private final GalleryMapper galleryMapper;

    private final GallerySearchRepository gallerySearchRepository;

    public GalleryServiceImpl(
        GalleryRepository galleryRepository,
        GalleryMapper galleryMapper,
        GallerySearchRepository gallerySearchRepository
    ) {
        this.galleryRepository = galleryRepository;
        this.galleryMapper = galleryMapper;
        this.gallerySearchRepository = gallerySearchRepository;
    }

    @Override
    public GalleryDTO save(GalleryDTO galleryDTO) {
        LOG.debug("Request to save Gallery : {}", galleryDTO);
        Gallery gallery = galleryMapper.toEntity(galleryDTO);
        gallery = galleryRepository.save(gallery);
        gallerySearchRepository.index(gallery);
        return galleryMapper.toDto(gallery);
    }

    @Override
    public GalleryDTO update(GalleryDTO galleryDTO) {
        LOG.debug("Request to update Gallery : {}", galleryDTO);
        Gallery gallery = galleryMapper.toEntity(galleryDTO);
        gallery = galleryRepository.save(gallery);
        gallerySearchRepository.index(gallery);
        return galleryMapper.toDto(gallery);
    }

    @Override
    public Optional<GalleryDTO> partialUpdate(GalleryDTO galleryDTO) {
        LOG.debug("Request to partially update Gallery : {}", galleryDTO);

        return galleryRepository
            .findById(galleryDTO.getId())
            .map(existingGallery -> {
                galleryMapper.partialUpdate(existingGallery, galleryDTO);

                return existingGallery;
            })
            .map(galleryRepository::save)
            .map(savedGallery -> {
                gallerySearchRepository.index(savedGallery);
                return savedGallery;
            })
            .map(galleryMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GalleryDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Galleries");
        return galleryRepository.findAll(pageable).map(galleryMapper::toDto);
    }

    public Page<GalleryDTO> findAllWithEagerRelationships(Pageable pageable) {
        return galleryRepository.findAllWithEagerRelationships(pageable).map(galleryMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GalleryDTO> findOne(Long id) {
        LOG.debug("Request to get Gallery : {}", id);
        return galleryRepository.findOneWithEagerRelationships(id).map(galleryMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete Gallery : {}", id);
        galleryRepository.deleteById(id);
        gallerySearchRepository.deleteFromIndexById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GalleryDTO> search(String query, Pageable pageable) {
        LOG.debug("Request to search for a page of Galleries for query {}", query);
        return gallerySearchRepository.search(query, pageable).map(galleryMapper::toDto);
    }
}
