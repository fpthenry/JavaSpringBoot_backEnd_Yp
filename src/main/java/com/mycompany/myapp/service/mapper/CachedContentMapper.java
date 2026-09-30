package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.CachedContent;
import com.mycompany.myapp.service.dto.CachedContentDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link CachedContent} and its DTO {@link CachedContentDTO}.
 */
@Mapper(componentModel = "spring")
public interface CachedContentMapper extends EntityMapper<CachedContentDTO, CachedContent> {}
