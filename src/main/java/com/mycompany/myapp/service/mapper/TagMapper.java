package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.BlogPost;
import com.mycompany.myapp.domain.Tag;
import com.mycompany.myapp.service.dto.BlogPostDTO;
import com.mycompany.myapp.service.dto.TagDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Tag} and its DTO {@link TagDTO}.
 */
@Mapper(componentModel = "spring")
public interface TagMapper extends EntityMapper<TagDTO, Tag> {
    // Sửa tay (JHipster luôn sinh chiều ngược của ManyToMany): không tải mọi bài viết của mục này vào DTO.
    // Lọc bài viết theo mục: /api/blog-posts?tagId.equals=...
    @Mapping(target = "blogPosts", ignore = true)
    TagDTO toDto(Tag s);

    @Mapping(target = "blogPosts", ignore = true)
    @Mapping(target = "removeBlogPost", ignore = true)
    Tag toEntity(TagDTO tagDTO);

    // Sửa tay: PATCH không được đụng tới blogPosts
    @Override
    @Named("partialUpdate")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "blogPosts", ignore = true)
    @Mapping(target = "removeBlogPost", ignore = true)
    void partialUpdate(@MappingTarget Tag entity, TagDTO dto);

    @Named("blogPostId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    BlogPostDTO toDtoBlogPostId(BlogPost blogPost);

    @Named("blogPostIdSet")
    default Set<BlogPostDTO> toDtoBlogPostIdSet(Set<BlogPost> blogPost) {
        return blogPost.stream().map(this::toDtoBlogPostId).collect(Collectors.toSet());
    }
}
