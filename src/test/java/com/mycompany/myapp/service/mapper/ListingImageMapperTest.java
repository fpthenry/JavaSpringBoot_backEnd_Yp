package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.ListingImageAsserts.*;
import static com.mycompany.myapp.domain.ListingImageTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListingImageMapperTest {

    private ListingImageMapper listingImageMapper;

    @BeforeEach
    void setUp() {
        listingImageMapper = new ListingImageMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getListingImageSample1();
        var actual = listingImageMapper.toEntity(listingImageMapper.toDto(expected));
        assertListingImageAllPropertiesEquals(expected, actual);
    }
}
