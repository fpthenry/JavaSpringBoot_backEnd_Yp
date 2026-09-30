package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.LocationAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Listing;
import com.mycompany.myapp.domain.Location;
import com.mycompany.myapp.domain.Location;
import com.mycompany.myapp.repository.LocationRepository;
import com.mycompany.myapp.repository.search.LocationSearchRepository;
import com.mycompany.myapp.service.LocationService;
import com.mycompany.myapp.service.dto.LocationDTO;
import com.mycompany.myapp.service.mapper.LocationMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.assertj.core.util.IterableUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Streamable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link LocationResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class LocationResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_SLUG = "AAAAAAAAAA";
    private static final String UPDATED_SLUG = "BBBBBBBBBB";

    private static final String DEFAULT_TYPE = "AAAAAAAAAA";
    private static final String UPDATED_TYPE = "BBBBBBBBBB";

    private static final String DEFAULT_PROVINCE_CODE = "AAAAAAAAAA";
    private static final String UPDATED_PROVINCE_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_DISTRICT_CODE = "AAAAAAAAAA";
    private static final String UPDATED_DISTRICT_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_WARD_CODE = "AAAAAAAAAA";
    private static final String UPDATED_WARD_CODE = "BBBBBBBBBB";

    private static final Integer DEFAULT_COUNT = 0;
    private static final Integer UPDATED_COUNT = 1;
    private static final Integer SMALLER_COUNT = 0 - 1;

    private static final Integer DEFAULT_DISPLAY_ORDER = 0;
    private static final Integer UPDATED_DISPLAY_ORDER = 1;
    private static final Integer SMALLER_DISPLAY_ORDER = 0 - 1;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1790760789812L);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.ofEpochMilli(1790760789812L);

    private static final String ENTITY_API_URL = "/api/locations";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/locations/_search";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private LocationRepository locationRepository;

    @Mock
    private LocationRepository locationRepositoryMock;

    @Autowired
    private LocationMapper locationMapper;

    @Mock
    private LocationService locationServiceMock;

    @Autowired
    private LocationSearchRepository locationSearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restLocationMockMvc;

    private Location location;

    private Location insertedLocation;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Location createEntity() {
        return new Location()
            .name(DEFAULT_NAME)
            .slug(DEFAULT_SLUG)
            .type(DEFAULT_TYPE)
            .provinceCode(DEFAULT_PROVINCE_CODE)
            .districtCode(DEFAULT_DISTRICT_CODE)
            .wardCode(DEFAULT_WARD_CODE)
            .count(DEFAULT_COUNT)
            .displayOrder(DEFAULT_DISPLAY_ORDER)
            .createdAt(DEFAULT_CREATED_AT)
            .updatedAt(DEFAULT_UPDATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Location createUpdatedEntity() {
        return new Location()
            .name(UPDATED_NAME)
            .slug(UPDATED_SLUG)
            .type(UPDATED_TYPE)
            .provinceCode(UPDATED_PROVINCE_CODE)
            .districtCode(UPDATED_DISTRICT_CODE)
            .wardCode(UPDATED_WARD_CODE)
            .count(UPDATED_COUNT)
            .displayOrder(UPDATED_DISPLAY_ORDER)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
    }

    @BeforeEach
    void initTest() {
        location = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedLocation != null) {
            locationRepository.delete(insertedLocation);
            locationSearchRepository.delete(insertedLocation);
            insertedLocation = null;
        }
    }

    @Test
    @Transactional
    void createLocation() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(locationSearchRepository.findAll());
        // Create the Location
        LocationDTO locationDTO = locationMapper.toDto(location);
        var returnedLocationDTO = om.readValue(
            restLocationMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(locationDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            LocationDTO.class
        );

        // Validate the Location in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedLocation = locationMapper.toEntity(returnedLocationDTO);
        assertLocationUpdatableFieldsEquals(returnedLocation, getPersistedLocation(returnedLocation));

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(locationSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore + 1);
            });

        insertedLocation = returnedLocation;
    }

    @Test
    @Transactional
    void createLocationWithExistingId() throws Exception {
        // Create the Location with an existing ID
        location.setId(1L);
        LocationDTO locationDTO = locationMapper.toDto(location);

        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(locationSearchRepository.findAll());

        // An entity with an existing ID cannot be created, so this API call must fail
        restLocationMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(locationDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Location in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(locationSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(locationSearchRepository.findAll());
        // set the field null
        location.setName(null);

        // Create the Location, which fails.
        LocationDTO locationDTO = locationMapper.toDto(location);

        restLocationMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(locationDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(locationSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkSlugIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(locationSearchRepository.findAll());
        // set the field null
        location.setSlug(null);

        // Create the Location, which fails.
        LocationDTO locationDTO = locationMapper.toDto(location);

        restLocationMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(locationDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(locationSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void getAllLocations() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList
        restLocationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(location.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE)))
            .andExpect(jsonPath("$.[*].provinceCode").value(hasItem(DEFAULT_PROVINCE_CODE)))
            .andExpect(jsonPath("$.[*].districtCode").value(hasItem(DEFAULT_DISTRICT_CODE)))
            .andExpect(jsonPath("$.[*].wardCode").value(hasItem(DEFAULT_WARD_CODE)))
            .andExpect(jsonPath("$.[*].count").value(hasItem(DEFAULT_COUNT)))
            .andExpect(jsonPath("$.[*].displayOrder").value(hasItem(DEFAULT_DISPLAY_ORDER)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllLocationsWithEagerRelationshipsIsEnabled() throws Exception {
        when(locationServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restLocationMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(locationServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllLocationsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(locationServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restLocationMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(locationRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getLocation() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get the location
        restLocationMockMvc
            .perform(get(ENTITY_API_URL_ID, location.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(location.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.slug").value(DEFAULT_SLUG))
            .andExpect(jsonPath("$.type").value(DEFAULT_TYPE))
            .andExpect(jsonPath("$.provinceCode").value(DEFAULT_PROVINCE_CODE))
            .andExpect(jsonPath("$.districtCode").value(DEFAULT_DISTRICT_CODE))
            .andExpect(jsonPath("$.wardCode").value(DEFAULT_WARD_CODE))
            .andExpect(jsonPath("$.count").value(DEFAULT_COUNT))
            .andExpect(jsonPath("$.displayOrder").value(DEFAULT_DISPLAY_ORDER))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()));
    }

    @Test
    @Transactional
    void getLocationsByIdFiltering() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        Long id = location.getId();

        defaultLocationFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultLocationFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultLocationFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllLocationsByNameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where name equals to
        defaultLocationFiltering("name.equals=" + DEFAULT_NAME, "name.equals=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllLocationsByNameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where name in
        defaultLocationFiltering("name.in=" + DEFAULT_NAME + "," + UPDATED_NAME, "name.in=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllLocationsByNameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where name is not null
        defaultLocationFiltering("name.specified=true", "name.specified=false");
    }

    @Test
    @Transactional
    void getAllLocationsByNameContainsSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where name contains
        defaultLocationFiltering("name.contains=" + DEFAULT_NAME, "name.contains=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllLocationsByNameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where name does not contain
        defaultLocationFiltering("name.doesNotContain=" + UPDATED_NAME, "name.doesNotContain=" + DEFAULT_NAME);
    }

    @Test
    @Transactional
    void getAllLocationsBySlugIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where slug equals to
        defaultLocationFiltering("slug.equals=" + DEFAULT_SLUG, "slug.equals=" + UPDATED_SLUG);
    }

    @Test
    @Transactional
    void getAllLocationsBySlugIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where slug in
        defaultLocationFiltering("slug.in=" + DEFAULT_SLUG + "," + UPDATED_SLUG, "slug.in=" + UPDATED_SLUG);
    }

    @Test
    @Transactional
    void getAllLocationsBySlugIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where slug is not null
        defaultLocationFiltering("slug.specified=true", "slug.specified=false");
    }

    @Test
    @Transactional
    void getAllLocationsBySlugContainsSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where slug contains
        defaultLocationFiltering("slug.contains=" + DEFAULT_SLUG, "slug.contains=" + UPDATED_SLUG);
    }

    @Test
    @Transactional
    void getAllLocationsBySlugNotContainsSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where slug does not contain
        defaultLocationFiltering("slug.doesNotContain=" + UPDATED_SLUG, "slug.doesNotContain=" + DEFAULT_SLUG);
    }

    @Test
    @Transactional
    void getAllLocationsByTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where type equals to
        defaultLocationFiltering("type.equals=" + DEFAULT_TYPE, "type.equals=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllLocationsByTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where type in
        defaultLocationFiltering("type.in=" + DEFAULT_TYPE + "," + UPDATED_TYPE, "type.in=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllLocationsByTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where type is not null
        defaultLocationFiltering("type.specified=true", "type.specified=false");
    }

    @Test
    @Transactional
    void getAllLocationsByTypeContainsSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where type contains
        defaultLocationFiltering("type.contains=" + DEFAULT_TYPE, "type.contains=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllLocationsByTypeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where type does not contain
        defaultLocationFiltering("type.doesNotContain=" + UPDATED_TYPE, "type.doesNotContain=" + DEFAULT_TYPE);
    }

    @Test
    @Transactional
    void getAllLocationsByProvinceCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where provinceCode equals to
        defaultLocationFiltering("provinceCode.equals=" + DEFAULT_PROVINCE_CODE, "provinceCode.equals=" + UPDATED_PROVINCE_CODE);
    }

    @Test
    @Transactional
    void getAllLocationsByProvinceCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where provinceCode in
        defaultLocationFiltering(
            "provinceCode.in=" + DEFAULT_PROVINCE_CODE + "," + UPDATED_PROVINCE_CODE,
            "provinceCode.in=" + UPDATED_PROVINCE_CODE
        );
    }

    @Test
    @Transactional
    void getAllLocationsByProvinceCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where provinceCode is not null
        defaultLocationFiltering("provinceCode.specified=true", "provinceCode.specified=false");
    }

    @Test
    @Transactional
    void getAllLocationsByProvinceCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where provinceCode contains
        defaultLocationFiltering("provinceCode.contains=" + DEFAULT_PROVINCE_CODE, "provinceCode.contains=" + UPDATED_PROVINCE_CODE);
    }

    @Test
    @Transactional
    void getAllLocationsByProvinceCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where provinceCode does not contain
        defaultLocationFiltering(
            "provinceCode.doesNotContain=" + UPDATED_PROVINCE_CODE,
            "provinceCode.doesNotContain=" + DEFAULT_PROVINCE_CODE
        );
    }

    @Test
    @Transactional
    void getAllLocationsByDistrictCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where districtCode equals to
        defaultLocationFiltering("districtCode.equals=" + DEFAULT_DISTRICT_CODE, "districtCode.equals=" + UPDATED_DISTRICT_CODE);
    }

    @Test
    @Transactional
    void getAllLocationsByDistrictCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where districtCode in
        defaultLocationFiltering(
            "districtCode.in=" + DEFAULT_DISTRICT_CODE + "," + UPDATED_DISTRICT_CODE,
            "districtCode.in=" + UPDATED_DISTRICT_CODE
        );
    }

    @Test
    @Transactional
    void getAllLocationsByDistrictCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where districtCode is not null
        defaultLocationFiltering("districtCode.specified=true", "districtCode.specified=false");
    }

    @Test
    @Transactional
    void getAllLocationsByDistrictCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where districtCode contains
        defaultLocationFiltering("districtCode.contains=" + DEFAULT_DISTRICT_CODE, "districtCode.contains=" + UPDATED_DISTRICT_CODE);
    }

    @Test
    @Transactional
    void getAllLocationsByDistrictCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where districtCode does not contain
        defaultLocationFiltering(
            "districtCode.doesNotContain=" + UPDATED_DISTRICT_CODE,
            "districtCode.doesNotContain=" + DEFAULT_DISTRICT_CODE
        );
    }

    @Test
    @Transactional
    void getAllLocationsByWardCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where wardCode equals to
        defaultLocationFiltering("wardCode.equals=" + DEFAULT_WARD_CODE, "wardCode.equals=" + UPDATED_WARD_CODE);
    }

    @Test
    @Transactional
    void getAllLocationsByWardCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where wardCode in
        defaultLocationFiltering("wardCode.in=" + DEFAULT_WARD_CODE + "," + UPDATED_WARD_CODE, "wardCode.in=" + UPDATED_WARD_CODE);
    }

    @Test
    @Transactional
    void getAllLocationsByWardCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where wardCode is not null
        defaultLocationFiltering("wardCode.specified=true", "wardCode.specified=false");
    }

    @Test
    @Transactional
    void getAllLocationsByWardCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where wardCode contains
        defaultLocationFiltering("wardCode.contains=" + DEFAULT_WARD_CODE, "wardCode.contains=" + UPDATED_WARD_CODE);
    }

    @Test
    @Transactional
    void getAllLocationsByWardCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where wardCode does not contain
        defaultLocationFiltering("wardCode.doesNotContain=" + UPDATED_WARD_CODE, "wardCode.doesNotContain=" + DEFAULT_WARD_CODE);
    }

    @Test
    @Transactional
    void getAllLocationsByCountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where count equals to
        defaultLocationFiltering("count.equals=" + DEFAULT_COUNT, "count.equals=" + UPDATED_COUNT);
    }

    @Test
    @Transactional
    void getAllLocationsByCountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where count in
        defaultLocationFiltering("count.in=" + DEFAULT_COUNT + "," + UPDATED_COUNT, "count.in=" + UPDATED_COUNT);
    }

    @Test
    @Transactional
    void getAllLocationsByCountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where count is not null
        defaultLocationFiltering("count.specified=true", "count.specified=false");
    }

    @Test
    @Transactional
    void getAllLocationsByCountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where count is greater than or equal to
        defaultLocationFiltering("count.greaterThanOrEqual=" + DEFAULT_COUNT, "count.greaterThanOrEqual=" + UPDATED_COUNT);
    }

    @Test
    @Transactional
    void getAllLocationsByCountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where count is less than or equal to
        defaultLocationFiltering("count.lessThanOrEqual=" + DEFAULT_COUNT, "count.lessThanOrEqual=" + SMALLER_COUNT);
    }

    @Test
    @Transactional
    void getAllLocationsByCountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where count is less than
        defaultLocationFiltering("count.lessThan=" + UPDATED_COUNT, "count.lessThan=" + DEFAULT_COUNT);
    }

    @Test
    @Transactional
    void getAllLocationsByCountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where count is greater than
        defaultLocationFiltering("count.greaterThan=" + SMALLER_COUNT, "count.greaterThan=" + DEFAULT_COUNT);
    }

    @Test
    @Transactional
    void getAllLocationsByDisplayOrderIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where displayOrder equals to
        defaultLocationFiltering("displayOrder.equals=" + DEFAULT_DISPLAY_ORDER, "displayOrder.equals=" + UPDATED_DISPLAY_ORDER);
    }

    @Test
    @Transactional
    void getAllLocationsByDisplayOrderIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where displayOrder in
        defaultLocationFiltering(
            "displayOrder.in=" + DEFAULT_DISPLAY_ORDER + "," + UPDATED_DISPLAY_ORDER,
            "displayOrder.in=" + UPDATED_DISPLAY_ORDER
        );
    }

    @Test
    @Transactional
    void getAllLocationsByDisplayOrderIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where displayOrder is not null
        defaultLocationFiltering("displayOrder.specified=true", "displayOrder.specified=false");
    }

    @Test
    @Transactional
    void getAllLocationsByDisplayOrderIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where displayOrder is greater than or equal to
        defaultLocationFiltering(
            "displayOrder.greaterThanOrEqual=" + DEFAULT_DISPLAY_ORDER,
            "displayOrder.greaterThanOrEqual=" + UPDATED_DISPLAY_ORDER
        );
    }

    @Test
    @Transactional
    void getAllLocationsByDisplayOrderIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where displayOrder is less than or equal to
        defaultLocationFiltering(
            "displayOrder.lessThanOrEqual=" + DEFAULT_DISPLAY_ORDER,
            "displayOrder.lessThanOrEqual=" + SMALLER_DISPLAY_ORDER
        );
    }

    @Test
    @Transactional
    void getAllLocationsByDisplayOrderIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where displayOrder is less than
        defaultLocationFiltering("displayOrder.lessThan=" + UPDATED_DISPLAY_ORDER, "displayOrder.lessThan=" + DEFAULT_DISPLAY_ORDER);
    }

    @Test
    @Transactional
    void getAllLocationsByDisplayOrderIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where displayOrder is greater than
        defaultLocationFiltering("displayOrder.greaterThan=" + SMALLER_DISPLAY_ORDER, "displayOrder.greaterThan=" + DEFAULT_DISPLAY_ORDER);
    }

    @Test
    @Transactional
    void getAllLocationsByCreatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where createdAt equals to
        defaultLocationFiltering("createdAt.equals=" + DEFAULT_CREATED_AT, "createdAt.equals=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllLocationsByCreatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where createdAt in
        defaultLocationFiltering("createdAt.in=" + DEFAULT_CREATED_AT + "," + UPDATED_CREATED_AT, "createdAt.in=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllLocationsByCreatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where createdAt is not null
        defaultLocationFiltering("createdAt.specified=true", "createdAt.specified=false");
    }

    @Test
    @Transactional
    void getAllLocationsByUpdatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where updatedAt equals to
        defaultLocationFiltering("updatedAt.equals=" + DEFAULT_UPDATED_AT, "updatedAt.equals=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllLocationsByUpdatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where updatedAt in
        defaultLocationFiltering("updatedAt.in=" + DEFAULT_UPDATED_AT + "," + UPDATED_UPDATED_AT, "updatedAt.in=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllLocationsByUpdatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        // Get all the locationList where updatedAt is not null
        defaultLocationFiltering("updatedAt.specified=true", "updatedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllLocationsByParentIsEqualToSomething() throws Exception {
        Location parent;
        if (TestUtil.findAll(em, Location.class).isEmpty()) {
            locationRepository.saveAndFlush(location);
            parent = LocationResourceIT.createEntity();
        } else {
            parent = TestUtil.findAll(em, Location.class).get(0);
        }
        em.persist(parent);
        em.flush();
        location.setParent(parent);
        locationRepository.saveAndFlush(location);
        Long parentId = parent.getId();
        // Get all the locationList where parent equals to parentId
        defaultLocationShouldBeFound("parentId.equals=" + parentId);

        // Get all the locationList where parent equals to (parentId + 1)
        defaultLocationShouldNotBeFound("parentId.equals=" + (parentId + 1));
    }

    @Test
    @Transactional
    void getAllLocationsByListingsIsEqualToSomething() throws Exception {
        Listing listings;
        if (TestUtil.findAll(em, Listing.class).isEmpty()) {
            locationRepository.saveAndFlush(location);
            listings = ListingResourceIT.createEntity();
        } else {
            listings = TestUtil.findAll(em, Listing.class).get(0);
        }
        em.persist(listings);
        em.flush();
        location.addListings(listings);
        locationRepository.saveAndFlush(location);
        Long listingsId = listings.getId();
        // Get all the locationList where listings equals to listingsId
        defaultLocationShouldBeFound("listingsId.equals=" + listingsId);

        // Get all the locationList where listings equals to (listingsId + 1)
        defaultLocationShouldNotBeFound("listingsId.equals=" + (listingsId + 1));
    }

    private void defaultLocationFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultLocationShouldBeFound(shouldBeFound);
        defaultLocationShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultLocationShouldBeFound(String filter) throws Exception {
        restLocationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(location.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE)))
            .andExpect(jsonPath("$.[*].provinceCode").value(hasItem(DEFAULT_PROVINCE_CODE)))
            .andExpect(jsonPath("$.[*].districtCode").value(hasItem(DEFAULT_DISTRICT_CODE)))
            .andExpect(jsonPath("$.[*].wardCode").value(hasItem(DEFAULT_WARD_CODE)))
            .andExpect(jsonPath("$.[*].count").value(hasItem(DEFAULT_COUNT)))
            .andExpect(jsonPath("$.[*].displayOrder").value(hasItem(DEFAULT_DISPLAY_ORDER)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));

        // Check, that the count call also returns 1
        restLocationMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultLocationShouldNotBeFound(String filter) throws Exception {
        restLocationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restLocationMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingLocation() throws Exception {
        // Get the location
        restLocationMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingLocation() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        long databaseSizeBeforeUpdate = getRepositoryCount();
        locationSearchRepository.save(location);
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(locationSearchRepository.findAll());

        // Update the location
        Location updatedLocation = locationRepository.findById(location.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedLocation are not directly saved in db
        em.detach(updatedLocation);
        updatedLocation
            .name(UPDATED_NAME)
            .slug(UPDATED_SLUG)
            .type(UPDATED_TYPE)
            .provinceCode(UPDATED_PROVINCE_CODE)
            .districtCode(UPDATED_DISTRICT_CODE)
            .wardCode(UPDATED_WARD_CODE)
            .count(UPDATED_COUNT)
            .displayOrder(UPDATED_DISPLAY_ORDER)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
        LocationDTO locationDTO = locationMapper.toDto(updatedLocation);

        restLocationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, locationDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(locationDTO))
            )
            .andExpect(status().isOk());

        // Validate the Location in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedLocationToMatchAllProperties(updatedLocation);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(locationSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
                List<Location> locationSearchList = Streamable.of(locationSearchRepository.findAll()).toList();
                Location testLocationSearch = locationSearchList.get(searchDatabaseSizeAfter - 1);

                assertLocationAllPropertiesEquals(testLocationSearch, updatedLocation);
            });
    }

    @Test
    @Transactional
    void putNonExistingLocation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(locationSearchRepository.findAll());
        location.setId(longCount.incrementAndGet());

        // Create the Location
        LocationDTO locationDTO = locationMapper.toDto(location);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restLocationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, locationDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(locationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Location in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(locationSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithIdMismatchLocation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(locationSearchRepository.findAll());
        location.setId(longCount.incrementAndGet());

        // Create the Location
        LocationDTO locationDTO = locationMapper.toDto(location);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLocationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(locationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Location in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(locationSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamLocation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(locationSearchRepository.findAll());
        location.setId(longCount.incrementAndGet());

        // Create the Location
        LocationDTO locationDTO = locationMapper.toDto(location);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLocationMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(locationDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Location in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(locationSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void partialUpdateLocationWithPatch() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the location using partial update
        Location partialUpdatedLocation = new Location();
        partialUpdatedLocation.setId(location.getId());

        partialUpdatedLocation.provinceCode(UPDATED_PROVINCE_CODE);

        restLocationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedLocation.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedLocation))
            )
            .andExpect(status().isOk());

        // Validate the Location in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertLocationUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedLocation, location), getPersistedLocation(location));
    }

    @Test
    @Transactional
    void fullUpdateLocationWithPatch() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the location using partial update
        Location partialUpdatedLocation = new Location();
        partialUpdatedLocation.setId(location.getId());

        partialUpdatedLocation
            .name(UPDATED_NAME)
            .slug(UPDATED_SLUG)
            .type(UPDATED_TYPE)
            .provinceCode(UPDATED_PROVINCE_CODE)
            .districtCode(UPDATED_DISTRICT_CODE)
            .wardCode(UPDATED_WARD_CODE)
            .count(UPDATED_COUNT)
            .displayOrder(UPDATED_DISPLAY_ORDER)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);

        restLocationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedLocation.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedLocation))
            )
            .andExpect(status().isOk());

        // Validate the Location in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertLocationUpdatableFieldsEquals(partialUpdatedLocation, getPersistedLocation(partialUpdatedLocation));
    }

    @Test
    @Transactional
    void patchNonExistingLocation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(locationSearchRepository.findAll());
        location.setId(longCount.incrementAndGet());

        // Create the Location
        LocationDTO locationDTO = locationMapper.toDto(location);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restLocationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, locationDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(locationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Location in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(locationSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithIdMismatchLocation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(locationSearchRepository.findAll());
        location.setId(longCount.incrementAndGet());

        // Create the Location
        LocationDTO locationDTO = locationMapper.toDto(location);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLocationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(locationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Location in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(locationSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamLocation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(locationSearchRepository.findAll());
        location.setId(longCount.incrementAndGet());

        // Create the Location
        LocationDTO locationDTO = locationMapper.toDto(location);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restLocationMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(locationDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Location in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(locationSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void deleteLocation() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);
        locationRepository.save(location);
        locationSearchRepository.save(location);

        long databaseSizeBeforeDelete = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(locationSearchRepository.findAll());
        assertThat(searchDatabaseSizeBefore).isEqualTo(databaseSizeBeforeDelete);

        // Delete the location
        restLocationMockMvc
            .perform(delete(ENTITY_API_URL_ID, location.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(locationSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore - 1);
    }

    @Test
    @Transactional
    void searchLocation() throws Exception {
        // Initialize the database
        insertedLocation = locationRepository.saveAndFlush(location);
        locationSearchRepository.save(location);

        // Search the location
        restLocationMockMvc
            .perform(get(ENTITY_SEARCH_API_URL + "?query=id:" + location.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(location.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE)))
            .andExpect(jsonPath("$.[*].provinceCode").value(hasItem(DEFAULT_PROVINCE_CODE)))
            .andExpect(jsonPath("$.[*].districtCode").value(hasItem(DEFAULT_DISTRICT_CODE)))
            .andExpect(jsonPath("$.[*].wardCode").value(hasItem(DEFAULT_WARD_CODE)))
            .andExpect(jsonPath("$.[*].count").value(hasItem(DEFAULT_COUNT)))
            .andExpect(jsonPath("$.[*].displayOrder").value(hasItem(DEFAULT_DISPLAY_ORDER)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    protected long getRepositoryCount() {
        return locationRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected Location getPersistedLocation(Location location) {
        return locationRepository.findById(location.getId()).orElseThrow();
    }

    protected void assertPersistedLocationToMatchAllProperties(Location expectedLocation) {
        assertLocationAllPropertiesEquals(expectedLocation, getPersistedLocation(expectedLocation));
    }

    protected void assertPersistedLocationToMatchUpdatableProperties(Location expectedLocation) {
        assertLocationAllUpdatablePropertiesEquals(expectedLocation, getPersistedLocation(expectedLocation));
    }
}
