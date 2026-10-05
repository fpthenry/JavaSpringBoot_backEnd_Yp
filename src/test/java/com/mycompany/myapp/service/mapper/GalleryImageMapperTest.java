package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.GalleryImageAsserts.*;
import static com.mycompany.myapp.domain.GalleryImageTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GalleryImageMapperTest {

    private GalleryImageMapper galleryImageMapper;

    @BeforeEach
    void setUp() {
        galleryImageMapper = new GalleryImageMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getGalleryImageSample1();
        var actual = galleryImageMapper.toEntity(galleryImageMapper.toDto(expected));
        assertGalleryImageAllPropertiesEquals(expected, actual);
    }
}
