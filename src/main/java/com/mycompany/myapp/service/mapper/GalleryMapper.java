package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Gallery;
import com.mycompany.myapp.domain.Listing;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.service.dto.GalleryDTO;
import com.mycompany.myapp.service.dto.ListingDTO;
import com.mycompany.myapp.service.dto.UserDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Gallery} and its DTO {@link GalleryDTO}.
 */
@Mapper(componentModel = "spring")
public interface GalleryMapper extends EntityMapper<GalleryDTO, Gallery> {
    @Mapping(target = "author", source = "author", qualifiedByName = "userLogin")
    @Mapping(target = "listing", source = "listing", qualifiedByName = "listingTitle")
    GalleryDTO toDto(Gallery s);

    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);

    @Named("listingTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    ListingDTO toDtoListingTitle(Listing listing);
}
