package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Tag;
import com.mycompany.myapp.service.dto.TagDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Tag} and its DTO {@link TagDTO}.
 */
@Mapper(componentModel = "spring")
public interface TagMapper extends EntityMapper<TagDTO, Tag> {}
