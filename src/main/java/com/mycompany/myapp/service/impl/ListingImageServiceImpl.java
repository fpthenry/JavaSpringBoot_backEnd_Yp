package com.mycompany.myapp.service.impl;

import com.mycompany.myapp.domain.ListingImage;
import com.mycompany.myapp.repository.ListingImageRepository;
import com.mycompany.myapp.repository.search.ListingImageSearchRepository;
import com.mycompany.myapp.service.ListingImageService;
import com.mycompany.myapp.service.dto.ListingImageDTO;
import com.mycompany.myapp.service.mapper.ListingImageMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.ListingImage}.
 */
@Service
@Transactional
public class ListingImageServiceImpl implements ListingImageService {

    private static final Logger LOG = LoggerFactory.getLogger(ListingImageServiceImpl.class);

    private final ListingImageRepository listingImageRepository;

    private final ListingImageMapper listingImageMapper;

    private final ListingImageSearchRepository listingImageSearchRepository;

    public ListingImageServiceImpl(
        ListingImageRepository listingImageRepository,
        ListingImageMapper listingImageMapper,
        ListingImageSearchRepository listingImageSearchRepository
    ) {
        this.listingImageRepository = listingImageRepository;
        this.listingImageMapper = listingImageMapper;
        this.listingImageSearchRepository = listingImageSearchRepository;
    }

    @Override
    public ListingImageDTO save(ListingImageDTO listingImageDTO) {
        LOG.debug("Request to save ListingImage : {}", listingImageDTO);
        ListingImage listingImage = listingImageMapper.toEntity(listingImageDTO);
        listingImage = listingImageRepository.save(listingImage);
        listingImageSearchRepository.index(listingImage);
        return listingImageMapper.toDto(listingImage);
    }

    @Override
    public ListingImageDTO update(ListingImageDTO listingImageDTO) {
        LOG.debug("Request to update ListingImage : {}", listingImageDTO);
        ListingImage listingImage = listingImageMapper.toEntity(listingImageDTO);
        listingImage = listingImageRepository.save(listingImage);
        listingImageSearchRepository.index(listingImage);
        return listingImageMapper.toDto(listingImage);
    }

    @Override
    public Optional<ListingImageDTO> partialUpdate(ListingImageDTO listingImageDTO) {
        LOG.debug("Request to partially update ListingImage : {}", listingImageDTO);

        return listingImageRepository
            .findById(listingImageDTO.getId())
            .map(existingListingImage -> {
                listingImageMapper.partialUpdate(existingListingImage, listingImageDTO);

                return existingListingImage;
            })
            .map(listingImageRepository::save)
            .map(savedListingImage -> {
                listingImageSearchRepository.index(savedListingImage);
                return savedListingImage;
            })
            .map(listingImageMapper::toDto);
    }

    public Page<ListingImageDTO> findAllWithEagerRelationships(Pageable pageable) {
        return listingImageRepository.findAllWithEagerRelationships(pageable).map(listingImageMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ListingImageDTO> findOne(Long id) {
        LOG.debug("Request to get ListingImage : {}", id);
        return listingImageRepository.findOneWithEagerRelationships(id).map(listingImageMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete ListingImage : {}", id);
        listingImageRepository.deleteById(id);
        listingImageSearchRepository.deleteFromIndexById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ListingImageDTO> search(String query, Pageable pageable) {
        LOG.debug("Request to search for a page of ListingImages for query {}", query);
        return listingImageSearchRepository.search(query, pageable).map(listingImageMapper::toDto);
    }
}
