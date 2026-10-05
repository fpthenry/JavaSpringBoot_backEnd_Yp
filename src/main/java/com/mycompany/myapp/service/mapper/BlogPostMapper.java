package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.BlogCategory;
import com.mycompany.myapp.domain.BlogPost;
import com.mycompany.myapp.domain.Tag;
import com.mycompany.myapp.service.dto.BlogCategoryDTO;
import com.mycompany.myapp.service.dto.BlogPostDTO;
import com.mycompany.myapp.service.dto.TagDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link BlogPost} and its DTO {@link BlogPostDTO}.
 */
@Mapper(componentModel = "spring")
public interface BlogPostMapper extends EntityMapper<BlogPostDTO, BlogPost> {
    @Mapping(target = "categories", source = "categories", qualifiedByName = "blogCategoryNameSet")
    @Mapping(target = "tags", source = "tags", qualifiedByName = "tagNameSet")
    BlogPostDTO toDto(BlogPost s);

    @Mapping(target = "removeCategory", ignore = true)
    @Mapping(target = "removeTag", ignore = true)
    BlogPost toEntity(BlogPostDTO blogPostDTO);

    @Named("blogCategoryName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    BlogCategoryDTO toDtoBlogCategoryName(BlogCategory blogCategory);

    @Named("blogCategoryNameSet")
    default Set<BlogCategoryDTO> toDtoBlogCategoryNameSet(Set<BlogCategory> blogCategory) {
        return blogCategory.stream().map(this::toDtoBlogCategoryName).collect(Collectors.toSet());
    }

    @Named("tagName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    TagDTO toDtoTagName(Tag tag);

    @Named("tagNameSet")
    default Set<TagDTO> toDtoTagNameSet(Set<Tag> tag) {
        return tag.stream().map(this::toDtoTagName).collect(Collectors.toSet());
    }
}
