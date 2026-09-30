package com.mycompany.myapp.service.impl;

import com.mycompany.myapp.domain.Listing;
import com.mycompany.myapp.repository.ListingRepository;
import com.mycompany.myapp.repository.search.ListingSearchRepository;
import com.mycompany.myapp.service.ListingService;
import com.mycompany.myapp.service.dto.ListingDTO;
import com.mycompany.myapp.service.mapper.ListingMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.mycompany.myapp.domain.Listing}.
 */
@Service
@Transactional
public class ListingServiceImpl implements ListingService {

    private static final Logger LOG = LoggerFactory.getLogger(ListingServiceImpl.class);

    private final ListingRepository listingRepository;

    private final ListingMapper listingMapper;

    private final ListingSearchRepository listingSearchRepository;

    public ListingServiceImpl(
        ListingRepository listingRepository,
        ListingMapper listingMapper,
        ListingSearchRepository listingSearchRepository
    ) {
        this.listingRepository = listingRepository;
        this.listingMapper = listingMapper;
        this.listingSearchRepository = listingSearchRepository;
    }

    @Override
    public ListingDTO save(ListingDTO listingDTO) {
        LOG.debug("Request to save Listing : {}", listingDTO);
        Listing listing = listingMapper.toEntity(listingDTO);
        listing = listingRepository.save(listing);
        listingSearchRepository.index(listing);
        return listingMapper.toDto(listing);
    }

    @Override
    public ListingDTO update(ListingDTO listingDTO) {
        LOG.debug("Request to update Listing : {}", listingDTO);
        Listing listing = listingMapper.toEntity(listingDTO);
        listing = listingRepository.save(listing);
        listingSearchRepository.index(listing);
        return listingMapper.toDto(listing);
    }

    @Override
    public Optional<ListingDTO> partialUpdate(ListingDTO listingDTO) {
        LOG.debug("Request to partially update Listing : {}", listingDTO);

        return listingRepository
            .findById(listingDTO.getId())
            .map(existingListing -> {
                listingMapper.partialUpdate(existingListing, listingDTO);

                return existingListing;
            })
            .map(listingRepository::save)
            .map(savedListing -> {
                listingSearchRepository.index(savedListing);
                return savedListing;
            })
            .map(listingMapper::toDto);
    }

    public Page<ListingDTO> findAllWithEagerRelationships(Pageable pageable) {
        return listingRepository.findAllWithEagerRelationships(pageable).map(listingMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ListingDTO> findOne(Long id) {
        LOG.debug("Request to get Listing : {}", id);
        return listingRepository.findOneWithEagerRelationships(id).map(listingMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete Listing : {}", id);
        listingRepository.deleteById(id);
        listingSearchRepository.deleteFromIndexById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ListingDTO> search(String query, Pageable pageable) {
        LOG.debug("Request to search for a page of Listings for query {}", query);
        return listingSearchRepository.search(query, pageable).map(listingMapper::toDto);
    }
}
