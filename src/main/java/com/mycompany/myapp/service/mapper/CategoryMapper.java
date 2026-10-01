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
    // Sửa tay (JHipster luôn sinh chiều ngược của ManyToMany): một ngành có hàng trăm nghìn listing,
    // map listings làm /api/categories treo. Lọc listing theo ngành: /api/listings?categoryId.equals=...
    @Mapping(target = "listings", ignore = true)
    CategoryDTO toDto(Category s);

    @Mapping(target = "listings", ignore = true)
    @Mapping(target = "removeListing", ignore = true)
    Category toEntity(CategoryDTO categoryDTO);

    // Sửa tay: PATCH không được đụng tới listings (tránh tải toàn bộ listing của ngành)
    @Override
    @Named("partialUpdate")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "listings", ignore = true)
    @Mapping(target = "removeListing", ignore = true)
    void partialUpdate(@MappingTarget Category entity, CategoryDTO dto);

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
