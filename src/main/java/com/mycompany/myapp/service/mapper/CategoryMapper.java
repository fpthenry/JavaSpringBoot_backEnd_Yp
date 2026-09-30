package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Category;
import com.mycompany.myapp.domain.Listing;
import com.mycompany.myapp.service.dto.CategoryDTO;
import com.mycompany.myapp.service.dto.ListingDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Category} and its DTO {@link CategoryDTO}.
 */
@Mapper(componentModel = "spring")
public interface CategoryMapper extends EntityMapper<CategoryDTO, Category> {
    @Mapping(target = "parent", source = "parent", qualifiedByName = "categoryName")
    @Mapping(target = "listingses", source = "listingses", qualifiedByName = "listingIdSet")
    CategoryDTO toDto(Category s);

    @Mapping(target = "listingses", ignore = true)
    @Mapping(target = "removeListings", ignore = true)
    Category toEntity(CategoryDTO categoryDTO);

    @Named("categoryName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    CategoryDTO toDtoCategoryName(Category category);

    @Named("listingId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    ListingDTO toDtoListingId(Listing listing);

    @Named("listingIdSet")
    default Set<ListingDTO> toDtoListingIdSet(Set<Listing> listing) {
        return listing.stream().map(this::toDtoListingId).collect(Collectors.toSet());
    }
}
