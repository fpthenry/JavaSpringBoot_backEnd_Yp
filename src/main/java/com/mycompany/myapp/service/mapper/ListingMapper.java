package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Category;
import com.mycompany.myapp.domain.Listing;
import com.mycompany.myapp.domain.Location;
import com.mycompany.myapp.service.dto.CategoryDTO;
import com.mycompany.myapp.service.dto.ListingDTO;
import com.mycompany.myapp.service.dto.LocationDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Listing} and its DTO {@link ListingDTO}.
 */
@Mapper(componentModel = "spring")
public interface ListingMapper extends EntityMapper<ListingDTO, Listing> {
    @Mapping(target = "categories", source = "categories", qualifiedByName = "categoryNameSet")
    @Mapping(target = "locations", source = "locations", qualifiedByName = "locationNameSet")
    ListingDTO toDto(Listing s);

    @Mapping(target = "removeCategory", ignore = true)
    @Mapping(target = "removeLocation", ignore = true)
    Listing toEntity(ListingDTO listingDTO);

    @Named("categoryName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    CategoryDTO toDtoCategoryName(Category category);

    @Named("categoryNameSet")
    default Set<CategoryDTO> toDtoCategoryNameSet(Set<Category> category) {
        return category.stream().map(this::toDtoCategoryName).collect(Collectors.toSet());
    }

    @Named("locationName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    LocationDTO toDtoLocationName(Location location);

    @Named("locationNameSet")
    default Set<LocationDTO> toDtoLocationNameSet(Set<Location> location) {
        return location.stream().map(this::toDtoLocationName).collect(Collectors.toSet());
    }
}
