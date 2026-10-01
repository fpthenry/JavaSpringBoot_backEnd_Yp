package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Listing;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

/**
 * Utility repository to load bag relationships based on https://vladmihalcea.com/hibernate-multiplebagfetchexception/
 */
public class ListingRepositoryWithBagRelationshipsImpl implements ListingRepositoryWithBagRelationships {

    private static final String ID_PARAMETER = "id";
    private static final String LISTINGS_PARAMETER = "listings";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Listing> fetchBagRelationships(Optional<Listing> listing) {
        return listing.map(this::fetchCategories).map(this::fetchLocations);
    }

    @Override
    public Page<Listing> fetchBagRelationships(Page<Listing> listings) {
        return new PageImpl<>(fetchBagRelationships(listings.getContent()), listings.getPageable(), listings.getTotalElements());
    }

    @Override
    public List<Listing> fetchBagRelationships(List<Listing> listings) {
        return Optional.of(listings).map(this::fetchCategories).map(this::fetchLocations).orElse(List.of());
    }

    Listing fetchCategories(Listing result) {
        return entityManager
            .createQuery("select listing from Listing listing left join fetch listing.categories where listing.id = :id", Listing.class)
            .setParameter(ID_PARAMETER, result.getId())
            .getSingleResult();
    }

    List<Listing> fetchCategories(List<Listing> listings) {
        HashMap<Object, Integer> order = new HashMap<>();
        IntStream.range(0, listings.size()).forEach(index -> order.put(listings.get(index).getId(), index));
        List<Listing> result = entityManager
            .createQuery("select listing from Listing listing left join fetch listing.categories where listing in :listings", Listing.class)
            .setParameter(LISTINGS_PARAMETER, listings)
            .getResultList();
        result.sort((o1, o2) -> Integer.compare(order.get(o1.getId()), order.get(o2.getId())));
        return result;
    }

    Listing fetchLocations(Listing result) {
        return entityManager
            .createQuery("select listing from Listing listing left join fetch listing.locations where listing.id = :id", Listing.class)
            .setParameter(ID_PARAMETER, result.getId())
            .getSingleResult();
    }

    List<Listing> fetchLocations(List<Listing> listings) {
        HashMap<Object, Integer> order = new HashMap<>();
        IntStream.range(0, listings.size()).forEach(index -> order.put(listings.get(index).getId(), index));
        List<Listing> result = entityManager
            .createQuery("select listing from Listing listing left join fetch listing.locations where listing in :listings", Listing.class)
            .setParameter(LISTINGS_PARAMETER, listings)
            .getResultList();
        result.sort((o1, o2) -> Integer.compare(order.get(o1.getId()), order.get(o2.getId())));
        return result;
    }
}
