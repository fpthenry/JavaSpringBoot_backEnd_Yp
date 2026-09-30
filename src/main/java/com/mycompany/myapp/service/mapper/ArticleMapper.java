package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.Article;
import com.mycompany.myapp.domain.ArticleCategory;
import com.mycompany.myapp.domain.ArticleTag;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.service.dto.ArticleCategoryDTO;
import com.mycompany.myapp.service.dto.ArticleDTO;
import com.mycompany.myapp.service.dto.ArticleTagDTO;
import com.mycompany.myapp.service.dto.UserDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Article} and its DTO {@link ArticleDTO}.
 */
@Mapper(componentModel = "spring")
public interface ArticleMapper extends EntityMapper<ArticleDTO, Article> {
    @Mapping(target = "author", source = "author", qualifiedByName = "userLogin")
    @Mapping(target = "categorieses", source = "categorieses", qualifiedByName = "articleCategoryNameSet")
    @Mapping(target = "tagses", source = "tagses", qualifiedByName = "articleTagNameSet")
    ArticleDTO toDto(Article s);

    @Mapping(target = "removeCategories", ignore = true)
    @Mapping(target = "removeTags", ignore = true)
    Article toEntity(ArticleDTO articleDTO);

    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);

    @Named("articleCategoryName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    ArticleCategoryDTO toDtoArticleCategoryName(ArticleCategory articleCategory);

    @Named("articleCategoryNameSet")
    default Set<ArticleCategoryDTO> toDtoArticleCategoryNameSet(Set<ArticleCategory> articleCategory) {
        return articleCategory.stream().map(this::toDtoArticleCategoryName).collect(Collectors.toSet());
    }

    @Named("articleTagName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    ArticleTagDTO toDtoArticleTagName(ArticleTag articleTag);

    @Named("articleTagNameSet")
    default Set<ArticleTagDTO> toDtoArticleTagNameSet(Set<ArticleTag> articleTag) {
        return articleTag.stream().map(this::toDtoArticleTagName).collect(Collectors.toSet());
    }
}
