package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Gallery;
import com.mycompany.myapp.domain.Listing;
import com.mycompany.myapp.domain.ListingImage;
import com.mycompany.myapp.service.dto.GalleryDTO;
import com.mycompany.myapp.service.dto.ListingDTO;
import com.mycompany.myapp.service.dto.ListingImageDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ListingImage} and its DTO {@link ListingImageDTO}.
 */
@Mapper(componentModel = "spring")
public interface ListingImageMapper extends EntityMapper<ListingImageDTO, ListingImage> {
    @Mapping(target = "listing", source = "listing", qualifiedByName = "listingTitle")
    @Mapping(target = "gallery", source = "gallery", qualifiedByName = "galleryTitle")
    ListingImageDTO toDto(ListingImage s);

    @Named("listingTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    ListingDTO toDtoListingTitle(Listing listing);

    @Named("galleryTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    GalleryDTO toDtoGalleryTitle(Gallery gallery);
}
