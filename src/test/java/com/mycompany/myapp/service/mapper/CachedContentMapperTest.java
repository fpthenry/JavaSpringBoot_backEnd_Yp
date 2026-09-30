package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.CachedContentAsserts.*;
import static com.mycompany.myapp.domain.CachedContentTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CachedContentMapperTest {

    private CachedContentMapper cachedContentMapper;

    @BeforeEach
    void setUp() {
        cachedContentMapper = new CachedContentMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getCachedContentSample1();
        var actual = cachedContentMapper.toEntity(cachedContentMapper.toDto(expected));
        assertCachedContentAllPropertiesEquals(expected, actual);
    }
}
