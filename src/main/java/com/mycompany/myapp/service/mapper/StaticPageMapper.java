package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.StaticPage;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.service.dto.StaticPageDTO;
import com.mycompany.myapp.service.dto.UserDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link StaticPage} and its DTO {@link StaticPageDTO}.
 */
@Mapper(componentModel = "spring")
public interface StaticPageMapper extends EntityMapper<StaticPageDTO, StaticPage> {
    @Mapping(target = "author", source = "author", qualifiedByName = "userLogin")
    @Mapping(target = "parent", source = "parent", qualifiedByName = "staticPageTitle")
    StaticPageDTO toDto(StaticPage s);

    @Named("staticPageTitle")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    StaticPageDTO toDtoStaticPageTitle(StaticPage staticPage);

    @Named("userLogin")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "login", source = "login")
    UserDTO toDtoUserLogin(User user);
}
