package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.BlogPost;
import com.mycompany.myapp.service.dto.BlogPostDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link BlogPost} and its DTO {@link BlogPostDTO}.
 */
@Mapper(componentModel = "spring")
public interface BlogPostMapper extends EntityMapper<BlogPostDTO, BlogPost> {}
