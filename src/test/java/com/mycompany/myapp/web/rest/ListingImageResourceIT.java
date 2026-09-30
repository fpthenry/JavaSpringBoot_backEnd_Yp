package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.ListingImageAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Gallery;
import com.mycompany.myapp.domain.Listing;
import com.mycompany.myapp.domain.ListingImage;
import com.mycompany.myapp.repository.ListingImageRepository;
import com.mycompany.myapp.repository.search.ListingImageSearchRepository;
import com.mycompany.myapp.service.ListingImageService;
import com.mycompany.myapp.service.dto.ListingImageDTO;
import com.mycompany.myapp.service.mapper.ListingImageMapper;
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
 * Integration tests for the {@link ListingImageResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ListingImageResourceIT {

    private static final String DEFAULT_IMAGE_URL = "AAAAAAAAAA";
    private static final String UPDATED_IMAGE_URL = "BBBBBBBBBB";

    private static final String DEFAULT_THUMBNAIL_URL = "AAAAAAAAAA";
    private static final String UPDATED_THUMBNAIL_URL = "BBBBBBBBBB";

    private static final String DEFAULT_ALT_TEXT = "AAAAAAAAAA";
    private static final String UPDATED_ALT_TEXT = "BBBBBBBBBB";

    private static final Integer DEFAULT_DISPLAY_ORDER = 0;
    private static final Integer UPDATED_DISPLAY_ORDER = 1;
    private static final Integer SMALLER_DISPLAY_ORDER = 0 - 1;

    private static final Boolean DEFAULT_IS_FEATURED = false;
    private static final Boolean UPDATED_IS_FEATURED = true;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1790760789812L);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.ofEpochMilli(1790760789812L);

    private static final String ENTITY_API_URL = "/api/listing-images";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/listing-images/_search";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ListingImageRepository listingImageRepository;

    @Mock
    private ListingImageRepository listingImageRepositoryMock;

    @Autowired
    private ListingImageMapper listingImageMapper;

    @Mock
    private ListingImageService listingImageServiceMock;

    @Autowired
    private ListingImageSearchRepository listingImageSearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restListingImageMockMvc;

    private ListingImage listingImage;

    private ListingImage insertedListingImage;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ListingImage createEntity(EntityManager em) {
        ListingImage listingImage = new ListingImage()
            .imageUrl(DEFAULT_IMAGE_URL)
            .thumbnailUrl(DEFAULT_THUMBNAIL_URL)
            .altText(DEFAULT_ALT_TEXT)
            .displayOrder(DEFAULT_DISPLAY_ORDER)
            .isFeatured(DEFAULT_IS_FEATURED)
            .createdAt(DEFAULT_CREATED_AT)
            .updatedAt(DEFAULT_UPDATED_AT);
        // Add required entity
        Listing listing;
        if (TestUtil.findAll(em, Listing.class).isEmpty()) {
            listing = ListingResourceIT.createEntity();
            em.persist(listing);
            em.flush();
        } else {
            listing = TestUtil.findAll(em, Listing.class).get(0);
        }
        listingImage.setListing(listing);
        return listingImage;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ListingImage createUpdatedEntity(EntityManager em) {
        ListingImage updatedListingImage = new ListingImage()
            .imageUrl(UPDATED_IMAGE_URL)
            .thumbnailUrl(UPDATED_THUMBNAIL_URL)
            .altText(UPDATED_ALT_TEXT)
            .displayOrder(UPDATED_DISPLAY_ORDER)
            .isFeatured(UPDATED_IS_FEATURED)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
        // Add required entity
        Listing listing;
        if (TestUtil.findAll(em, Listing.class).isEmpty()) {
            listing = ListingResourceIT.createUpdatedEntity();
            em.persist(listing);
            em.flush();
        } else {
            listing = TestUtil.findAll(em, Listing.class).get(0);
        }
        updatedListingImage.setListing(listing);
        return updatedListingImage;
    }

    @BeforeEach
    void initTest() {
        listingImage = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedListingImage != null) {
            listingImageRepository.delete(insertedListingImage);
            listingImageSearchRepository.delete(insertedListingImage);
            insertedListingImage = null;
        }
    }

    @Test
    @Transactional
    void createListingImage() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(listingImageSearchRepository.findAll());
        // Create the ListingImage
        ListingImageDTO listingImageDTO = listingImageMapper.toDto(listingImage);
        var returnedListingImageDTO = om.readValue(
            restListingImageMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(listingImageDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ListingImageDTO.class
        );

        // Validate the ListingImage in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedListingImage = listingImageMapper.toEntity(returnedListingImageDTO);
        assertListingImageUpdatableFieldsEquals(returnedListingImage, getPersistedListingImage(returnedListingImage));

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(listingImageSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore + 1);
            });

        insertedListingImage = returnedListingImage;
    }

    @Test
    @Transactional
    void createListingImageWithExistingId() throws Exception {
        // Create the ListingImage with an existing ID
        listingImage.setId(1L);
        ListingImageDTO listingImageDTO = listingImageMapper.toDto(listingImage);

        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(listingImageSearchRepository.findAll());

        // An entity with an existing ID cannot be created, so this API call must fail
        restListingImageMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(listingImageDTO)))
            .andExpect(status().isBadRequest());

        // Validate the ListingImage in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(listingImageSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void getAllListingImages() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        // Get all the listingImageList
        restListingImageMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(listingImage.getId().intValue())))
            .andExpect(jsonPath("$.[*].imageUrl").value(hasItem(DEFAULT_IMAGE_URL)))
            .andExpect(jsonPath("$.[*].thumbnailUrl").value(hasItem(DEFAULT_THUMBNAIL_URL)))
            .andExpect(jsonPath("$.[*].altText").value(hasItem(DEFAULT_ALT_TEXT)))
            .andExpect(jsonPath("$.[*].displayOrder").value(hasItem(DEFAULT_DISPLAY_ORDER)))
            .andExpect(jsonPath("$.[*].isFeatured").value(hasItem(DEFAULT_IS_FEATURED)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllListingImagesWithEagerRelationshipsIsEnabled() throws Exception {
        when(listingImageServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restListingImageMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(listingImageServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllListingImagesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(listingImageServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restListingImageMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(listingImageRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getListingImage() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        // Get the listingImage
        restListingImageMockMvc
            .perform(get(ENTITY_API_URL_ID, listingImage.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(listingImage.getId().intValue()))
            .andExpect(jsonPath("$.imageUrl").value(DEFAULT_IMAGE_URL))
            .andExpect(jsonPath("$.thumbnailUrl").value(DEFAULT_THUMBNAIL_URL))
            .andExpect(jsonPath("$.altText").value(DEFAULT_ALT_TEXT))
            .andExpect(jsonPath("$.displayOrder").value(DEFAULT_DISPLAY_ORDER))
            .andExpect(jsonPath("$.isFeatured").value(DEFAULT_IS_FEATURED))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()));
    }

    @Test
    @Transactional
    void getListingImagesByIdFiltering() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        Long id = listingImage.getId();

        defaultListingImageFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultListingImageFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultListingImageFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllListingImagesByAltTextIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        // Get all the listingImageList where altText equals to
        defaultListingImageFiltering("altText.equals=" + DEFAULT_ALT_TEXT, "altText.equals=" + UPDATED_ALT_TEXT);
    }

    @Test
    @Transactional
    void getAllListingImagesByAltTextIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        // Get all the listingImageList where altText in
        defaultListingImageFiltering("altText.in=" + DEFAULT_ALT_TEXT + "," + UPDATED_ALT_TEXT, "altText.in=" + UPDATED_ALT_TEXT);
    }

    @Test
    @Transactional
    void getAllListingImagesByAltTextIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        // Get all the listingImageList where altText is not null
        defaultListingImageFiltering("altText.specified=true", "altText.specified=false");
    }

    @Test
    @Transactional
    void getAllListingImagesByAltTextContainsSomething() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        // Get all the listingImageList where altText contains
        defaultListingImageFiltering("altText.contains=" + DEFAULT_ALT_TEXT, "altText.contains=" + UPDATED_ALT_TEXT);
    }

    @Test
    @Transactional
    void getAllListingImagesByAltTextNotContainsSomething() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        // Get all the listingImageList where altText does not contain
        defaultListingImageFiltering("altText.doesNotContain=" + UPDATED_ALT_TEXT, "altText.doesNotContain=" + DEFAULT_ALT_TEXT);
    }

    @Test
    @Transactional
    void getAllListingImagesByDisplayOrderIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        // Get all the listingImageList where displayOrder equals to
        defaultListingImageFiltering("displayOrder.equals=" + DEFAULT_DISPLAY_ORDER, "displayOrder.equals=" + UPDATED_DISPLAY_ORDER);
    }

    @Test
    @Transactional
    void getAllListingImagesByDisplayOrderIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        // Get all the listingImageList where displayOrder in
        defaultListingImageFiltering(
            "displayOrder.in=" + DEFAULT_DISPLAY_ORDER + "," + UPDATED_DISPLAY_ORDER,
            "displayOrder.in=" + UPDATED_DISPLAY_ORDER
        );
    }

    @Test
    @Transactional
    void getAllListingImagesByDisplayOrderIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        // Get all the listingImageList where displayOrder is not null
        defaultListingImageFiltering("displayOrder.specified=true", "displayOrder.specified=false");
    }

    @Test
    @Transactional
    void getAllListingImagesByDisplayOrderIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        // Get all the listingImageList where displayOrder is greater than or equal to
        defaultListingImageFiltering(
            "displayOrder.greaterThanOrEqual=" + DEFAULT_DISPLAY_ORDER,
            "displayOrder.greaterThanOrEqual=" + UPDATED_DISPLAY_ORDER
        );
    }

    @Test
    @Transactional
    void getAllListingImagesByDisplayOrderIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        // Get all the listingImageList where displayOrder is less than or equal to
        defaultListingImageFiltering(
            "displayOrder.lessThanOrEqual=" + DEFAULT_DISPLAY_ORDER,
            "displayOrder.lessThanOrEqual=" + SMALLER_DISPLAY_ORDER
        );
    }

    @Test
    @Transactional
    void getAllListingImagesByDisplayOrderIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        // Get all the listingImageList where displayOrder is less than
        defaultListingImageFiltering("displayOrder.lessThan=" + UPDATED_DISPLAY_ORDER, "displayOrder.lessThan=" + DEFAULT_DISPLAY_ORDER);
    }

    @Test
    @Transactional
    void getAllListingImagesByDisplayOrderIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        // Get all the listingImageList where displayOrder is greater than
        defaultListingImageFiltering(
            "displayOrder.greaterThan=" + SMALLER_DISPLAY_ORDER,
            "displayOrder.greaterThan=" + DEFAULT_DISPLAY_ORDER
        );
    }

    @Test
    @Transactional
    void getAllListingImagesByIsFeaturedIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        // Get all the listingImageList where isFeatured equals to
        defaultListingImageFiltering("isFeatured.equals=" + DEFAULT_IS_FEATURED, "isFeatured.equals=" + UPDATED_IS_FEATURED);
    }

    @Test
    @Transactional
    void getAllListingImagesByIsFeaturedIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        // Get all the listingImageList where isFeatured in
        defaultListingImageFiltering(
            "isFeatured.in=" + DEFAULT_IS_FEATURED + "," + UPDATED_IS_FEATURED,
            "isFeatured.in=" + UPDATED_IS_FEATURED
        );
    }

    @Test
    @Transactional
    void getAllListingImagesByIsFeaturedIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        // Get all the listingImageList where isFeatured is not null
        defaultListingImageFiltering("isFeatured.specified=true", "isFeatured.specified=false");
    }

    @Test
    @Transactional
    void getAllListingImagesByCreatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        // Get all the listingImageList where createdAt equals to
        defaultListingImageFiltering("createdAt.equals=" + DEFAULT_CREATED_AT, "createdAt.equals=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllListingImagesByCreatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        // Get all the listingImageList where createdAt in
        defaultListingImageFiltering("createdAt.in=" + DEFAULT_CREATED_AT + "," + UPDATED_CREATED_AT, "createdAt.in=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllListingImagesByCreatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        // Get all the listingImageList where createdAt is not null
        defaultListingImageFiltering("createdAt.specified=true", "createdAt.specified=false");
    }

    @Test
    @Transactional
    void getAllListingImagesByUpdatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        // Get all the listingImageList where updatedAt equals to
        defaultListingImageFiltering("updatedAt.equals=" + DEFAULT_UPDATED_AT, "updatedAt.equals=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllListingImagesByUpdatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        // Get all the listingImageList where updatedAt in
        defaultListingImageFiltering("updatedAt.in=" + DEFAULT_UPDATED_AT + "," + UPDATED_UPDATED_AT, "updatedAt.in=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllListingImagesByUpdatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        // Get all the listingImageList where updatedAt is not null
        defaultListingImageFiltering("updatedAt.specified=true", "updatedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllListingImagesByListingIsEqualToSomething() throws Exception {
        Listing listing;
        if (TestUtil.findAll(em, Listing.class).isEmpty()) {
            listingImageRepository.saveAndFlush(listingImage);
            listing = ListingResourceIT.createEntity();
        } else {
            listing = TestUtil.findAll(em, Listing.class).get(0);
        }
        em.persist(listing);
        em.flush();
        listingImage.setListing(listing);
        listingImageRepository.saveAndFlush(listingImage);
        Long listingId = listing.getId();
        // Get all the listingImageList where listing equals to listingId
        defaultListingImageShouldBeFound("listingId.equals=" + listingId);

        // Get all the listingImageList where listing equals to (listingId + 1)
        defaultListingImageShouldNotBeFound("listingId.equals=" + (listingId + 1));
    }

    @Test
    @Transactional
    void getAllListingImagesByGalleryIsEqualToSomething() throws Exception {
        Gallery gallery;
        if (TestUtil.findAll(em, Gallery.class).isEmpty()) {
            listingImageRepository.saveAndFlush(listingImage);
            gallery = GalleryResourceIT.createEntity();
        } else {
            gallery = TestUtil.findAll(em, Gallery.class).get(0);
        }
        em.persist(gallery);
        em.flush();
        listingImage.setGallery(gallery);
        listingImageRepository.saveAndFlush(listingImage);
        Long galleryId = gallery.getId();
        // Get all the listingImageList where gallery equals to galleryId
        defaultListingImageShouldBeFound("galleryId.equals=" + galleryId);

        // Get all the listingImageList where gallery equals to (galleryId + 1)
        defaultListingImageShouldNotBeFound("galleryId.equals=" + (galleryId + 1));
    }

    private void defaultListingImageFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultListingImageShouldBeFound(shouldBeFound);
        defaultListingImageShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultListingImageShouldBeFound(String filter) throws Exception {
        restListingImageMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(listingImage.getId().intValue())))
            .andExpect(jsonPath("$.[*].imageUrl").value(hasItem(DEFAULT_IMAGE_URL)))
            .andExpect(jsonPath("$.[*].thumbnailUrl").value(hasItem(DEFAULT_THUMBNAIL_URL)))
            .andExpect(jsonPath("$.[*].altText").value(hasItem(DEFAULT_ALT_TEXT)))
            .andExpect(jsonPath("$.[*].displayOrder").value(hasItem(DEFAULT_DISPLAY_ORDER)))
            .andExpect(jsonPath("$.[*].isFeatured").value(hasItem(DEFAULT_IS_FEATURED)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));

        // Check, that the count call also returns 1
        restListingImageMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultListingImageShouldNotBeFound(String filter) throws Exception {
        restListingImageMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restListingImageMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingListingImage() throws Exception {
        // Get the listingImage
        restListingImageMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingListingImage() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        long databaseSizeBeforeUpdate = getRepositoryCount();
        listingImageSearchRepository.save(listingImage);
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(listingImageSearchRepository.findAll());

        // Update the listingImage
        ListingImage updatedListingImage = listingImageRepository.findById(listingImage.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedListingImage are not directly saved in db
        em.detach(updatedListingImage);
        updatedListingImage
            .imageUrl(UPDATED_IMAGE_URL)
            .thumbnailUrl(UPDATED_THUMBNAIL_URL)
            .altText(UPDATED_ALT_TEXT)
            .displayOrder(UPDATED_DISPLAY_ORDER)
            .isFeatured(UPDATED_IS_FEATURED)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
        ListingImageDTO listingImageDTO = listingImageMapper.toDto(updatedListingImage);

        restListingImageMockMvc
            .perform(
                put(ENTITY_API_URL_ID, listingImageDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(listingImageDTO))
            )
            .andExpect(status().isOk());

        // Validate the ListingImage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedListingImageToMatchAllProperties(updatedListingImage);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(listingImageSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
                List<ListingImage> listingImageSearchList = Streamable.of(listingImageSearchRepository.findAll()).toList();
                ListingImage testListingImageSearch = listingImageSearchList.get(searchDatabaseSizeAfter - 1);

                assertListingImageAllPropertiesEquals(testListingImageSearch, updatedListingImage);
            });
    }

    @Test
    @Transactional
    void putNonExistingListingImage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(listingImageSearchRepository.findAll());
        listingImage.setId(longCount.incrementAndGet());

        // Create the ListingImage
        ListingImageDTO listingImageDTO = listingImageMapper.toDto(listingImage);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restListingImageMockMvc
            .perform(
                put(ENTITY_API_URL_ID, listingImageDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(listingImageDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ListingImage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(listingImageSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithIdMismatchListingImage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(listingImageSearchRepository.findAll());
        listingImage.setId(longCount.incrementAndGet());

        // Create the ListingImage
        ListingImageDTO listingImageDTO = listingImageMapper.toDto(listingImage);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restListingImageMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(listingImageDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ListingImage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(listingImageSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamListingImage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(listingImageSearchRepository.findAll());
        listingImage.setId(longCount.incrementAndGet());

        // Create the ListingImage
        ListingImageDTO listingImageDTO = listingImageMapper.toDto(listingImage);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restListingImageMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(listingImageDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ListingImage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(listingImageSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void partialUpdateListingImageWithPatch() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the listingImage using partial update
        ListingImage partialUpdatedListingImage = new ListingImage();
        partialUpdatedListingImage.setId(listingImage.getId());

        partialUpdatedListingImage.thumbnailUrl(UPDATED_THUMBNAIL_URL).isFeatured(UPDATED_IS_FEATURED).updatedAt(UPDATED_UPDATED_AT);

        restListingImageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedListingImage.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedListingImage))
            )
            .andExpect(status().isOk());

        // Validate the ListingImage in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertListingImageUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedListingImage, listingImage),
            getPersistedListingImage(listingImage)
        );
    }

    @Test
    @Transactional
    void fullUpdateListingImageWithPatch() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the listingImage using partial update
        ListingImage partialUpdatedListingImage = new ListingImage();
        partialUpdatedListingImage.setId(listingImage.getId());

        partialUpdatedListingImage
            .imageUrl(UPDATED_IMAGE_URL)
            .thumbnailUrl(UPDATED_THUMBNAIL_URL)
            .altText(UPDATED_ALT_TEXT)
            .displayOrder(UPDATED_DISPLAY_ORDER)
            .isFeatured(UPDATED_IS_FEATURED)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);

        restListingImageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedListingImage.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedListingImage))
            )
            .andExpect(status().isOk());

        // Validate the ListingImage in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertListingImageUpdatableFieldsEquals(partialUpdatedListingImage, getPersistedListingImage(partialUpdatedListingImage));
    }

    @Test
    @Transactional
    void patchNonExistingListingImage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(listingImageSearchRepository.findAll());
        listingImage.setId(longCount.incrementAndGet());

        // Create the ListingImage
        ListingImageDTO listingImageDTO = listingImageMapper.toDto(listingImage);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restListingImageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, listingImageDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(listingImageDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ListingImage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(listingImageSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithIdMismatchListingImage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(listingImageSearchRepository.findAll());
        listingImage.setId(longCount.incrementAndGet());

        // Create the ListingImage
        ListingImageDTO listingImageDTO = listingImageMapper.toDto(listingImage);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restListingImageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(listingImageDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ListingImage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(listingImageSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamListingImage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(listingImageSearchRepository.findAll());
        listingImage.setId(longCount.incrementAndGet());

        // Create the ListingImage
        ListingImageDTO listingImageDTO = listingImageMapper.toDto(listingImage);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restListingImageMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(listingImageDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ListingImage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(listingImageSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void deleteListingImage() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);
        listingImageRepository.save(listingImage);
        listingImageSearchRepository.save(listingImage);

        long databaseSizeBeforeDelete = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(listingImageSearchRepository.findAll());
        assertThat(searchDatabaseSizeBefore).isEqualTo(databaseSizeBeforeDelete);

        // Delete the listingImage
        restListingImageMockMvc
            .perform(delete(ENTITY_API_URL_ID, listingImage.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(listingImageSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore - 1);
    }

    @Test
    @Transactional
    void searchListingImage() throws Exception {
        // Initialize the database
        insertedListingImage = listingImageRepository.saveAndFlush(listingImage);
        listingImageSearchRepository.save(listingImage);

        // Search the listingImage
        restListingImageMockMvc
            .perform(get(ENTITY_SEARCH_API_URL + "?query=id:" + listingImage.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(listingImage.getId().intValue())))
            .andExpect(jsonPath("$.[*].imageUrl").value(hasItem(DEFAULT_IMAGE_URL.toString())))
            .andExpect(jsonPath("$.[*].thumbnailUrl").value(hasItem(DEFAULT_THUMBNAIL_URL.toString())))
            .andExpect(jsonPath("$.[*].altText").value(hasItem(DEFAULT_ALT_TEXT)))
            .andExpect(jsonPath("$.[*].displayOrder").value(hasItem(DEFAULT_DISPLAY_ORDER)))
            .andExpect(jsonPath("$.[*].isFeatured").value(hasItem(DEFAULT_IS_FEATURED)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    protected long getRepositoryCount() {
        return listingImageRepository.count();
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

    protected ListingImage getPersistedListingImage(ListingImage listingImage) {
        return listingImageRepository.findById(listingImage.getId()).orElseThrow();
    }

    protected void assertPersistedListingImageToMatchAllProperties(ListingImage expectedListingImage) {
        assertListingImageAllPropertiesEquals(expectedListingImage, getPersistedListingImage(expectedListingImage));
    }

    protected void assertPersistedListingImageToMatchUpdatableProperties(ListingImage expectedListingImage) {
        assertListingImageAllUpdatablePropertiesEquals(expectedListingImage, getPersistedListingImage(expectedListingImage));
    }
}
