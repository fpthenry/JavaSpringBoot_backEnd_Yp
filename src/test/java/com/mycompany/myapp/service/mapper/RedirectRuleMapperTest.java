package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.RedirectRuleAsserts.*;
import static com.mycompany.myapp.domain.RedirectRuleTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RedirectRuleMapperTest {

    private RedirectRuleMapper redirectRuleMapper;

    @BeforeEach
    void setUp() {
        redirectRuleMapper = new RedirectRuleMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getRedirectRuleSample1();
        var actual = redirectRuleMapper.toEntity(redirectRuleMapper.toDto(expected));
        assertRedirectRuleAllPropertiesEquals(expected, actual);
    }
}
