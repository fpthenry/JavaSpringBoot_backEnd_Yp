package com.mycompany.myapp.service.impl;

import com.mycompany.myapp.domain.GalleryImage;
import com.mycompany.myapp.repository.GalleryImageRepository;
import com.mycompany.myapp.service.GalleryImageService;
import com.mycompany.myapp.service.dto.GalleryImageDTO;
import com.mycompany.myapp.service.mapper.GalleryImageMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.GalleryImage}.
 */
@Service
@Transactional
public class GalleryImageServiceImpl implements GalleryImageService {

    private static final Logger LOG = LoggerFactory.getLogger(GalleryImageServiceImpl.class);

    private final GalleryImageRepository galleryImageRepository;

    private final GalleryImageMapper galleryImageMapper;

    public GalleryImageServiceImpl(GalleryImageRepository galleryImageRepository, GalleryImageMapper galleryImageMapper) {
        this.galleryImageRepository = galleryImageRepository;
        this.galleryImageMapper = galleryImageMapper;
    }

    @Override
    public GalleryImageDTO save(GalleryImageDTO galleryImageDTO) {
        LOG.debug("Request to save GalleryImage : {}", galleryImageDTO);
        GalleryImage galleryImage = galleryImageMapper.toEntity(galleryImageDTO);
        galleryImage = galleryImageRepository.save(galleryImage);
        return galleryImageMapper.toDto(galleryImage);
    }

    @Override
    public GalleryImageDTO update(GalleryImageDTO galleryImageDTO) {
        LOG.debug("Request to update GalleryImage : {}", galleryImageDTO);
        GalleryImage galleryImage = galleryImageMapper.toEntity(galleryImageDTO);
        galleryImage = galleryImageRepository.save(galleryImage);
        return galleryImageMapper.toDto(galleryImage);
    }

    @Override
    public Optional<GalleryImageDTO> partialUpdate(GalleryImageDTO galleryImageDTO) {
        LOG.debug("Request to partially update GalleryImage : {}", galleryImageDTO);

        return galleryImageRepository
            .findById(galleryImageDTO.getId())
            .map(existingGalleryImage -> {
                galleryImageMapper.partialUpdate(existingGalleryImage, galleryImageDTO);

                return existingGalleryImage;
            })
            .map(galleryImageRepository::save)
            .map(galleryImageMapper::toDto);
    }

    public Page<GalleryImageDTO> findAllWithEagerRelationships(Pageable pageable) {
        return galleryImageRepository.findAllWithEagerRelationships(pageable).map(galleryImageMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GalleryImageDTO> findOne(Long id) {
        LOG.debug("Request to get GalleryImage : {}", id);
        return galleryImageRepository.findOneWithEagerRelationships(id).map(galleryImageMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete GalleryImage : {}", id);
        galleryImageRepository.deleteById(id);
    }
}
