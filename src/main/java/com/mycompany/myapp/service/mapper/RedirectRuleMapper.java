package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.RedirectRule;
import com.mycompany.myapp.service.dto.RedirectRuleDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link RedirectRule} and its DTO {@link RedirectRuleDTO}.
 */
@Mapper(componentModel = "spring")
public interface RedirectRuleMapper extends EntityMapper<RedirectRuleDTO, RedirectRule> {}
