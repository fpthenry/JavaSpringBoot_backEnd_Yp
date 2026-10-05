package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.GalleryAsserts.*;
import static com.mycompany.myapp.domain.GalleryTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GalleryMapperTest {

    private GalleryMapper galleryMapper;

    @BeforeEach
    void setUp() {
        galleryMapper = new GalleryMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getGallerySample1();
        var actual = galleryMapper.toEntity(galleryMapper.toDto(expected));
        assertGalleryAllPropertiesEquals(expected, actual);
    }
}
