package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Listing;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface ListingRepositoryWithBagRelationships {
    Optional<Listing> fetchBagRelationships(Optional<Listing> listing);

    List<Listing> fetchBagRelationships(List<Listing> listings);

    Page<Listing> fetchBagRelationships(Page<Listing> listings);
}
