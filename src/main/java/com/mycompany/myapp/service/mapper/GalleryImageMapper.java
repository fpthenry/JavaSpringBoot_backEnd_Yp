package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Gallery;
import com.mycompany.myapp.domain.GalleryImage;
import com.mycompany.myapp.service.dto.GalleryDTO;
import com.mycompany.myapp.service.dto.GalleryImageDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link GalleryImage} and its DTO {@link GalleryImageDTO}.
 */
@Mapper(componentModel = "spring")
public interface GalleryImageMapper extends EntityMapper<GalleryImageDTO, GalleryImage> {
    @Mapping(target = "gallery", source = "gallery", qualifiedByName = "galleryName")
    GalleryImageDTO toDto(GalleryImage s);

    @Named("galleryName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    GalleryDTO toDtoGalleryName(Gallery gallery);
}
