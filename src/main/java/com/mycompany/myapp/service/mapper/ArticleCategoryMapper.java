package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Article;
import com.mycompany.myapp.domain.ArticleCategory;
import com.mycompany.myapp.service.dto.ArticleCategoryDTO;
import com.mycompany.myapp.service.dto.ArticleDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ArticleCategory} and its DTO {@link ArticleCategoryDTO}.
 */
@Mapper(componentModel = "spring")
public interface ArticleCategoryMapper extends EntityMapper<ArticleCategoryDTO, ArticleCategory> {
    @Mapping(target = "parent", source = "parent", qualifiedByName = "articleCategoryName")
    @Mapping(target = "articleses", source = "articleses", qualifiedByName = "articleIdSet")
    ArticleCategoryDTO toDto(ArticleCategory s);

    @Mapping(target = "articleses", ignore = true)
    @Mapping(target = "removeArticles", ignore = true)
    ArticleCategory toEntity(ArticleCategoryDTO articleCategoryDTO);

    @Named("articleCategoryName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    ArticleCategoryDTO toDtoArticleCategoryName(ArticleCategory articleCategory);

    @Named("articleId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    ArticleDTO toDtoArticleId(Article article);

    @Named("articleIdSet")
    default Set<ArticleDTO> toDtoArticleIdSet(Set<Article> article) {
        return article.stream().map(this::toDtoArticleId).collect(Collectors.toSet());
    }
}
