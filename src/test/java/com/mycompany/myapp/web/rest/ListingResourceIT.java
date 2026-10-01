package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.ListingAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Category;
import com.mycompany.myapp.domain.Listing;
import com.mycompany.myapp.domain.Location;
import com.mycompany.myapp.repository.ListingRepository;
import com.mycompany.myapp.repository.search.ListingSearchRepository;
import com.mycompany.myapp.service.ListingService;
import com.mycompany.myapp.service.dto.ListingDTO;
import com.mycompany.myapp.service.mapper.ListingMapper;
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
 * Integration tests for the {@link ListingResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ListingResourceIT {

    private static final Long DEFAULT_WP_ID = 1L;
    private static final Long UPDATED_WP_ID = 2L;
    private static final Long SMALLER_WP_ID = 1L - 1L;

    private static final String DEFAULT_API_ID = "AAAAAAAAAA";
    private static final String UPDATED_API_ID = "BBBBBBBBBB";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_NAME_EN = "AAAAAAAAAA";
    private static final String UPDATED_NAME_EN = "BBBBBBBBBB";

    private static final String DEFAULT_NAME_ALIAS = "AAAAAAAAAA";
    private static final String UPDATED_NAME_ALIAS = "BBBBBBBBBB";

    private static final String DEFAULT_SLUG = "AAAAAAAAAA";
    private static final String UPDATED_SLUG = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final String DEFAULT_PHONE = "AAAAAAAAAA";
    private static final String UPDATED_PHONE = "BBBBBBBBBB";

    private static final String DEFAULT_MOBILE = "AAAAAAAAAA";
    private static final String UPDATED_MOBILE = "BBBBBBBBBB";

    private static final String DEFAULT_EMAIL = "AAAAAAAAAA";
    private static final String UPDATED_EMAIL = "BBBBBBBBBB";

    private static final String DEFAULT_WEBSITE = "AAAAAAAAAA";
    private static final String UPDATED_WEBSITE = "BBBBBBBBBB";

    private static final String DEFAULT_ADDRESS = "AAAAAAAAAA";
    private static final String UPDATED_ADDRESS = "BBBBBBBBBB";

    private static final String DEFAULT_LOCATION_JSON = "AAAAAAAAAA";
    private static final String UPDATED_LOCATION_JSON = "BBBBBBBBBB";

    private static final String DEFAULT_TAX_CODE = "AAAAAAAAAA";
    private static final String UPDATED_TAX_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_REPRESENTATIVE = "AAAAAAAAAA";
    private static final String UPDATED_REPRESENTATIVE = "BBBBBBBBBB";

    private static final String DEFAULT_CAPITAL = "AAAAAAAAAA";
    private static final String UPDATED_CAPITAL = "BBBBBBBBBB";

    private static final String DEFAULT_FOUNDED_YEAR = "AAAAAAAAAA";
    private static final String UPDATED_FOUNDED_YEAR = "BBBBBBBBBB";

    private static final String DEFAULT_BUSINESS_TYPE = "AAAAAAAAAA";
    private static final String UPDATED_BUSINESS_TYPE = "BBBBBBBBBB";

    private static final String DEFAULT_BUSINESS_STATUS = "AAAAAAAAAA";
    private static final String UPDATED_BUSINESS_STATUS = "BBBBBBBBBB";

    private static final String DEFAULT_INDUSTRY_CODE = "AAAAAAAAAA";
    private static final String UPDATED_INDUSTRY_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_MANAGED_BY = "AAAAAAAAAA";
    private static final String UPDATED_MANAGED_BY = "BBBBBBBBBB";

    private static final String DEFAULT_THUMBNAIL = "AAAAAAAAAA";
    private static final String UPDATED_THUMBNAIL = "BBBBBBBBBB";

    private static final String DEFAULT_IMAGES = "AAAAAAAAAA";
    private static final String UPDATED_IMAGES = "BBBBBBBBBB";

    private static final Integer DEFAULT_VIEW_COUNT = 1;
    private static final Integer UPDATED_VIEW_COUNT = 2;
    private static final Integer SMALLER_VIEW_COUNT = 1 - 1;

    private static final Boolean DEFAULT_IS_FEATURED = false;
    private static final Boolean UPDATED_IS_FEATURED = true;

    private static final String DEFAULT_STATUS = "AAAAAAAAAA";
    private static final String UPDATED_STATUS = "BBBBBBBBBB";

    private static final Instant DEFAULT_PUBLISHED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_PUBLISHED_AT = Instant.ofEpochMilli(1790760789812L);

    private static final Instant DEFAULT_MODIFIED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_MODIFIED_AT = Instant.ofEpochMilli(1790760789812L);

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1790760789812L);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.ofEpochMilli(1790760789812L);

    private static final Boolean DEFAULT_ES_INDEXED = false;
    private static final Boolean UPDATED_ES_INDEXED = true;

    private static final String ENTITY_API_URL = "/api/listings";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/listings/_search";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ListingRepository listingRepository;

    @Mock
    private ListingRepository listingRepositoryMock;

    @Autowired
    private ListingMapper listingMapper;

    @Mock
    private ListingService listingServiceMock;

    @Autowired
    private ListingSearchRepository listingSearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restListingMockMvc;

    private Listing listing;

    private Listing insertedListing;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Listing createEntity() {
        return new Listing()
            .wpId(DEFAULT_WP_ID)
            .apiId(DEFAULT_API_ID)
            .name(DEFAULT_NAME)
            .nameEn(DEFAULT_NAME_EN)
            .nameAlias(DEFAULT_NAME_ALIAS)
            .slug(DEFAULT_SLUG)
            .description(DEFAULT_DESCRIPTION)
            .phone(DEFAULT_PHONE)
            .mobile(DEFAULT_MOBILE)
            .email(DEFAULT_EMAIL)
            .website(DEFAULT_WEBSITE)
            .address(DEFAULT_ADDRESS)
            .locationJson(DEFAULT_LOCATION_JSON)
            .taxCode(DEFAULT_TAX_CODE)
            .representative(DEFAULT_REPRESENTATIVE)
            .capital(DEFAULT_CAPITAL)
            .foundedYear(DEFAULT_FOUNDED_YEAR)
            .businessType(DEFAULT_BUSINESS_TYPE)
            .businessStatus(DEFAULT_BUSINESS_STATUS)
            .industryCode(DEFAULT_INDUSTRY_CODE)
            .managedBy(DEFAULT_MANAGED_BY)
            .thumbnail(DEFAULT_THUMBNAIL)
            .images(DEFAULT_IMAGES)
            .viewCount(DEFAULT_VIEW_COUNT)
            .isFeatured(DEFAULT_IS_FEATURED)
            .status(DEFAULT_STATUS)
            .publishedAt(DEFAULT_PUBLISHED_AT)
            .modifiedAt(DEFAULT_MODIFIED_AT)
            .createdAt(DEFAULT_CREATED_AT)
            .updatedAt(DEFAULT_UPDATED_AT)
            .esIndexed(DEFAULT_ES_INDEXED);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Listing createUpdatedEntity() {
        return new Listing()
            .wpId(UPDATED_WP_ID)
            .apiId(UPDATED_API_ID)
            .name(UPDATED_NAME)
            .nameEn(UPDATED_NAME_EN)
            .nameAlias(UPDATED_NAME_ALIAS)
            .slug(UPDATED_SLUG)
            .description(UPDATED_DESCRIPTION)
            .phone(UPDATED_PHONE)
            .mobile(UPDATED_MOBILE)
            .email(UPDATED_EMAIL)
            .website(UPDATED_WEBSITE)
            .address(UPDATED_ADDRESS)
            .locationJson(UPDATED_LOCATION_JSON)
            .taxCode(UPDATED_TAX_CODE)
            .representative(UPDATED_REPRESENTATIVE)
            .capital(UPDATED_CAPITAL)
            .foundedYear(UPDATED_FOUNDED_YEAR)
            .businessType(UPDATED_BUSINESS_TYPE)
            .businessStatus(UPDATED_BUSINESS_STATUS)
            .industryCode(UPDATED_INDUSTRY_CODE)
            .managedBy(UPDATED_MANAGED_BY)
            .thumbnail(UPDATED_THUMBNAIL)
            .images(UPDATED_IMAGES)
            .viewCount(UPDATED_VIEW_COUNT)
            .isFeatured(UPDATED_IS_FEATURED)
            .status(UPDATED_STATUS)
            .publishedAt(UPDATED_PUBLISHED_AT)
            .modifiedAt(UPDATED_MODIFIED_AT)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT)
            .esIndexed(UPDATED_ES_INDEXED);
    }

    @BeforeEach
    void initTest() {
        listing = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedListing != null) {
            listingRepository.delete(insertedListing);
            listingSearchRepository.delete(insertedListing);
            insertedListing = null;
        }
    }

    @Test
    @Transactional
    void createListing() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(listingSearchRepository.findAll());
        // Create the Listing
        ListingDTO listingDTO = listingMapper.toDto(listing);
        var returnedListingDTO = om.readValue(
            restListingMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(listingDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ListingDTO.class
        );

        // Validate the Listing in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedListing = listingMapper.toEntity(returnedListingDTO);
        assertListingUpdatableFieldsEquals(returnedListing, getPersistedListing(returnedListing));

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(listingSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore + 1);
            });

        insertedListing = returnedListing;
    }

    @Test
    @Transactional
    void createListingWithExistingId() throws Exception {
        // Create the Listing with an existing ID
        listing.setId(1L);
        ListingDTO listingDTO = listingMapper.toDto(listing);

        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(listingSearchRepository.findAll());

        // An entity with an existing ID cannot be created, so this API call must fail
        restListingMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(listingDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Listing in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(listingSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkWpIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(listingSearchRepository.findAll());
        // set the field null
        listing.setWpId(null);

        // Create the Listing, which fails.
        ListingDTO listingDTO = listingMapper.toDto(listing);

        restListingMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(listingDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(listingSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(listingSearchRepository.findAll());
        // set the field null
        listing.setName(null);

        // Create the Listing, which fails.
        ListingDTO listingDTO = listingMapper.toDto(listing);

        restListingMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(listingDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(listingSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void getAllListings() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList
        restListingMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(listing.getId().intValue())))
            .andExpect(jsonPath("$.[*].wpId").value(hasItem(DEFAULT_WP_ID.intValue())))
            .andExpect(jsonPath("$.[*].apiId").value(hasItem(DEFAULT_API_ID)))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].nameEn").value(hasItem(DEFAULT_NAME_EN)))
            .andExpect(jsonPath("$.[*].nameAlias").value(hasItem(DEFAULT_NAME_ALIAS)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].phone").value(hasItem(DEFAULT_PHONE)))
            .andExpect(jsonPath("$.[*].mobile").value(hasItem(DEFAULT_MOBILE)))
            .andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL)))
            .andExpect(jsonPath("$.[*].website").value(hasItem(DEFAULT_WEBSITE)))
            .andExpect(jsonPath("$.[*].address").value(hasItem(DEFAULT_ADDRESS)))
            .andExpect(jsonPath("$.[*].locationJson").value(hasItem(DEFAULT_LOCATION_JSON)))
            .andExpect(jsonPath("$.[*].taxCode").value(hasItem(DEFAULT_TAX_CODE)))
            .andExpect(jsonPath("$.[*].representative").value(hasItem(DEFAULT_REPRESENTATIVE)))
            .andExpect(jsonPath("$.[*].capital").value(hasItem(DEFAULT_CAPITAL)))
            .andExpect(jsonPath("$.[*].foundedYear").value(hasItem(DEFAULT_FOUNDED_YEAR)))
            .andExpect(jsonPath("$.[*].businessType").value(hasItem(DEFAULT_BUSINESS_TYPE)))
            .andExpect(jsonPath("$.[*].businessStatus").value(hasItem(DEFAULT_BUSINESS_STATUS)))
            .andExpect(jsonPath("$.[*].industryCode").value(hasItem(DEFAULT_INDUSTRY_CODE)))
            .andExpect(jsonPath("$.[*].managedBy").value(hasItem(DEFAULT_MANAGED_BY)))
            .andExpect(jsonPath("$.[*].thumbnail").value(hasItem(DEFAULT_THUMBNAIL)))
            .andExpect(jsonPath("$.[*].images").value(hasItem(DEFAULT_IMAGES)))
            .andExpect(jsonPath("$.[*].viewCount").value(hasItem(DEFAULT_VIEW_COUNT)))
            .andExpect(jsonPath("$.[*].isFeatured").value(hasItem(DEFAULT_IS_FEATURED)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS)))
            .andExpect(jsonPath("$.[*].publishedAt").value(hasItem(DEFAULT_PUBLISHED_AT.toString())))
            .andExpect(jsonPath("$.[*].modifiedAt").value(hasItem(DEFAULT_MODIFIED_AT.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())))
            .andExpect(jsonPath("$.[*].esIndexed").value(hasItem(DEFAULT_ES_INDEXED)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllListingsWithEagerRelationshipsIsEnabled() throws Exception {
        when(listingServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restListingMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(listingServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllListingsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(listingServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restListingMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(listingRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getListing() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get the listing
        restListingMockMvc
            .perform(get(ENTITY_API_URL_ID, listing.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(listing.getId().intValue()))
            .andExpect(jsonPath("$.wpId").value(DEFAULT_WP_ID.intValue()))
            .andExpect(jsonPath("$.apiId").value(DEFAULT_API_ID))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.nameEn").value(DEFAULT_NAME_EN))
            .andExpect(jsonPath("$.nameAlias").value(DEFAULT_NAME_ALIAS))
            .andExpect(jsonPath("$.slug").value(DEFAULT_SLUG))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.phone").value(DEFAULT_PHONE))
            .andExpect(jsonPath("$.mobile").value(DEFAULT_MOBILE))
            .andExpect(jsonPath("$.email").value(DEFAULT_EMAIL))
            .andExpect(jsonPath("$.website").value(DEFAULT_WEBSITE))
            .andExpect(jsonPath("$.address").value(DEFAULT_ADDRESS))
            .andExpect(jsonPath("$.locationJson").value(DEFAULT_LOCATION_JSON))
            .andExpect(jsonPath("$.taxCode").value(DEFAULT_TAX_CODE))
            .andExpect(jsonPath("$.representative").value(DEFAULT_REPRESENTATIVE))
            .andExpect(jsonPath("$.capital").value(DEFAULT_CAPITAL))
            .andExpect(jsonPath("$.foundedYear").value(DEFAULT_FOUNDED_YEAR))
            .andExpect(jsonPath("$.businessType").value(DEFAULT_BUSINESS_TYPE))
            .andExpect(jsonPath("$.businessStatus").value(DEFAULT_BUSINESS_STATUS))
            .andExpect(jsonPath("$.industryCode").value(DEFAULT_INDUSTRY_CODE))
            .andExpect(jsonPath("$.managedBy").value(DEFAULT_MANAGED_BY))
            .andExpect(jsonPath("$.thumbnail").value(DEFAULT_THUMBNAIL))
            .andExpect(jsonPath("$.images").value(DEFAULT_IMAGES))
            .andExpect(jsonPath("$.viewCount").value(DEFAULT_VIEW_COUNT))
            .andExpect(jsonPath("$.isFeatured").value(DEFAULT_IS_FEATURED))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS))
            .andExpect(jsonPath("$.publishedAt").value(DEFAULT_PUBLISHED_AT.toString()))
            .andExpect(jsonPath("$.modifiedAt").value(DEFAULT_MODIFIED_AT.toString()))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()))
            .andExpect(jsonPath("$.esIndexed").value(DEFAULT_ES_INDEXED));
    }

    @Test
    @Transactional
    void getListingsByIdFiltering() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        Long id = listing.getId();

        defaultListingFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultListingFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultListingFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllListingsByWpIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where wpId equals to
        defaultListingFiltering("wpId.equals=" + DEFAULT_WP_ID, "wpId.equals=" + UPDATED_WP_ID);
    }

    @Test
    @Transactional
    void getAllListingsByWpIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where wpId in
        defaultListingFiltering("wpId.in=" + DEFAULT_WP_ID + "," + UPDATED_WP_ID, "wpId.in=" + UPDATED_WP_ID);
    }

    @Test
    @Transactional
    void getAllListingsByWpIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where wpId is not null
        defaultListingFiltering("wpId.specified=true", "wpId.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByWpIdIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where wpId is greater than or equal to
        defaultListingFiltering("wpId.greaterThanOrEqual=" + DEFAULT_WP_ID, "wpId.greaterThanOrEqual=" + UPDATED_WP_ID);
    }

    @Test
    @Transactional
    void getAllListingsByWpIdIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where wpId is less than or equal to
        defaultListingFiltering("wpId.lessThanOrEqual=" + DEFAULT_WP_ID, "wpId.lessThanOrEqual=" + SMALLER_WP_ID);
    }

    @Test
    @Transactional
    void getAllListingsByWpIdIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where wpId is less than
        defaultListingFiltering("wpId.lessThan=" + UPDATED_WP_ID, "wpId.lessThan=" + DEFAULT_WP_ID);
    }

    @Test
    @Transactional
    void getAllListingsByWpIdIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where wpId is greater than
        defaultListingFiltering("wpId.greaterThan=" + SMALLER_WP_ID, "wpId.greaterThan=" + DEFAULT_WP_ID);
    }

    @Test
    @Transactional
    void getAllListingsByApiIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where apiId equals to
        defaultListingFiltering("apiId.equals=" + DEFAULT_API_ID, "apiId.equals=" + UPDATED_API_ID);
    }

    @Test
    @Transactional
    void getAllListingsByApiIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where apiId in
        defaultListingFiltering("apiId.in=" + DEFAULT_API_ID + "," + UPDATED_API_ID, "apiId.in=" + UPDATED_API_ID);
    }

    @Test
    @Transactional
    void getAllListingsByApiIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where apiId is not null
        defaultListingFiltering("apiId.specified=true", "apiId.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByApiIdContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where apiId contains
        defaultListingFiltering("apiId.contains=" + DEFAULT_API_ID, "apiId.contains=" + UPDATED_API_ID);
    }

    @Test
    @Transactional
    void getAllListingsByApiIdNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where apiId does not contain
        defaultListingFiltering("apiId.doesNotContain=" + UPDATED_API_ID, "apiId.doesNotContain=" + DEFAULT_API_ID);
    }

    @Test
    @Transactional
    void getAllListingsByNameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where name equals to
        defaultListingFiltering("name.equals=" + DEFAULT_NAME, "name.equals=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllListingsByNameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where name in
        defaultListingFiltering("name.in=" + DEFAULT_NAME + "," + UPDATED_NAME, "name.in=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllListingsByNameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where name is not null
        defaultListingFiltering("name.specified=true", "name.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByNameContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where name contains
        defaultListingFiltering("name.contains=" + DEFAULT_NAME, "name.contains=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllListingsByNameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where name does not contain
        defaultListingFiltering("name.doesNotContain=" + UPDATED_NAME, "name.doesNotContain=" + DEFAULT_NAME);
    }

    @Test
    @Transactional
    void getAllListingsByNameEnIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where nameEn equals to
        defaultListingFiltering("nameEn.equals=" + DEFAULT_NAME_EN, "nameEn.equals=" + UPDATED_NAME_EN);
    }

    @Test
    @Transactional
    void getAllListingsByNameEnIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where nameEn in
        defaultListingFiltering("nameEn.in=" + DEFAULT_NAME_EN + "," + UPDATED_NAME_EN, "nameEn.in=" + UPDATED_NAME_EN);
    }

    @Test
    @Transactional
    void getAllListingsByNameEnIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where nameEn is not null
        defaultListingFiltering("nameEn.specified=true", "nameEn.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByNameEnContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where nameEn contains
        defaultListingFiltering("nameEn.contains=" + DEFAULT_NAME_EN, "nameEn.contains=" + UPDATED_NAME_EN);
    }

    @Test
    @Transactional
    void getAllListingsByNameEnNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where nameEn does not contain
        defaultListingFiltering("nameEn.doesNotContain=" + UPDATED_NAME_EN, "nameEn.doesNotContain=" + DEFAULT_NAME_EN);
    }

    @Test
    @Transactional
    void getAllListingsByNameAliasIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where nameAlias equals to
        defaultListingFiltering("nameAlias.equals=" + DEFAULT_NAME_ALIAS, "nameAlias.equals=" + UPDATED_NAME_ALIAS);
    }

    @Test
    @Transactional
    void getAllListingsByNameAliasIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where nameAlias in
        defaultListingFiltering("nameAlias.in=" + DEFAULT_NAME_ALIAS + "," + UPDATED_NAME_ALIAS, "nameAlias.in=" + UPDATED_NAME_ALIAS);
    }

    @Test
    @Transactional
    void getAllListingsByNameAliasIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where nameAlias is not null
        defaultListingFiltering("nameAlias.specified=true", "nameAlias.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByNameAliasContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where nameAlias contains
        defaultListingFiltering("nameAlias.contains=" + DEFAULT_NAME_ALIAS, "nameAlias.contains=" + UPDATED_NAME_ALIAS);
    }

    @Test
    @Transactional
    void getAllListingsByNameAliasNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where nameAlias does not contain
        defaultListingFiltering("nameAlias.doesNotContain=" + UPDATED_NAME_ALIAS, "nameAlias.doesNotContain=" + DEFAULT_NAME_ALIAS);
    }

    @Test
    @Transactional
    void getAllListingsBySlugIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where slug equals to
        defaultListingFiltering("slug.equals=" + DEFAULT_SLUG, "slug.equals=" + UPDATED_SLUG);
    }

    @Test
    @Transactional
    void getAllListingsBySlugIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where slug in
        defaultListingFiltering("slug.in=" + DEFAULT_SLUG + "," + UPDATED_SLUG, "slug.in=" + UPDATED_SLUG);
    }

    @Test
    @Transactional
    void getAllListingsBySlugIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where slug is not null
        defaultListingFiltering("slug.specified=true", "slug.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsBySlugContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where slug contains
        defaultListingFiltering("slug.contains=" + DEFAULT_SLUG, "slug.contains=" + UPDATED_SLUG);
    }

    @Test
    @Transactional
    void getAllListingsBySlugNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where slug does not contain
        defaultListingFiltering("slug.doesNotContain=" + UPDATED_SLUG, "slug.doesNotContain=" + DEFAULT_SLUG);
    }

    @Test
    @Transactional
    void getAllListingsByPhoneIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where phone equals to
        defaultListingFiltering("phone.equals=" + DEFAULT_PHONE, "phone.equals=" + UPDATED_PHONE);
    }

    @Test
    @Transactional
    void getAllListingsByPhoneIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where phone in
        defaultListingFiltering("phone.in=" + DEFAULT_PHONE + "," + UPDATED_PHONE, "phone.in=" + UPDATED_PHONE);
    }

    @Test
    @Transactional
    void getAllListingsByPhoneIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where phone is not null
        defaultListingFiltering("phone.specified=true", "phone.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByPhoneContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where phone contains
        defaultListingFiltering("phone.contains=" + DEFAULT_PHONE, "phone.contains=" + UPDATED_PHONE);
    }

    @Test
    @Transactional
    void getAllListingsByPhoneNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where phone does not contain
        defaultListingFiltering("phone.doesNotContain=" + UPDATED_PHONE, "phone.doesNotContain=" + DEFAULT_PHONE);
    }

    @Test
    @Transactional
    void getAllListingsByMobileIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where mobile equals to
        defaultListingFiltering("mobile.equals=" + DEFAULT_MOBILE, "mobile.equals=" + UPDATED_MOBILE);
    }

    @Test
    @Transactional
    void getAllListingsByMobileIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where mobile in
        defaultListingFiltering("mobile.in=" + DEFAULT_MOBILE + "," + UPDATED_MOBILE, "mobile.in=" + UPDATED_MOBILE);
    }

    @Test
    @Transactional
    void getAllListingsByMobileIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where mobile is not null
        defaultListingFiltering("mobile.specified=true", "mobile.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByMobileContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where mobile contains
        defaultListingFiltering("mobile.contains=" + DEFAULT_MOBILE, "mobile.contains=" + UPDATED_MOBILE);
    }

    @Test
    @Transactional
    void getAllListingsByMobileNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where mobile does not contain
        defaultListingFiltering("mobile.doesNotContain=" + UPDATED_MOBILE, "mobile.doesNotContain=" + DEFAULT_MOBILE);
    }

    @Test
    @Transactional
    void getAllListingsByEmailIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where email equals to
        defaultListingFiltering("email.equals=" + DEFAULT_EMAIL, "email.equals=" + UPDATED_EMAIL);
    }

    @Test
    @Transactional
    void getAllListingsByEmailIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where email in
        defaultListingFiltering("email.in=" + DEFAULT_EMAIL + "," + UPDATED_EMAIL, "email.in=" + UPDATED_EMAIL);
    }

    @Test
    @Transactional
    void getAllListingsByEmailIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where email is not null
        defaultListingFiltering("email.specified=true", "email.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByEmailContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where email contains
        defaultListingFiltering("email.contains=" + DEFAULT_EMAIL, "email.contains=" + UPDATED_EMAIL);
    }

    @Test
    @Transactional
    void getAllListingsByEmailNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where email does not contain
        defaultListingFiltering("email.doesNotContain=" + UPDATED_EMAIL, "email.doesNotContain=" + DEFAULT_EMAIL);
    }

    @Test
    @Transactional
    void getAllListingsByWebsiteIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where website equals to
        defaultListingFiltering("website.equals=" + DEFAULT_WEBSITE, "website.equals=" + UPDATED_WEBSITE);
    }

    @Test
    @Transactional
    void getAllListingsByWebsiteIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where website in
        defaultListingFiltering("website.in=" + DEFAULT_WEBSITE + "," + UPDATED_WEBSITE, "website.in=" + UPDATED_WEBSITE);
    }

    @Test
    @Transactional
    void getAllListingsByWebsiteIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where website is not null
        defaultListingFiltering("website.specified=true", "website.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByWebsiteContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where website contains
        defaultListingFiltering("website.contains=" + DEFAULT_WEBSITE, "website.contains=" + UPDATED_WEBSITE);
    }

    @Test
    @Transactional
    void getAllListingsByWebsiteNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where website does not contain
        defaultListingFiltering("website.doesNotContain=" + UPDATED_WEBSITE, "website.doesNotContain=" + DEFAULT_WEBSITE);
    }

    @Test
    @Transactional
    void getAllListingsByTaxCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where taxCode equals to
        defaultListingFiltering("taxCode.equals=" + DEFAULT_TAX_CODE, "taxCode.equals=" + UPDATED_TAX_CODE);
    }

    @Test
    @Transactional
    void getAllListingsByTaxCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where taxCode in
        defaultListingFiltering("taxCode.in=" + DEFAULT_TAX_CODE + "," + UPDATED_TAX_CODE, "taxCode.in=" + UPDATED_TAX_CODE);
    }

    @Test
    @Transactional
    void getAllListingsByTaxCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where taxCode is not null
        defaultListingFiltering("taxCode.specified=true", "taxCode.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByTaxCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where taxCode contains
        defaultListingFiltering("taxCode.contains=" + DEFAULT_TAX_CODE, "taxCode.contains=" + UPDATED_TAX_CODE);
    }

    @Test
    @Transactional
    void getAllListingsByTaxCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where taxCode does not contain
        defaultListingFiltering("taxCode.doesNotContain=" + UPDATED_TAX_CODE, "taxCode.doesNotContain=" + DEFAULT_TAX_CODE);
    }

    @Test
    @Transactional
    void getAllListingsByRepresentativeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where representative equals to
        defaultListingFiltering("representative.equals=" + DEFAULT_REPRESENTATIVE, "representative.equals=" + UPDATED_REPRESENTATIVE);
    }

    @Test
    @Transactional
    void getAllListingsByRepresentativeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where representative in
        defaultListingFiltering(
            "representative.in=" + DEFAULT_REPRESENTATIVE + "," + UPDATED_REPRESENTATIVE,
            "representative.in=" + UPDATED_REPRESENTATIVE
        );
    }

    @Test
    @Transactional
    void getAllListingsByRepresentativeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where representative is not null
        defaultListingFiltering("representative.specified=true", "representative.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByRepresentativeContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where representative contains
        defaultListingFiltering("representative.contains=" + DEFAULT_REPRESENTATIVE, "representative.contains=" + UPDATED_REPRESENTATIVE);
    }

    @Test
    @Transactional
    void getAllListingsByRepresentativeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where representative does not contain
        defaultListingFiltering(
            "representative.doesNotContain=" + UPDATED_REPRESENTATIVE,
            "representative.doesNotContain=" + DEFAULT_REPRESENTATIVE
        );
    }

    @Test
    @Transactional
    void getAllListingsByCapitalIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where capital equals to
        defaultListingFiltering("capital.equals=" + DEFAULT_CAPITAL, "capital.equals=" + UPDATED_CAPITAL);
    }

    @Test
    @Transactional
    void getAllListingsByCapitalIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where capital in
        defaultListingFiltering("capital.in=" + DEFAULT_CAPITAL + "," + UPDATED_CAPITAL, "capital.in=" + UPDATED_CAPITAL);
    }

    @Test
    @Transactional
    void getAllListingsByCapitalIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where capital is not null
        defaultListingFiltering("capital.specified=true", "capital.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByCapitalContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where capital contains
        defaultListingFiltering("capital.contains=" + DEFAULT_CAPITAL, "capital.contains=" + UPDATED_CAPITAL);
    }

    @Test
    @Transactional
    void getAllListingsByCapitalNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where capital does not contain
        defaultListingFiltering("capital.doesNotContain=" + UPDATED_CAPITAL, "capital.doesNotContain=" + DEFAULT_CAPITAL);
    }

    @Test
    @Transactional
    void getAllListingsByFoundedYearIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where foundedYear equals to
        defaultListingFiltering("foundedYear.equals=" + DEFAULT_FOUNDED_YEAR, "foundedYear.equals=" + UPDATED_FOUNDED_YEAR);
    }

    @Test
    @Transactional
    void getAllListingsByFoundedYearIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where foundedYear in
        defaultListingFiltering(
            "foundedYear.in=" + DEFAULT_FOUNDED_YEAR + "," + UPDATED_FOUNDED_YEAR,
            "foundedYear.in=" + UPDATED_FOUNDED_YEAR
        );
    }

    @Test
    @Transactional
    void getAllListingsByFoundedYearIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where foundedYear is not null
        defaultListingFiltering("foundedYear.specified=true", "foundedYear.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByFoundedYearContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where foundedYear contains
        defaultListingFiltering("foundedYear.contains=" + DEFAULT_FOUNDED_YEAR, "foundedYear.contains=" + UPDATED_FOUNDED_YEAR);
    }

    @Test
    @Transactional
    void getAllListingsByFoundedYearNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where foundedYear does not contain
        defaultListingFiltering("foundedYear.doesNotContain=" + UPDATED_FOUNDED_YEAR, "foundedYear.doesNotContain=" + DEFAULT_FOUNDED_YEAR);
    }

    @Test
    @Transactional
    void getAllListingsByBusinessTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where businessType equals to
        defaultListingFiltering("businessType.equals=" + DEFAULT_BUSINESS_TYPE, "businessType.equals=" + UPDATED_BUSINESS_TYPE);
    }

    @Test
    @Transactional
    void getAllListingsByBusinessTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where businessType in
        defaultListingFiltering(
            "businessType.in=" + DEFAULT_BUSINESS_TYPE + "," + UPDATED_BUSINESS_TYPE,
            "businessType.in=" + UPDATED_BUSINESS_TYPE
        );
    }

    @Test
    @Transactional
    void getAllListingsByBusinessTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where businessType is not null
        defaultListingFiltering("businessType.specified=true", "businessType.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByBusinessTypeContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where businessType contains
        defaultListingFiltering("businessType.contains=" + DEFAULT_BUSINESS_TYPE, "businessType.contains=" + UPDATED_BUSINESS_TYPE);
    }

    @Test
    @Transactional
    void getAllListingsByBusinessTypeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where businessType does not contain
        defaultListingFiltering(
            "businessType.doesNotContain=" + UPDATED_BUSINESS_TYPE,
            "businessType.doesNotContain=" + DEFAULT_BUSINESS_TYPE
        );
    }

    @Test
    @Transactional
    void getAllListingsByBusinessStatusIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where businessStatus equals to
        defaultListingFiltering("businessStatus.equals=" + DEFAULT_BUSINESS_STATUS, "businessStatus.equals=" + UPDATED_BUSINESS_STATUS);
    }

    @Test
    @Transactional
    void getAllListingsByBusinessStatusIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where businessStatus in
        defaultListingFiltering(
            "businessStatus.in=" + DEFAULT_BUSINESS_STATUS + "," + UPDATED_BUSINESS_STATUS,
            "businessStatus.in=" + UPDATED_BUSINESS_STATUS
        );
    }

    @Test
    @Transactional
    void getAllListingsByBusinessStatusIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where businessStatus is not null
        defaultListingFiltering("businessStatus.specified=true", "businessStatus.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByBusinessStatusContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where businessStatus contains
        defaultListingFiltering("businessStatus.contains=" + DEFAULT_BUSINESS_STATUS, "businessStatus.contains=" + UPDATED_BUSINESS_STATUS);
    }

    @Test
    @Transactional
    void getAllListingsByBusinessStatusNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where businessStatus does not contain
        defaultListingFiltering(
            "businessStatus.doesNotContain=" + UPDATED_BUSINESS_STATUS,
            "businessStatus.doesNotContain=" + DEFAULT_BUSINESS_STATUS
        );
    }

    @Test
    @Transactional
    void getAllListingsByIndustryCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where industryCode equals to
        defaultListingFiltering("industryCode.equals=" + DEFAULT_INDUSTRY_CODE, "industryCode.equals=" + UPDATED_INDUSTRY_CODE);
    }

    @Test
    @Transactional
    void getAllListingsByIndustryCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where industryCode in
        defaultListingFiltering(
            "industryCode.in=" + DEFAULT_INDUSTRY_CODE + "," + UPDATED_INDUSTRY_CODE,
            "industryCode.in=" + UPDATED_INDUSTRY_CODE
        );
    }

    @Test
    @Transactional
    void getAllListingsByIndustryCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where industryCode is not null
        defaultListingFiltering("industryCode.specified=true", "industryCode.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByIndustryCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where industryCode contains
        defaultListingFiltering("industryCode.contains=" + DEFAULT_INDUSTRY_CODE, "industryCode.contains=" + UPDATED_INDUSTRY_CODE);
    }

    @Test
    @Transactional
    void getAllListingsByIndustryCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where industryCode does not contain
        defaultListingFiltering(
            "industryCode.doesNotContain=" + UPDATED_INDUSTRY_CODE,
            "industryCode.doesNotContain=" + DEFAULT_INDUSTRY_CODE
        );
    }

    @Test
    @Transactional
    void getAllListingsByManagedByIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where managedBy equals to
        defaultListingFiltering("managedBy.equals=" + DEFAULT_MANAGED_BY, "managedBy.equals=" + UPDATED_MANAGED_BY);
    }

    @Test
    @Transactional
    void getAllListingsByManagedByIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where managedBy in
        defaultListingFiltering("managedBy.in=" + DEFAULT_MANAGED_BY + "," + UPDATED_MANAGED_BY, "managedBy.in=" + UPDATED_MANAGED_BY);
    }

    @Test
    @Transactional
    void getAllListingsByManagedByIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where managedBy is not null
        defaultListingFiltering("managedBy.specified=true", "managedBy.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByManagedByContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where managedBy contains
        defaultListingFiltering("managedBy.contains=" + DEFAULT_MANAGED_BY, "managedBy.contains=" + UPDATED_MANAGED_BY);
    }

    @Test
    @Transactional
    void getAllListingsByManagedByNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where managedBy does not contain
        defaultListingFiltering("managedBy.doesNotContain=" + UPDATED_MANAGED_BY, "managedBy.doesNotContain=" + DEFAULT_MANAGED_BY);
    }

    @Test
    @Transactional
    void getAllListingsByThumbnailIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where thumbnail equals to
        defaultListingFiltering("thumbnail.equals=" + DEFAULT_THUMBNAIL, "thumbnail.equals=" + UPDATED_THUMBNAIL);
    }

    @Test
    @Transactional
    void getAllListingsByThumbnailIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where thumbnail in
        defaultListingFiltering("thumbnail.in=" + DEFAULT_THUMBNAIL + "," + UPDATED_THUMBNAIL, "thumbnail.in=" + UPDATED_THUMBNAIL);
    }

    @Test
    @Transactional
    void getAllListingsByThumbnailIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where thumbnail is not null
        defaultListingFiltering("thumbnail.specified=true", "thumbnail.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByThumbnailContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where thumbnail contains
        defaultListingFiltering("thumbnail.contains=" + DEFAULT_THUMBNAIL, "thumbnail.contains=" + UPDATED_THUMBNAIL);
    }

    @Test
    @Transactional
    void getAllListingsByThumbnailNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where thumbnail does not contain
        defaultListingFiltering("thumbnail.doesNotContain=" + UPDATED_THUMBNAIL, "thumbnail.doesNotContain=" + DEFAULT_THUMBNAIL);
    }

    @Test
    @Transactional
    void getAllListingsByViewCountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where viewCount equals to
        defaultListingFiltering("viewCount.equals=" + DEFAULT_VIEW_COUNT, "viewCount.equals=" + UPDATED_VIEW_COUNT);
    }

    @Test
    @Transactional
    void getAllListingsByViewCountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where viewCount in
        defaultListingFiltering("viewCount.in=" + DEFAULT_VIEW_COUNT + "," + UPDATED_VIEW_COUNT, "viewCount.in=" + UPDATED_VIEW_COUNT);
    }

    @Test
    @Transactional
    void getAllListingsByViewCountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where viewCount is not null
        defaultListingFiltering("viewCount.specified=true", "viewCount.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByViewCountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where viewCount is greater than or equal to
        defaultListingFiltering("viewCount.greaterThanOrEqual=" + DEFAULT_VIEW_COUNT, "viewCount.greaterThanOrEqual=" + UPDATED_VIEW_COUNT);
    }

    @Test
    @Transactional
    void getAllListingsByViewCountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where viewCount is less than or equal to
        defaultListingFiltering("viewCount.lessThanOrEqual=" + DEFAULT_VIEW_COUNT, "viewCount.lessThanOrEqual=" + SMALLER_VIEW_COUNT);
    }

    @Test
    @Transactional
    void getAllListingsByViewCountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where viewCount is less than
        defaultListingFiltering("viewCount.lessThan=" + UPDATED_VIEW_COUNT, "viewCount.lessThan=" + DEFAULT_VIEW_COUNT);
    }

    @Test
    @Transactional
    void getAllListingsByViewCountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where viewCount is greater than
        defaultListingFiltering("viewCount.greaterThan=" + SMALLER_VIEW_COUNT, "viewCount.greaterThan=" + DEFAULT_VIEW_COUNT);
    }

    @Test
    @Transactional
    void getAllListingsByIsFeaturedIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where isFeatured equals to
        defaultListingFiltering("isFeatured.equals=" + DEFAULT_IS_FEATURED, "isFeatured.equals=" + UPDATED_IS_FEATURED);
    }

    @Test
    @Transactional
    void getAllListingsByIsFeaturedIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where isFeatured in
        defaultListingFiltering("isFeatured.in=" + DEFAULT_IS_FEATURED + "," + UPDATED_IS_FEATURED, "isFeatured.in=" + UPDATED_IS_FEATURED);
    }

    @Test
    @Transactional
    void getAllListingsByIsFeaturedIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where isFeatured is not null
        defaultListingFiltering("isFeatured.specified=true", "isFeatured.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByStatusIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where status equals to
        defaultListingFiltering("status.equals=" + DEFAULT_STATUS, "status.equals=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllListingsByStatusIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where status in
        defaultListingFiltering("status.in=" + DEFAULT_STATUS + "," + UPDATED_STATUS, "status.in=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllListingsByStatusIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where status is not null
        defaultListingFiltering("status.specified=true", "status.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByStatusContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where status contains
        defaultListingFiltering("status.contains=" + DEFAULT_STATUS, "status.contains=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllListingsByStatusNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where status does not contain
        defaultListingFiltering("status.doesNotContain=" + UPDATED_STATUS, "status.doesNotContain=" + DEFAULT_STATUS);
    }

    @Test
    @Transactional
    void getAllListingsByPublishedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where publishedAt equals to
        defaultListingFiltering("publishedAt.equals=" + DEFAULT_PUBLISHED_AT, "publishedAt.equals=" + UPDATED_PUBLISHED_AT);
    }

    @Test
    @Transactional
    void getAllListingsByPublishedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where publishedAt in
        defaultListingFiltering(
            "publishedAt.in=" + DEFAULT_PUBLISHED_AT + "," + UPDATED_PUBLISHED_AT,
            "publishedAt.in=" + UPDATED_PUBLISHED_AT
        );
    }

    @Test
    @Transactional
    void getAllListingsByPublishedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where publishedAt is not null
        defaultListingFiltering("publishedAt.specified=true", "publishedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByModifiedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where modifiedAt equals to
        defaultListingFiltering("modifiedAt.equals=" + DEFAULT_MODIFIED_AT, "modifiedAt.equals=" + UPDATED_MODIFIED_AT);
    }

    @Test
    @Transactional
    void getAllListingsByModifiedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where modifiedAt in
        defaultListingFiltering("modifiedAt.in=" + DEFAULT_MODIFIED_AT + "," + UPDATED_MODIFIED_AT, "modifiedAt.in=" + UPDATED_MODIFIED_AT);
    }

    @Test
    @Transactional
    void getAllListingsByModifiedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where modifiedAt is not null
        defaultListingFiltering("modifiedAt.specified=true", "modifiedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByCreatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where createdAt equals to
        defaultListingFiltering("createdAt.equals=" + DEFAULT_CREATED_AT, "createdAt.equals=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllListingsByCreatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where createdAt in
        defaultListingFiltering("createdAt.in=" + DEFAULT_CREATED_AT + "," + UPDATED_CREATED_AT, "createdAt.in=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllListingsByCreatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where createdAt is not null
        defaultListingFiltering("createdAt.specified=true", "createdAt.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByUpdatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where updatedAt equals to
        defaultListingFiltering("updatedAt.equals=" + DEFAULT_UPDATED_AT, "updatedAt.equals=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllListingsByUpdatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where updatedAt in
        defaultListingFiltering("updatedAt.in=" + DEFAULT_UPDATED_AT + "," + UPDATED_UPDATED_AT, "updatedAt.in=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllListingsByUpdatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where updatedAt is not null
        defaultListingFiltering("updatedAt.specified=true", "updatedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByEsIndexedIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where esIndexed equals to
        defaultListingFiltering("esIndexed.equals=" + DEFAULT_ES_INDEXED, "esIndexed.equals=" + UPDATED_ES_INDEXED);
    }

    @Test
    @Transactional
    void getAllListingsByEsIndexedIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where esIndexed in
        defaultListingFiltering("esIndexed.in=" + DEFAULT_ES_INDEXED + "," + UPDATED_ES_INDEXED, "esIndexed.in=" + UPDATED_ES_INDEXED);
    }

    @Test
    @Transactional
    void getAllListingsByEsIndexedIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where esIndexed is not null
        defaultListingFiltering("esIndexed.specified=true", "esIndexed.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByCategoryIsEqualToSomething() throws Exception {
        Category category;
        if (TestUtil.findAll(em, Category.class).isEmpty()) {
            listingRepository.saveAndFlush(listing);
            category = CategoryResourceIT.createEntity();
        } else {
            category = TestUtil.findAll(em, Category.class).get(0);
        }
        em.persist(category);
        em.flush();
        listing.addCategory(category);
        listingRepository.saveAndFlush(listing);
        Long categoryId = category.getId();
        // Get all the listingList where category equals to categoryId
        defaultListingShouldBeFound("categoryId.equals=" + categoryId);

        // Get all the listingList where category equals to (categoryId + 1)
        defaultListingShouldNotBeFound("categoryId.equals=" + (categoryId + 1));
    }

    @Test
    @Transactional
    void getAllListingsByLocationIsEqualToSomething() throws Exception {
        Location location;
        if (TestUtil.findAll(em, Location.class).isEmpty()) {
            listingRepository.saveAndFlush(listing);
            location = LocationResourceIT.createEntity();
        } else {
            location = TestUtil.findAll(em, Location.class).get(0);
        }
        em.persist(location);
        em.flush();
        listing.addLocation(location);
        listingRepository.saveAndFlush(listing);
        Long locationId = location.getId();
        // Get all the listingList where location equals to locationId
        defaultListingShouldBeFound("locationId.equals=" + locationId);

        // Get all the listingList where location equals to (locationId + 1)
        defaultListingShouldNotBeFound("locationId.equals=" + (locationId + 1));
    }

    private void defaultListingFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultListingShouldBeFound(shouldBeFound);
        defaultListingShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultListingShouldBeFound(String filter) throws Exception {
        restListingMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(listing.getId().intValue())))
            .andExpect(jsonPath("$.[*].wpId").value(hasItem(DEFAULT_WP_ID.intValue())))
            .andExpect(jsonPath("$.[*].apiId").value(hasItem(DEFAULT_API_ID)))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].nameEn").value(hasItem(DEFAULT_NAME_EN)))
            .andExpect(jsonPath("$.[*].nameAlias").value(hasItem(DEFAULT_NAME_ALIAS)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].phone").value(hasItem(DEFAULT_PHONE)))
            .andExpect(jsonPath("$.[*].mobile").value(hasItem(DEFAULT_MOBILE)))
            .andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL)))
            .andExpect(jsonPath("$.[*].website").value(hasItem(DEFAULT_WEBSITE)))
            .andExpect(jsonPath("$.[*].address").value(hasItem(DEFAULT_ADDRESS)))
            .andExpect(jsonPath("$.[*].locationJson").value(hasItem(DEFAULT_LOCATION_JSON)))
            .andExpect(jsonPath("$.[*].taxCode").value(hasItem(DEFAULT_TAX_CODE)))
            .andExpect(jsonPath("$.[*].representative").value(hasItem(DEFAULT_REPRESENTATIVE)))
            .andExpect(jsonPath("$.[*].capital").value(hasItem(DEFAULT_CAPITAL)))
            .andExpect(jsonPath("$.[*].foundedYear").value(hasItem(DEFAULT_FOUNDED_YEAR)))
            .andExpect(jsonPath("$.[*].businessType").value(hasItem(DEFAULT_BUSINESS_TYPE)))
            .andExpect(jsonPath("$.[*].businessStatus").value(hasItem(DEFAULT_BUSINESS_STATUS)))
            .andExpect(jsonPath("$.[*].industryCode").value(hasItem(DEFAULT_INDUSTRY_CODE)))
            .andExpect(jsonPath("$.[*].managedBy").value(hasItem(DEFAULT_MANAGED_BY)))
            .andExpect(jsonPath("$.[*].thumbnail").value(hasItem(DEFAULT_THUMBNAIL)))
            .andExpect(jsonPath("$.[*].images").value(hasItem(DEFAULT_IMAGES)))
            .andExpect(jsonPath("$.[*].viewCount").value(hasItem(DEFAULT_VIEW_COUNT)))
            .andExpect(jsonPath("$.[*].isFeatured").value(hasItem(DEFAULT_IS_FEATURED)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS)))
            .andExpect(jsonPath("$.[*].publishedAt").value(hasItem(DEFAULT_PUBLISHED_AT.toString())))
            .andExpect(jsonPath("$.[*].modifiedAt").value(hasItem(DEFAULT_MODIFIED_AT.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())))
            .andExpect(jsonPath("$.[*].esIndexed").value(hasItem(DEFAULT_ES_INDEXED)));

        // Check, that the count call also returns 1
        restListingMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultListingShouldNotBeFound(String filter) throws Exception {
        restListingMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restListingMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingListing() throws Exception {
        // Get the listing
        restListingMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingListing() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        long databaseSizeBeforeUpdate = getRepositoryCount();
        listingSearchRepository.save(listing);
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(listingSearchRepository.findAll());

        // Update the listing
        Listing updatedListing = listingRepository.findById(listing.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedListing are not directly saved in db
        em.detach(updatedListing);
        updatedListing
            .wpId(UPDATED_WP_ID)
            .apiId(UPDATED_API_ID)
            .name(UPDATED_NAME)
            .nameEn(UPDATED_NAME_EN)
            .nameAlias(UPDATED_NAME_ALIAS)
            .slug(UPDATED_SLUG)
            .description(UPDATED_DESCRIPTION)
            .phone(UPDATED_PHONE)
            .mobile(UPDATED_MOBILE)
            .email(UPDATED_EMAIL)
            .website(UPDATED_WEBSITE)
            .address(UPDATED_ADDRESS)
            .locationJson(UPDATED_LOCATION_JSON)
            .taxCode(UPDATED_TAX_CODE)
            .representative(UPDATED_REPRESENTATIVE)
            .capital(UPDATED_CAPITAL)
            .foundedYear(UPDATED_FOUNDED_YEAR)
            .businessType(UPDATED_BUSINESS_TYPE)
            .businessStatus(UPDATED_BUSINESS_STATUS)
            .industryCode(UPDATED_INDUSTRY_CODE)
            .managedBy(UPDATED_MANAGED_BY)
            .thumbnail(UPDATED_THUMBNAIL)
            .images(UPDATED_IMAGES)
            .viewCount(UPDATED_VIEW_COUNT)
            .isFeatured(UPDATED_IS_FEATURED)
            .status(UPDATED_STATUS)
            .publishedAt(UPDATED_PUBLISHED_AT)
            .modifiedAt(UPDATED_MODIFIED_AT)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT)
            .esIndexed(UPDATED_ES_INDEXED);
        ListingDTO listingDTO = listingMapper.toDto(updatedListing);

        restListingMockMvc
            .perform(
                put(ENTITY_API_URL_ID, listingDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(listingDTO))
            )
            .andExpect(status().isOk());

        // Validate the Listing in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedListingToMatchAllProperties(updatedListing);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(listingSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
                List<Listing> listingSearchList = Streamable.of(listingSearchRepository.findAll()).toList();
                Listing testListingSearch = listingSearchList.get(searchDatabaseSizeAfter - 1);

                assertListingAllPropertiesEquals(testListingSearch, updatedListing);
            });
    }

    @Test
    @Transactional
    void putNonExistingListing() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(listingSearchRepository.findAll());
        listing.setId(longCount.incrementAndGet());

        // Create the Listing
        ListingDTO listingDTO = listingMapper.toDto(listing);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restListingMockMvc
            .perform(
                put(ENTITY_API_URL_ID, listingDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(listingDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Listing in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(listingSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithIdMismatchListing() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(listingSearchRepository.findAll());
        listing.setId(longCount.incrementAndGet());

        // Create the Listing
        ListingDTO listingDTO = listingMapper.toDto(listing);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restListingMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(listingDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Listing in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(listingSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamListing() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(listingSearchRepository.findAll());
        listing.setId(longCount.incrementAndGet());

        // Create the Listing
        ListingDTO listingDTO = listingMapper.toDto(listing);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restListingMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(listingDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Listing in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(listingSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void partialUpdateListingWithPatch() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the listing using partial update
        Listing partialUpdatedListing = new Listing();
        partialUpdatedListing.setId(listing.getId());

        partialUpdatedListing
            .nameEn(UPDATED_NAME_EN)
            .nameAlias(UPDATED_NAME_ALIAS)
            .description(UPDATED_DESCRIPTION)
            .email(UPDATED_EMAIL)
            .address(UPDATED_ADDRESS)
            .locationJson(UPDATED_LOCATION_JSON)
            .taxCode(UPDATED_TAX_CODE)
            .industryCode(UPDATED_INDUSTRY_CODE)
            .thumbnail(UPDATED_THUMBNAIL)
            .images(UPDATED_IMAGES)
            .viewCount(UPDATED_VIEW_COUNT)
            .isFeatured(UPDATED_IS_FEATURED)
            .publishedAt(UPDATED_PUBLISHED_AT)
            .modifiedAt(UPDATED_MODIFIED_AT)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT)
            .esIndexed(UPDATED_ES_INDEXED);

        restListingMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedListing.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedListing))
            )
            .andExpect(status().isOk());

        // Validate the Listing in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertListingUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedListing, listing), getPersistedListing(listing));
    }

    @Test
    @Transactional
    void fullUpdateListingWithPatch() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the listing using partial update
        Listing partialUpdatedListing = new Listing();
        partialUpdatedListing.setId(listing.getId());

        partialUpdatedListing
            .wpId(UPDATED_WP_ID)
            .apiId(UPDATED_API_ID)
            .name(UPDATED_NAME)
            .nameEn(UPDATED_NAME_EN)
            .nameAlias(UPDATED_NAME_ALIAS)
            .slug(UPDATED_SLUG)
            .description(UPDATED_DESCRIPTION)
            .phone(UPDATED_PHONE)
            .mobile(UPDATED_MOBILE)
            .email(UPDATED_EMAIL)
            .website(UPDATED_WEBSITE)
            .address(UPDATED_ADDRESS)
            .locationJson(UPDATED_LOCATION_JSON)
            .taxCode(UPDATED_TAX_CODE)
            .representative(UPDATED_REPRESENTATIVE)
            .capital(UPDATED_CAPITAL)
            .foundedYear(UPDATED_FOUNDED_YEAR)
            .businessType(UPDATED_BUSINESS_TYPE)
            .businessStatus(UPDATED_BUSINESS_STATUS)
            .industryCode(UPDATED_INDUSTRY_CODE)
            .managedBy(UPDATED_MANAGED_BY)
            .thumbnail(UPDATED_THUMBNAIL)
            .images(UPDATED_IMAGES)
            .viewCount(UPDATED_VIEW_COUNT)
            .isFeatured(UPDATED_IS_FEATURED)
            .status(UPDATED_STATUS)
            .publishedAt(UPDATED_PUBLISHED_AT)
            .modifiedAt(UPDATED_MODIFIED_AT)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT)
            .esIndexed(UPDATED_ES_INDEXED);

        restListingMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedListing.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedListing))
            )
            .andExpect(status().isOk());

        // Validate the Listing in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertListingUpdatableFieldsEquals(partialUpdatedListing, getPersistedListing(partialUpdatedListing));
    }

    @Test
    @Transactional
    void patchNonExistingListing() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(listingSearchRepository.findAll());
        listing.setId(longCount.incrementAndGet());

        // Create the Listing
        ListingDTO listingDTO = listingMapper.toDto(listing);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restListingMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, listingDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(listingDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Listing in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(listingSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithIdMismatchListing() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(listingSearchRepository.findAll());
        listing.setId(longCount.incrementAndGet());

        // Create the Listing
        ListingDTO listingDTO = listingMapper.toDto(listing);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restListingMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(listingDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Listing in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(listingSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamListing() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(listingSearchRepository.findAll());
        listing.setId(longCount.incrementAndGet());

        // Create the Listing
        ListingDTO listingDTO = listingMapper.toDto(listing);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restListingMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(listingDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Listing in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(listingSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void deleteListing() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);
        listingRepository.save(listing);
        listingSearchRepository.save(listing);

        long databaseSizeBeforeDelete = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(listingSearchRepository.findAll());
        assertThat(searchDatabaseSizeBefore).isEqualTo(databaseSizeBeforeDelete);

        // Delete the listing
        restListingMockMvc
            .perform(delete(ENTITY_API_URL_ID, listing.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(listingSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore - 1);
    }

    @Test
    @Transactional
    void searchListing() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);
        listingSearchRepository.save(listing);

        // Search the listing
        restListingMockMvc
            .perform(get(ENTITY_SEARCH_API_URL + "?query=id:" + listing.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(listing.getId().intValue())))
            .andExpect(jsonPath("$.[*].wpId").value(hasItem(DEFAULT_WP_ID.intValue())))
            .andExpect(jsonPath("$.[*].apiId").value(hasItem(DEFAULT_API_ID)))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].nameEn").value(hasItem(DEFAULT_NAME_EN)))
            .andExpect(jsonPath("$.[*].nameAlias").value(hasItem(DEFAULT_NAME_ALIAS)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION.toString())))
            .andExpect(jsonPath("$.[*].phone").value(hasItem(DEFAULT_PHONE)))
            .andExpect(jsonPath("$.[*].mobile").value(hasItem(DEFAULT_MOBILE)))
            .andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL)))
            .andExpect(jsonPath("$.[*].website").value(hasItem(DEFAULT_WEBSITE)))
            .andExpect(jsonPath("$.[*].address").value(hasItem(DEFAULT_ADDRESS.toString())))
            .andExpect(jsonPath("$.[*].locationJson").value(hasItem(DEFAULT_LOCATION_JSON.toString())))
            .andExpect(jsonPath("$.[*].taxCode").value(hasItem(DEFAULT_TAX_CODE)))
            .andExpect(jsonPath("$.[*].representative").value(hasItem(DEFAULT_REPRESENTATIVE)))
            .andExpect(jsonPath("$.[*].capital").value(hasItem(DEFAULT_CAPITAL)))
            .andExpect(jsonPath("$.[*].foundedYear").value(hasItem(DEFAULT_FOUNDED_YEAR)))
            .andExpect(jsonPath("$.[*].businessType").value(hasItem(DEFAULT_BUSINESS_TYPE)))
            .andExpect(jsonPath("$.[*].businessStatus").value(hasItem(DEFAULT_BUSINESS_STATUS)))
            .andExpect(jsonPath("$.[*].industryCode").value(hasItem(DEFAULT_INDUSTRY_CODE)))
            .andExpect(jsonPath("$.[*].managedBy").value(hasItem(DEFAULT_MANAGED_BY)))
            .andExpect(jsonPath("$.[*].thumbnail").value(hasItem(DEFAULT_THUMBNAIL)))
            .andExpect(jsonPath("$.[*].images").value(hasItem(DEFAULT_IMAGES.toString())))
            .andExpect(jsonPath("$.[*].viewCount").value(hasItem(DEFAULT_VIEW_COUNT)))
            .andExpect(jsonPath("$.[*].isFeatured").value(hasItem(DEFAULT_IS_FEATURED)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS)))
            .andExpect(jsonPath("$.[*].publishedAt").value(hasItem(DEFAULT_PUBLISHED_AT.toString())))
            .andExpect(jsonPath("$.[*].modifiedAt").value(hasItem(DEFAULT_MODIFIED_AT.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())))
            .andExpect(jsonPath("$.[*].esIndexed").value(hasItem(DEFAULT_ES_INDEXED)));
    }

    protected long getRepositoryCount() {
        return listingRepository.count();
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

    protected Listing getPersistedListing(Listing listing) {
        return listingRepository.findById(listing.getId()).orElseThrow();
    }

    protected void assertPersistedListingToMatchAllProperties(Listing expectedListing) {
        assertListingAllPropertiesEquals(expectedListing, getPersistedListing(expectedListing));
    }

    protected void assertPersistedListingToMatchUpdatableProperties(Listing expectedListing) {
        assertListingAllUpdatablePropertiesEquals(expectedListing, getPersistedListing(expectedListing));
    }
}
