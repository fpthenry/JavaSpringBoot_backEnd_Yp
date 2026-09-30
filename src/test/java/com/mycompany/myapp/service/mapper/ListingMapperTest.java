package com.mycompany.myapp.service.mapper;

import static com.mycompany.myapp.domain.ListingAsserts.*;
import static com.mycompany.myapp.domain.ListingTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListingMapperTest {

    private ListingMapper listingMapper;

    @BeforeEach
    void setUp() {
        listingMapper = new ListingMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getListingSample1();
        var actual = listingMapper.toEntity(listingMapper.toDto(expected));
        assertListingAllPropertiesEquals(expected, actual);
    }
}
