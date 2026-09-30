package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Article;
import com.mycompany.myapp.domain.ArticleTag;
import com.mycompany.myapp.service.dto.ArticleDTO;
import com.mycompany.myapp.service.dto.ArticleTagDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ArticleTag} and its DTO {@link ArticleTagDTO}.
 */
@Mapper(componentModel = "spring")
public interface ArticleTagMapper extends EntityMapper<ArticleTagDTO, ArticleTag> {
    @Mapping(target = "articleses", source = "articleses", qualifiedByName = "articleIdSet")
    ArticleTagDTO toDto(ArticleTag s);

    @Mapping(target = "articleses", ignore = true)
    @Mapping(target = "removeArticles", ignore = true)
    ArticleTag toEntity(ArticleTagDTO articleTagDTO);

    @Named("articleId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    ArticleDTO toDtoArticleId(Article article);

    @Named("articleIdSet")
    default Set<ArticleDTO> toDtoArticleIdSet(Set<Article> article) {
        return article.stream().map(this::toDtoArticleId).collect(Collectors.toSet());
    }
}
