package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.ListingAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static com.mycompany.myapp.web.rest.TestUtil.sameNumber;
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
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.ListingRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.repository.search.ListingSearchRepository;
import com.mycompany.myapp.service.ListingService;
import com.mycompany.myapp.service.dto.ListingDTO;
import com.mycompany.myapp.service.mapper.ListingMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
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

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_SLUG = "AAAAAAAAAA";
    private static final String UPDATED_SLUG = "BBBBBBBBBB";

    private static final String DEFAULT_CONTENT = "AAAAAAAAAA";
    private static final String UPDATED_CONTENT = "BBBBBBBBBB";

    private static final String DEFAULT_STATUS = "AAAAAAAAAA";
    private static final String UPDATED_STATUS = "BBBBBBBBBB";

    private static final String DEFAULT_EMAIL = "AAAAAAAAAA";
    private static final String UPDATED_EMAIL = "BBBBBBBBBB";

    private static final String DEFAULT_TELEPHONE = "AAAAAAAAAA";
    private static final String UPDATED_TELEPHONE = "BBBBBBBBBB";

    private static final String DEFAULT_MOBILE = "AAAAAAAAAA";
    private static final String UPDATED_MOBILE = "BBBBBBBBBB";

    private static final String DEFAULT_WEBSITE = "AAAAAAAAAA";
    private static final String UPDATED_WEBSITE = "BBBBBBBBBB";

    private static final String DEFAULT_FAX = "AAAAAAAAAA";
    private static final String UPDATED_FAX = "BBBBBBBBBB";

    private static final String DEFAULT_TAX_CODE = "AAAAAAAAAA";
    private static final String UPDATED_TAX_CODE = "BBBBBBBBBB";

    private static final String DEFAULT_NAME_ALIAS = "AAAAAAAAAA";
    private static final String UPDATED_NAME_ALIAS = "BBBBBBBBBB";

    private static final String DEFAULT_NAME_EN = "AAAAAAAAAA";
    private static final String UPDATED_NAME_EN = "BBBBBBBBBB";

    private static final String DEFAULT_REPRESENTATIVE = "AAAAAAAAAA";
    private static final String UPDATED_REPRESENTATIVE = "BBBBBBBBBB";

    private static final String DEFAULT_MAIN_INDUSTRY = "AAAAAAAAAA";
    private static final String UPDATED_MAIN_INDUSTRY = "BBBBBBBBBB";

    private static final String DEFAULT_MANAGED_BY = "AAAAAAAAAA";
    private static final String UPDATED_MANAGED_BY = "BBBBBBBBBB";

    private static final String DEFAULT_BUSINESS_TYPE = "AAAAAAAAAA";
    private static final String UPDATED_BUSINESS_TYPE = "BBBBBBBBBB";

    private static final String DEFAULT_STATUS_YP = "AAAAAAAAAA";
    private static final String UPDATED_STATUS_YP = "BBBBBBBBBB";

    private static final Instant DEFAULT_FOUNDED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_FOUNDED_DATE = Instant.ofEpochMilli(1790760789812L);

    private static final Instant DEFAULT_LICENSE_MODIFIED_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_LICENSE_MODIFIED_DATE = Instant.ofEpochMilli(1790760789812L);

    private static final String DEFAULT_ADDRESS = "AAAAAAAAAA";
    private static final String UPDATED_ADDRESS = "BBBBBBBBBB";

    private static final String DEFAULT_ADDRESS_ALTERNATIVE = "AAAAAAAAAA";
    private static final String UPDATED_ADDRESS_ALTERNATIVE = "BBBBBBBBBB";

    private static final BigDecimal DEFAULT_LATITUDE = new BigDecimal(1);
    private static final BigDecimal UPDATED_LATITUDE = new BigDecimal(2);
    private static final BigDecimal SMALLER_LATITUDE = new BigDecimal(1 - 1);

    private static final BigDecimal DEFAULT_LONGITUDE = new BigDecimal(1);
    private static final BigDecimal UPDATED_LONGITUDE = new BigDecimal(2);
    private static final BigDecimal SMALLER_LONGITUDE = new BigDecimal(1 - 1);

    private static final Long DEFAULT_API_ID = 1L;
    private static final Long UPDATED_API_ID = 2L;
    private static final Long SMALLER_API_ID = 1L - 1L;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1790760789812L);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.ofEpochMilli(1790760789812L);

    private static final String ENTITY_API_URL = "/api/listings";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/listings/_search";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private UserRepository userRepository;

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
            .title(DEFAULT_TITLE)
            .slug(DEFAULT_SLUG)
            .content(DEFAULT_CONTENT)
            .status(DEFAULT_STATUS)
            .email(DEFAULT_EMAIL)
            .telephone(DEFAULT_TELEPHONE)
            .mobile(DEFAULT_MOBILE)
            .website(DEFAULT_WEBSITE)
            .fax(DEFAULT_FAX)
            .taxCode(DEFAULT_TAX_CODE)
            .nameAlias(DEFAULT_NAME_ALIAS)
            .nameEn(DEFAULT_NAME_EN)
            .representative(DEFAULT_REPRESENTATIVE)
            .mainIndustry(DEFAULT_MAIN_INDUSTRY)
            .managedBy(DEFAULT_MANAGED_BY)
            .businessType(DEFAULT_BUSINESS_TYPE)
            .statusYp(DEFAULT_STATUS_YP)
            .foundedDate(DEFAULT_FOUNDED_DATE)
            .licenseModifiedDate(DEFAULT_LICENSE_MODIFIED_DATE)
            .address(DEFAULT_ADDRESS)
            .addressAlternative(DEFAULT_ADDRESS_ALTERNATIVE)
            .latitude(DEFAULT_LATITUDE)
            .longitude(DEFAULT_LONGITUDE)
            .apiId(DEFAULT_API_ID)
            .createdAt(DEFAULT_CREATED_AT)
            .updatedAt(DEFAULT_UPDATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Listing createUpdatedEntity() {
        return new Listing()
            .title(UPDATED_TITLE)
            .slug(UPDATED_SLUG)
            .content(UPDATED_CONTENT)
            .status(UPDATED_STATUS)
            .email(UPDATED_EMAIL)
            .telephone(UPDATED_TELEPHONE)
            .mobile(UPDATED_MOBILE)
            .website(UPDATED_WEBSITE)
            .fax(UPDATED_FAX)
            .taxCode(UPDATED_TAX_CODE)
            .nameAlias(UPDATED_NAME_ALIAS)
            .nameEn(UPDATED_NAME_EN)
            .representative(UPDATED_REPRESENTATIVE)
            .mainIndustry(UPDATED_MAIN_INDUSTRY)
            .managedBy(UPDATED_MANAGED_BY)
            .businessType(UPDATED_BUSINESS_TYPE)
            .statusYp(UPDATED_STATUS_YP)
            .foundedDate(UPDATED_FOUNDED_DATE)
            .licenseModifiedDate(UPDATED_LICENSE_MODIFIED_DATE)
            .address(UPDATED_ADDRESS)
            .addressAlternative(UPDATED_ADDRESS_ALTERNATIVE)
            .latitude(UPDATED_LATITUDE)
            .longitude(UPDATED_LONGITUDE)
            .apiId(UPDATED_API_ID)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
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
    void checkTitleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(listingSearchRepository.findAll());
        // set the field null
        listing.setTitle(null);

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
    void checkSlugIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(listingSearchRepository.findAll());
        // set the field null
        listing.setSlug(null);

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
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].content").value(hasItem(DEFAULT_CONTENT)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS)))
            .andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL)))
            .andExpect(jsonPath("$.[*].telephone").value(hasItem(DEFAULT_TELEPHONE)))
            .andExpect(jsonPath("$.[*].mobile").value(hasItem(DEFAULT_MOBILE)))
            .andExpect(jsonPath("$.[*].website").value(hasItem(DEFAULT_WEBSITE)))
            .andExpect(jsonPath("$.[*].fax").value(hasItem(DEFAULT_FAX)))
            .andExpect(jsonPath("$.[*].taxCode").value(hasItem(DEFAULT_TAX_CODE)))
            .andExpect(jsonPath("$.[*].nameAlias").value(hasItem(DEFAULT_NAME_ALIAS)))
            .andExpect(jsonPath("$.[*].nameEn").value(hasItem(DEFAULT_NAME_EN)))
            .andExpect(jsonPath("$.[*].representative").value(hasItem(DEFAULT_REPRESENTATIVE)))
            .andExpect(jsonPath("$.[*].mainIndustry").value(hasItem(DEFAULT_MAIN_INDUSTRY)))
            .andExpect(jsonPath("$.[*].managedBy").value(hasItem(DEFAULT_MANAGED_BY)))
            .andExpect(jsonPath("$.[*].businessType").value(hasItem(DEFAULT_BUSINESS_TYPE)))
            .andExpect(jsonPath("$.[*].statusYp").value(hasItem(DEFAULT_STATUS_YP)))
            .andExpect(jsonPath("$.[*].foundedDate").value(hasItem(DEFAULT_FOUNDED_DATE.toString())))
            .andExpect(jsonPath("$.[*].licenseModifiedDate").value(hasItem(DEFAULT_LICENSE_MODIFIED_DATE.toString())))
            .andExpect(jsonPath("$.[*].address").value(hasItem(DEFAULT_ADDRESS)))
            .andExpect(jsonPath("$.[*].addressAlternative").value(hasItem(DEFAULT_ADDRESS_ALTERNATIVE)))
            .andExpect(jsonPath("$.[*].latitude").value(hasItem(sameNumber(DEFAULT_LATITUDE))))
            .andExpect(jsonPath("$.[*].longitude").value(hasItem(sameNumber(DEFAULT_LONGITUDE))))
            .andExpect(jsonPath("$.[*].apiId").value(hasItem(DEFAULT_API_ID.intValue())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
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
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
            .andExpect(jsonPath("$.slug").value(DEFAULT_SLUG))
            .andExpect(jsonPath("$.content").value(DEFAULT_CONTENT))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS))
            .andExpect(jsonPath("$.email").value(DEFAULT_EMAIL))
            .andExpect(jsonPath("$.telephone").value(DEFAULT_TELEPHONE))
            .andExpect(jsonPath("$.mobile").value(DEFAULT_MOBILE))
            .andExpect(jsonPath("$.website").value(DEFAULT_WEBSITE))
            .andExpect(jsonPath("$.fax").value(DEFAULT_FAX))
            .andExpect(jsonPath("$.taxCode").value(DEFAULT_TAX_CODE))
            .andExpect(jsonPath("$.nameAlias").value(DEFAULT_NAME_ALIAS))
            .andExpect(jsonPath("$.nameEn").value(DEFAULT_NAME_EN))
            .andExpect(jsonPath("$.representative").value(DEFAULT_REPRESENTATIVE))
            .andExpect(jsonPath("$.mainIndustry").value(DEFAULT_MAIN_INDUSTRY))
            .andExpect(jsonPath("$.managedBy").value(DEFAULT_MANAGED_BY))
            .andExpect(jsonPath("$.businessType").value(DEFAULT_BUSINESS_TYPE))
            .andExpect(jsonPath("$.statusYp").value(DEFAULT_STATUS_YP))
            .andExpect(jsonPath("$.foundedDate").value(DEFAULT_FOUNDED_DATE.toString()))
            .andExpect(jsonPath("$.licenseModifiedDate").value(DEFAULT_LICENSE_MODIFIED_DATE.toString()))
            .andExpect(jsonPath("$.address").value(DEFAULT_ADDRESS))
            .andExpect(jsonPath("$.addressAlternative").value(DEFAULT_ADDRESS_ALTERNATIVE))
            .andExpect(jsonPath("$.latitude").value(sameNumber(DEFAULT_LATITUDE)))
            .andExpect(jsonPath("$.longitude").value(sameNumber(DEFAULT_LONGITUDE)))
            .andExpect(jsonPath("$.apiId").value(DEFAULT_API_ID.intValue()))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()));
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
    void getAllListingsByTitleIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where title equals to
        defaultListingFiltering("title.equals=" + DEFAULT_TITLE, "title.equals=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllListingsByTitleIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where title in
        defaultListingFiltering("title.in=" + DEFAULT_TITLE + "," + UPDATED_TITLE, "title.in=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllListingsByTitleIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where title is not null
        defaultListingFiltering("title.specified=true", "title.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByTitleContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where title contains
        defaultListingFiltering("title.contains=" + DEFAULT_TITLE, "title.contains=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllListingsByTitleNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where title does not contain
        defaultListingFiltering("title.doesNotContain=" + UPDATED_TITLE, "title.doesNotContain=" + DEFAULT_TITLE);
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
    void getAllListingsByTelephoneIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where telephone equals to
        defaultListingFiltering("telephone.equals=" + DEFAULT_TELEPHONE, "telephone.equals=" + UPDATED_TELEPHONE);
    }

    @Test
    @Transactional
    void getAllListingsByTelephoneIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where telephone in
        defaultListingFiltering("telephone.in=" + DEFAULT_TELEPHONE + "," + UPDATED_TELEPHONE, "telephone.in=" + UPDATED_TELEPHONE);
    }

    @Test
    @Transactional
    void getAllListingsByTelephoneIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where telephone is not null
        defaultListingFiltering("telephone.specified=true", "telephone.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByTelephoneContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where telephone contains
        defaultListingFiltering("telephone.contains=" + DEFAULT_TELEPHONE, "telephone.contains=" + UPDATED_TELEPHONE);
    }

    @Test
    @Transactional
    void getAllListingsByTelephoneNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where telephone does not contain
        defaultListingFiltering("telephone.doesNotContain=" + UPDATED_TELEPHONE, "telephone.doesNotContain=" + DEFAULT_TELEPHONE);
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
    void getAllListingsByFaxIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where fax equals to
        defaultListingFiltering("fax.equals=" + DEFAULT_FAX, "fax.equals=" + UPDATED_FAX);
    }

    @Test
    @Transactional
    void getAllListingsByFaxIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where fax in
        defaultListingFiltering("fax.in=" + DEFAULT_FAX + "," + UPDATED_FAX, "fax.in=" + UPDATED_FAX);
    }

    @Test
    @Transactional
    void getAllListingsByFaxIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where fax is not null
        defaultListingFiltering("fax.specified=true", "fax.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByFaxContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where fax contains
        defaultListingFiltering("fax.contains=" + DEFAULT_FAX, "fax.contains=" + UPDATED_FAX);
    }

    @Test
    @Transactional
    void getAllListingsByFaxNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where fax does not contain
        defaultListingFiltering("fax.doesNotContain=" + UPDATED_FAX, "fax.doesNotContain=" + DEFAULT_FAX);
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
    void getAllListingsByMainIndustryIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where mainIndustry equals to
        defaultListingFiltering("mainIndustry.equals=" + DEFAULT_MAIN_INDUSTRY, "mainIndustry.equals=" + UPDATED_MAIN_INDUSTRY);
    }

    @Test
    @Transactional
    void getAllListingsByMainIndustryIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where mainIndustry in
        defaultListingFiltering(
            "mainIndustry.in=" + DEFAULT_MAIN_INDUSTRY + "," + UPDATED_MAIN_INDUSTRY,
            "mainIndustry.in=" + UPDATED_MAIN_INDUSTRY
        );
    }

    @Test
    @Transactional
    void getAllListingsByMainIndustryIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where mainIndustry is not null
        defaultListingFiltering("mainIndustry.specified=true", "mainIndustry.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByMainIndustryContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where mainIndustry contains
        defaultListingFiltering("mainIndustry.contains=" + DEFAULT_MAIN_INDUSTRY, "mainIndustry.contains=" + UPDATED_MAIN_INDUSTRY);
    }

    @Test
    @Transactional
    void getAllListingsByMainIndustryNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where mainIndustry does not contain
        defaultListingFiltering(
            "mainIndustry.doesNotContain=" + UPDATED_MAIN_INDUSTRY,
            "mainIndustry.doesNotContain=" + DEFAULT_MAIN_INDUSTRY
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
    void getAllListingsByStatusYpIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where statusYp equals to
        defaultListingFiltering("statusYp.equals=" + DEFAULT_STATUS_YP, "statusYp.equals=" + UPDATED_STATUS_YP);
    }

    @Test
    @Transactional
    void getAllListingsByStatusYpIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where statusYp in
        defaultListingFiltering("statusYp.in=" + DEFAULT_STATUS_YP + "," + UPDATED_STATUS_YP, "statusYp.in=" + UPDATED_STATUS_YP);
    }

    @Test
    @Transactional
    void getAllListingsByStatusYpIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where statusYp is not null
        defaultListingFiltering("statusYp.specified=true", "statusYp.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByStatusYpContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where statusYp contains
        defaultListingFiltering("statusYp.contains=" + DEFAULT_STATUS_YP, "statusYp.contains=" + UPDATED_STATUS_YP);
    }

    @Test
    @Transactional
    void getAllListingsByStatusYpNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where statusYp does not contain
        defaultListingFiltering("statusYp.doesNotContain=" + UPDATED_STATUS_YP, "statusYp.doesNotContain=" + DEFAULT_STATUS_YP);
    }

    @Test
    @Transactional
    void getAllListingsByFoundedDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where foundedDate equals to
        defaultListingFiltering("foundedDate.equals=" + DEFAULT_FOUNDED_DATE, "foundedDate.equals=" + UPDATED_FOUNDED_DATE);
    }

    @Test
    @Transactional
    void getAllListingsByFoundedDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where foundedDate in
        defaultListingFiltering(
            "foundedDate.in=" + DEFAULT_FOUNDED_DATE + "," + UPDATED_FOUNDED_DATE,
            "foundedDate.in=" + UPDATED_FOUNDED_DATE
        );
    }

    @Test
    @Transactional
    void getAllListingsByFoundedDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where foundedDate is not null
        defaultListingFiltering("foundedDate.specified=true", "foundedDate.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByLicenseModifiedDateIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where licenseModifiedDate equals to
        defaultListingFiltering(
            "licenseModifiedDate.equals=" + DEFAULT_LICENSE_MODIFIED_DATE,
            "licenseModifiedDate.equals=" + UPDATED_LICENSE_MODIFIED_DATE
        );
    }

    @Test
    @Transactional
    void getAllListingsByLicenseModifiedDateIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where licenseModifiedDate in
        defaultListingFiltering(
            "licenseModifiedDate.in=" + DEFAULT_LICENSE_MODIFIED_DATE + "," + UPDATED_LICENSE_MODIFIED_DATE,
            "licenseModifiedDate.in=" + UPDATED_LICENSE_MODIFIED_DATE
        );
    }

    @Test
    @Transactional
    void getAllListingsByLicenseModifiedDateIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where licenseModifiedDate is not null
        defaultListingFiltering("licenseModifiedDate.specified=true", "licenseModifiedDate.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByLatitudeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where latitude equals to
        defaultListingFiltering("latitude.equals=" + DEFAULT_LATITUDE, "latitude.equals=" + UPDATED_LATITUDE);
    }

    @Test
    @Transactional
    void getAllListingsByLatitudeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where latitude in
        defaultListingFiltering("latitude.in=" + DEFAULT_LATITUDE + "," + UPDATED_LATITUDE, "latitude.in=" + UPDATED_LATITUDE);
    }

    @Test
    @Transactional
    void getAllListingsByLatitudeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where latitude is not null
        defaultListingFiltering("latitude.specified=true", "latitude.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByLatitudeIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where latitude is greater than or equal to
        defaultListingFiltering("latitude.greaterThanOrEqual=" + DEFAULT_LATITUDE, "latitude.greaterThanOrEqual=" + UPDATED_LATITUDE);
    }

    @Test
    @Transactional
    void getAllListingsByLatitudeIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where latitude is less than or equal to
        defaultListingFiltering("latitude.lessThanOrEqual=" + DEFAULT_LATITUDE, "latitude.lessThanOrEqual=" + SMALLER_LATITUDE);
    }

    @Test
    @Transactional
    void getAllListingsByLatitudeIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where latitude is less than
        defaultListingFiltering("latitude.lessThan=" + UPDATED_LATITUDE, "latitude.lessThan=" + DEFAULT_LATITUDE);
    }

    @Test
    @Transactional
    void getAllListingsByLatitudeIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where latitude is greater than
        defaultListingFiltering("latitude.greaterThan=" + SMALLER_LATITUDE, "latitude.greaterThan=" + DEFAULT_LATITUDE);
    }

    @Test
    @Transactional
    void getAllListingsByLongitudeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where longitude equals to
        defaultListingFiltering("longitude.equals=" + DEFAULT_LONGITUDE, "longitude.equals=" + UPDATED_LONGITUDE);
    }

    @Test
    @Transactional
    void getAllListingsByLongitudeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where longitude in
        defaultListingFiltering("longitude.in=" + DEFAULT_LONGITUDE + "," + UPDATED_LONGITUDE, "longitude.in=" + UPDATED_LONGITUDE);
    }

    @Test
    @Transactional
    void getAllListingsByLongitudeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where longitude is not null
        defaultListingFiltering("longitude.specified=true", "longitude.specified=false");
    }

    @Test
    @Transactional
    void getAllListingsByLongitudeIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where longitude is greater than or equal to
        defaultListingFiltering("longitude.greaterThanOrEqual=" + DEFAULT_LONGITUDE, "longitude.greaterThanOrEqual=" + UPDATED_LONGITUDE);
    }

    @Test
    @Transactional
    void getAllListingsByLongitudeIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where longitude is less than or equal to
        defaultListingFiltering("longitude.lessThanOrEqual=" + DEFAULT_LONGITUDE, "longitude.lessThanOrEqual=" + SMALLER_LONGITUDE);
    }

    @Test
    @Transactional
    void getAllListingsByLongitudeIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where longitude is less than
        defaultListingFiltering("longitude.lessThan=" + UPDATED_LONGITUDE, "longitude.lessThan=" + DEFAULT_LONGITUDE);
    }

    @Test
    @Transactional
    void getAllListingsByLongitudeIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where longitude is greater than
        defaultListingFiltering("longitude.greaterThan=" + SMALLER_LONGITUDE, "longitude.greaterThan=" + DEFAULT_LONGITUDE);
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
    void getAllListingsByApiIdIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where apiId is greater than or equal to
        defaultListingFiltering("apiId.greaterThanOrEqual=" + DEFAULT_API_ID, "apiId.greaterThanOrEqual=" + UPDATED_API_ID);
    }

    @Test
    @Transactional
    void getAllListingsByApiIdIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where apiId is less than or equal to
        defaultListingFiltering("apiId.lessThanOrEqual=" + DEFAULT_API_ID, "apiId.lessThanOrEqual=" + SMALLER_API_ID);
    }

    @Test
    @Transactional
    void getAllListingsByApiIdIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where apiId is less than
        defaultListingFiltering("apiId.lessThan=" + UPDATED_API_ID, "apiId.lessThan=" + DEFAULT_API_ID);
    }

    @Test
    @Transactional
    void getAllListingsByApiIdIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedListing = listingRepository.saveAndFlush(listing);

        // Get all the listingList where apiId is greater than
        defaultListingFiltering("apiId.greaterThan=" + SMALLER_API_ID, "apiId.greaterThan=" + DEFAULT_API_ID);
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
    void getAllListingsByAuthorIsEqualToSomething() throws Exception {
        User author;
        if (TestUtil.findAll(em, User.class).isEmpty()) {
            listingRepository.saveAndFlush(listing);
            author = UserResourceIT.createEntity();
        } else {
            author = TestUtil.findAll(em, User.class).get(0);
        }
        em.persist(author);
        em.flush();
        listing.setAuthor(author);
        listingRepository.saveAndFlush(listing);
        Long authorId = author.getId();
        // Get all the listingList where author equals to authorId
        defaultListingShouldBeFound("authorId.equals=" + authorId);

        // Get all the listingList where author equals to (authorId + 1)
        defaultListingShouldNotBeFound("authorId.equals=" + (authorId + 1));
    }

    @Test
    @Transactional
    void getAllListingsByCategoriesIsEqualToSomething() throws Exception {
        Category categories;
        if (TestUtil.findAll(em, Category.class).isEmpty()) {
            listingRepository.saveAndFlush(listing);
            categories = CategoryResourceIT.createEntity();
        } else {
            categories = TestUtil.findAll(em, Category.class).get(0);
        }
        em.persist(categories);
        em.flush();
        listing.addCategories(categories);
        listingRepository.saveAndFlush(listing);
        Long categoriesId = categories.getId();
        // Get all the listingList where categories equals to categoriesId
        defaultListingShouldBeFound("categoriesId.equals=" + categoriesId);

        // Get all the listingList where categories equals to (categoriesId + 1)
        defaultListingShouldNotBeFound("categoriesId.equals=" + (categoriesId + 1));
    }

    @Test
    @Transactional
    void getAllListingsByLocationsIsEqualToSomething() throws Exception {
        Location locations;
        if (TestUtil.findAll(em, Location.class).isEmpty()) {
            listingRepository.saveAndFlush(listing);
            locations = LocationResourceIT.createEntity();
        } else {
            locations = TestUtil.findAll(em, Location.class).get(0);
        }
        em.persist(locations);
        em.flush();
        listing.addLocations(locations);
        listingRepository.saveAndFlush(listing);
        Long locationsId = locations.getId();
        // Get all the listingList where locations equals to locationsId
        defaultListingShouldBeFound("locationsId.equals=" + locationsId);

        // Get all the listingList where locations equals to (locationsId + 1)
        defaultListingShouldNotBeFound("locationsId.equals=" + (locationsId + 1));
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
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].content").value(hasItem(DEFAULT_CONTENT)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS)))
            .andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL)))
            .andExpect(jsonPath("$.[*].telephone").value(hasItem(DEFAULT_TELEPHONE)))
            .andExpect(jsonPath("$.[*].mobile").value(hasItem(DEFAULT_MOBILE)))
            .andExpect(jsonPath("$.[*].website").value(hasItem(DEFAULT_WEBSITE)))
            .andExpect(jsonPath("$.[*].fax").value(hasItem(DEFAULT_FAX)))
            .andExpect(jsonPath("$.[*].taxCode").value(hasItem(DEFAULT_TAX_CODE)))
            .andExpect(jsonPath("$.[*].nameAlias").value(hasItem(DEFAULT_NAME_ALIAS)))
            .andExpect(jsonPath("$.[*].nameEn").value(hasItem(DEFAULT_NAME_EN)))
            .andExpect(jsonPath("$.[*].representative").value(hasItem(DEFAULT_REPRESENTATIVE)))
            .andExpect(jsonPath("$.[*].mainIndustry").value(hasItem(DEFAULT_MAIN_INDUSTRY)))
            .andExpect(jsonPath("$.[*].managedBy").value(hasItem(DEFAULT_MANAGED_BY)))
            .andExpect(jsonPath("$.[*].businessType").value(hasItem(DEFAULT_BUSINESS_TYPE)))
            .andExpect(jsonPath("$.[*].statusYp").value(hasItem(DEFAULT_STATUS_YP)))
            .andExpect(jsonPath("$.[*].foundedDate").value(hasItem(DEFAULT_FOUNDED_DATE.toString())))
            .andExpect(jsonPath("$.[*].licenseModifiedDate").value(hasItem(DEFAULT_LICENSE_MODIFIED_DATE.toString())))
            .andExpect(jsonPath("$.[*].address").value(hasItem(DEFAULT_ADDRESS)))
            .andExpect(jsonPath("$.[*].addressAlternative").value(hasItem(DEFAULT_ADDRESS_ALTERNATIVE)))
            .andExpect(jsonPath("$.[*].latitude").value(hasItem(sameNumber(DEFAULT_LATITUDE))))
            .andExpect(jsonPath("$.[*].longitude").value(hasItem(sameNumber(DEFAULT_LONGITUDE))))
            .andExpect(jsonPath("$.[*].apiId").value(hasItem(DEFAULT_API_ID.intValue())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));

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
            .title(UPDATED_TITLE)
            .slug(UPDATED_SLUG)
            .content(UPDATED_CONTENT)
            .status(UPDATED_STATUS)
            .email(UPDATED_EMAIL)
            .telephone(UPDATED_TELEPHONE)
            .mobile(UPDATED_MOBILE)
            .website(UPDATED_WEBSITE)
            .fax(UPDATED_FAX)
            .taxCode(UPDATED_TAX_CODE)
            .nameAlias(UPDATED_NAME_ALIAS)
            .nameEn(UPDATED_NAME_EN)
            .representative(UPDATED_REPRESENTATIVE)
            .mainIndustry(UPDATED_MAIN_INDUSTRY)
            .managedBy(UPDATED_MANAGED_BY)
            .businessType(UPDATED_BUSINESS_TYPE)
            .statusYp(UPDATED_STATUS_YP)
            .foundedDate(UPDATED_FOUNDED_DATE)
            .licenseModifiedDate(UPDATED_LICENSE_MODIFIED_DATE)
            .address(UPDATED_ADDRESS)
            .addressAlternative(UPDATED_ADDRESS_ALTERNATIVE)
            .latitude(UPDATED_LATITUDE)
            .longitude(UPDATED_LONGITUDE)
            .apiId(UPDATED_API_ID)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
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
            .status(UPDATED_STATUS)
            .email(UPDATED_EMAIL)
            .mobile(UPDATED_MOBILE)
            .taxCode(UPDATED_TAX_CODE)
            .nameEn(UPDATED_NAME_EN)
            .representative(UPDATED_REPRESENTATIVE)
            .mainIndustry(UPDATED_MAIN_INDUSTRY)
            .address(UPDATED_ADDRESS)
            .latitude(UPDATED_LATITUDE)
            .longitude(UPDATED_LONGITUDE)
            .apiId(UPDATED_API_ID)
            .createdAt(UPDATED_CREATED_AT);

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
            .title(UPDATED_TITLE)
            .slug(UPDATED_SLUG)
            .content(UPDATED_CONTENT)
            .status(UPDATED_STATUS)
            .email(UPDATED_EMAIL)
            .telephone(UPDATED_TELEPHONE)
            .mobile(UPDATED_MOBILE)
            .website(UPDATED_WEBSITE)
            .fax(UPDATED_FAX)
            .taxCode(UPDATED_TAX_CODE)
            .nameAlias(UPDATED_NAME_ALIAS)
            .nameEn(UPDATED_NAME_EN)
            .representative(UPDATED_REPRESENTATIVE)
            .mainIndustry(UPDATED_MAIN_INDUSTRY)
            .managedBy(UPDATED_MANAGED_BY)
            .businessType(UPDATED_BUSINESS_TYPE)
            .statusYp(UPDATED_STATUS_YP)
            .foundedDate(UPDATED_FOUNDED_DATE)
            .licenseModifiedDate(UPDATED_LICENSE_MODIFIED_DATE)
            .address(UPDATED_ADDRESS)
            .addressAlternative(UPDATED_ADDRESS_ALTERNATIVE)
            .latitude(UPDATED_LATITUDE)
            .longitude(UPDATED_LONGITUDE)
            .apiId(UPDATED_API_ID)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);

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
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].content").value(hasItem(DEFAULT_CONTENT.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS)))
            .andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL)))
            .andExpect(jsonPath("$.[*].telephone").value(hasItem(DEFAULT_TELEPHONE)))
            .andExpect(jsonPath("$.[*].mobile").value(hasItem(DEFAULT_MOBILE)))
            .andExpect(jsonPath("$.[*].website").value(hasItem(DEFAULT_WEBSITE)))
            .andExpect(jsonPath("$.[*].fax").value(hasItem(DEFAULT_FAX)))
            .andExpect(jsonPath("$.[*].taxCode").value(hasItem(DEFAULT_TAX_CODE)))
            .andExpect(jsonPath("$.[*].nameAlias").value(hasItem(DEFAULT_NAME_ALIAS)))
            .andExpect(jsonPath("$.[*].nameEn").value(hasItem(DEFAULT_NAME_EN)))
            .andExpect(jsonPath("$.[*].representative").value(hasItem(DEFAULT_REPRESENTATIVE)))
            .andExpect(jsonPath("$.[*].mainIndustry").value(hasItem(DEFAULT_MAIN_INDUSTRY)))
            .andExpect(jsonPath("$.[*].managedBy").value(hasItem(DEFAULT_MANAGED_BY)))
            .andExpect(jsonPath("$.[*].businessType").value(hasItem(DEFAULT_BUSINESS_TYPE)))
            .andExpect(jsonPath("$.[*].statusYp").value(hasItem(DEFAULT_STATUS_YP)))
            .andExpect(jsonPath("$.[*].foundedDate").value(hasItem(DEFAULT_FOUNDED_DATE.toString())))
            .andExpect(jsonPath("$.[*].licenseModifiedDate").value(hasItem(DEFAULT_LICENSE_MODIFIED_DATE.toString())))
            .andExpect(jsonPath("$.[*].address").value(hasItem(DEFAULT_ADDRESS.toString())))
            .andExpect(jsonPath("$.[*].addressAlternative").value(hasItem(DEFAULT_ADDRESS_ALTERNATIVE.toString())))
            .andExpect(jsonPath("$.[*].latitude").value(hasItem(sameNumber(DEFAULT_LATITUDE))))
            .andExpect(jsonPath("$.[*].longitude").value(hasItem(sameNumber(DEFAULT_LONGITUDE))))
            .andExpect(jsonPath("$.[*].apiId").value(hasItem(DEFAULT_API_ID.intValue())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
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
