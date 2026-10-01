package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Listing;
import com.mycompany.myapp.domain.Location;
import com.mycompany.myapp.service.dto.ListingDTO;
import com.mycompany.myapp.service.dto.LocationDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Location} and its DTO {@link LocationDTO}.
 */
@Mapper(componentModel = "spring")
public interface LocationMapper extends EntityMapper<LocationDTO, Location> {
    @Mapping(target = "parent", source = "parent", qualifiedByName = "locationName")
    @Mapping(target = "listings", source = "listings", qualifiedByName = "listingIdSet")
    LocationDTO toDto(Location s);

    @Mapping(target = "listings", ignore = true)
    @Mapping(target = "removeListing", ignore = true)
    Location toEntity(LocationDTO locationDTO);

    @Named("locationName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    LocationDTO toDtoLocationName(Location location);

    @Named("listingId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    ListingDTO toDtoListingId(Listing listing);

    @Named("listingIdSet")
    default Set<ListingDTO> toDtoListingIdSet(Set<Listing> listing) {
        return listing.stream().map(this::toDtoListingId).collect(Collectors.toSet());
    }
}
