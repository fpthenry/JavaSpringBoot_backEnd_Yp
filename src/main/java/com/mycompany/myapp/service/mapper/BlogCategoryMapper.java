package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.BlogCategory;
import com.mycompany.myapp.domain.BlogPost;
import com.mycompany.myapp.service.dto.BlogCategoryDTO;
import com.mycompany.myapp.service.dto.BlogPostDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link BlogCategory} and its DTO {@link BlogCategoryDTO}.
 */
@Mapper(componentModel = "spring")
public interface BlogCategoryMapper extends EntityMapper<BlogCategoryDTO, BlogCategory> {
    @Mapping(target = "parent", source = "parent", qualifiedByName = "blogCategoryName")
    // Sửa tay (JHipster luôn sinh chiều ngược của ManyToMany): không tải mọi bài viết của mục này vào DTO.
    // Lọc bài viết theo mục: /api/blog-posts?categoryId.equals=...
    @Mapping(target = "blogPosts", ignore = true)
    BlogCategoryDTO toDto(BlogCategory s);

    @Mapping(target = "blogPosts", ignore = true)
    @Mapping(target = "removeBlogPost", ignore = true)
    BlogCategory toEntity(BlogCategoryDTO blogCategoryDTO);

    // Sửa tay: PATCH không được đụng tới blogPosts
    @Override
    @Named("partialUpdate")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "blogPosts", ignore = true)
    @Mapping(target = "removeBlogPost", ignore = true)
    void partialUpdate(@MappingTarget BlogCategory entity, BlogCategoryDTO dto);

    @Named("blogCategoryName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    BlogCategoryDTO toDtoBlogCategoryName(BlogCategory blogCategory);

    @Named("blogPostId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    BlogPostDTO toDtoBlogPostId(BlogPost blogPost);

    @Named("blogPostIdSet")
    default Set<BlogPostDTO> toDtoBlogPostIdSet(Set<BlogPost> blogPost) {
        return blogPost.stream().map(this::toDtoBlogPostId).collect(Collectors.toSet());
    }
}
