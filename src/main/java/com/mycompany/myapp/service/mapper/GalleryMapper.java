package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Gallery;
import com.mycompany.myapp.service.dto.GalleryDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Gallery} and its DTO {@link GalleryDTO}.
 */
@Mapper(componentModel = "spring")
public interface GalleryMapper extends EntityMapper<GalleryDTO, Gallery> {}
