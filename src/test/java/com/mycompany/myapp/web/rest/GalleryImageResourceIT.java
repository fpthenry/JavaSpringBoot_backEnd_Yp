package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.GalleryImageAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Gallery;
import com.mycompany.myapp.domain.GalleryImage;
import com.mycompany.myapp.repository.GalleryImageRepository;
import com.mycompany.myapp.service.GalleryImageService;
import com.mycompany.myapp.service.dto.GalleryImageDTO;
import com.mycompany.myapp.service.mapper.GalleryImageMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link GalleryImageResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class GalleryImageResourceIT {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final byte[] DEFAULT_IMAGE = TestUtil.createByteArray(1, "0");
    private static final byte[] UPDATED_IMAGE = TestUtil.createByteArray(1, "1");
    private static final String DEFAULT_IMAGE_CONTENT_TYPE = "image/jpg";
    private static final String UPDATED_IMAGE_CONTENT_TYPE = "image/png";

    private static final String DEFAULT_IMAGE_URL = "AAAAAAAAAA";
    private static final String UPDATED_IMAGE_URL = "BBBBBBBBBB";

    private static final String DEFAULT_LINK_URL = "AAAAAAAAAA";
    private static final String UPDATED_LINK_URL = "BBBBBBBBBB";

    private static final String DEFAULT_ALT_TEXT = "AAAAAAAAAA";
    private static final String UPDATED_ALT_TEXT = "BBBBBBBBBB";

    private static final Integer DEFAULT_DISPLAY_ORDER = 0;
    private static final Integer UPDATED_DISPLAY_ORDER = 1;
    private static final Integer SMALLER_DISPLAY_ORDER = 0 - 1;

    private static final Boolean DEFAULT_ACTIVE = false;
    private static final Boolean UPDATED_ACTIVE = true;

    private static final Instant DEFAULT_START_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_START_AT = Instant.ofEpochMilli(1790760789812L);

    private static final Instant DEFAULT_END_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_END_AT = Instant.ofEpochMilli(1790760789812L);

    private static final Boolean DEFAULT_OPEN_IN_NEW_TAB = false;
    private static final Boolean UPDATED_OPEN_IN_NEW_TAB = true;

    private static final String ENTITY_API_URL = "/api/gallery-images";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private GalleryImageRepository galleryImageRepository;

    @Mock
    private GalleryImageRepository galleryImageRepositoryMock;

    @Autowired
    private GalleryImageMapper galleryImageMapper;

    @Mock
    private GalleryImageService galleryImageServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restGalleryImageMockMvc;

    private GalleryImage galleryImage;

    private GalleryImage insertedGalleryImage;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static GalleryImage createEntity(EntityManager em) {
        GalleryImage galleryImage = new GalleryImage()
            .title(DEFAULT_TITLE)
            .image(DEFAULT_IMAGE)
            .imageContentType(DEFAULT_IMAGE_CONTENT_TYPE)
            .imageUrl(DEFAULT_IMAGE_URL)
            .linkUrl(DEFAULT_LINK_URL)
            .altText(DEFAULT_ALT_TEXT)
            .displayOrder(DEFAULT_DISPLAY_ORDER)
            .active(DEFAULT_ACTIVE)
            .startAt(DEFAULT_START_AT)
            .endAt(DEFAULT_END_AT)
            .openInNewTab(DEFAULT_OPEN_IN_NEW_TAB);
        // Add required entity
        Gallery gallery;
        if (TestUtil.findAll(em, Gallery.class).isEmpty()) {
            gallery = GalleryResourceIT.createEntity();
            em.persist(gallery);
            em.flush();
        } else {
            gallery = TestUtil.findAll(em, Gallery.class).get(0);
        }
        galleryImage.setGallery(gallery);
        return galleryImage;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static GalleryImage createUpdatedEntity(EntityManager em) {
        GalleryImage updatedGalleryImage = new GalleryImage()
            .title(UPDATED_TITLE)
            .image(UPDATED_IMAGE)
            .imageContentType(UPDATED_IMAGE_CONTENT_TYPE)
            .imageUrl(UPDATED_IMAGE_URL)
            .linkUrl(UPDATED_LINK_URL)
            .altText(UPDATED_ALT_TEXT)
            .displayOrder(UPDATED_DISPLAY_ORDER)
            .active(UPDATED_ACTIVE)
            .startAt(UPDATED_START_AT)
            .endAt(UPDATED_END_AT)
            .openInNewTab(UPDATED_OPEN_IN_NEW_TAB);
        // Add required entity
        Gallery gallery;
        if (TestUtil.findAll(em, Gallery.class).isEmpty()) {
            gallery = GalleryResourceIT.createUpdatedEntity();
            em.persist(gallery);
            em.flush();
        } else {
            gallery = TestUtil.findAll(em, Gallery.class).get(0);
        }
        updatedGalleryImage.setGallery(gallery);
        return updatedGalleryImage;
    }

    @BeforeEach
    void initTest() {
        galleryImage = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedGalleryImage != null) {
            galleryImageRepository.delete(insertedGalleryImage);
            insertedGalleryImage = null;
        }
    }

    @Test
    @Transactional
    void createGalleryImage() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the GalleryImage
        GalleryImageDTO galleryImageDTO = galleryImageMapper.toDto(galleryImage);
        var returnedGalleryImageDTO = om.readValue(
            restGalleryImageMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(galleryImageDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            GalleryImageDTO.class
        );

        // Validate the GalleryImage in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedGalleryImage = galleryImageMapper.toEntity(returnedGalleryImageDTO);
        assertGalleryImageUpdatableFieldsEquals(returnedGalleryImage, getPersistedGalleryImage(returnedGalleryImage));

        insertedGalleryImage = returnedGalleryImage;
    }

    @Test
    @Transactional
    void createGalleryImageWithExistingId() throws Exception {
        // Create the GalleryImage with an existing ID
        galleryImage.setId(1L);
        GalleryImageDTO galleryImageDTO = galleryImageMapper.toDto(galleryImage);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restGalleryImageMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(galleryImageDTO)))
            .andExpect(status().isBadRequest());

        // Validate the GalleryImage in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkDisplayOrderIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        galleryImage.setDisplayOrder(null);

        // Create the GalleryImage, which fails.
        GalleryImageDTO galleryImageDTO = galleryImageMapper.toDto(galleryImage);

        restGalleryImageMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(galleryImageDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActiveIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        galleryImage.setActive(null);

        // Create the GalleryImage, which fails.
        GalleryImageDTO galleryImageDTO = galleryImageMapper.toDto(galleryImage);

        restGalleryImageMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(galleryImageDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllGalleryImages() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList
        restGalleryImageMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(galleryImage.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].imageContentType").value(hasItem(DEFAULT_IMAGE_CONTENT_TYPE)))
            .andExpect(jsonPath("$.[*].image").value(hasItem(Base64.getEncoder().encodeToString(DEFAULT_IMAGE))))
            .andExpect(jsonPath("$.[*].imageUrl").value(hasItem(DEFAULT_IMAGE_URL)))
            .andExpect(jsonPath("$.[*].linkUrl").value(hasItem(DEFAULT_LINK_URL)))
            .andExpect(jsonPath("$.[*].altText").value(hasItem(DEFAULT_ALT_TEXT)))
            .andExpect(jsonPath("$.[*].displayOrder").value(hasItem(DEFAULT_DISPLAY_ORDER)))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)))
            .andExpect(jsonPath("$.[*].startAt").value(hasItem(DEFAULT_START_AT.toString())))
            .andExpect(jsonPath("$.[*].endAt").value(hasItem(DEFAULT_END_AT.toString())))
            .andExpect(jsonPath("$.[*].openInNewTab").value(hasItem(DEFAULT_OPEN_IN_NEW_TAB)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllGalleryImagesWithEagerRelationshipsIsEnabled() throws Exception {
        when(galleryImageServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restGalleryImageMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(galleryImageServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllGalleryImagesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(galleryImageServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restGalleryImageMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(galleryImageRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getGalleryImage() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get the galleryImage
        restGalleryImageMockMvc
            .perform(get(ENTITY_API_URL_ID, galleryImage.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(galleryImage.getId().intValue()))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
            .andExpect(jsonPath("$.imageContentType").value(DEFAULT_IMAGE_CONTENT_TYPE))
            .andExpect(jsonPath("$.image").value(Base64.getEncoder().encodeToString(DEFAULT_IMAGE)))
            .andExpect(jsonPath("$.imageUrl").value(DEFAULT_IMAGE_URL))
            .andExpect(jsonPath("$.linkUrl").value(DEFAULT_LINK_URL))
            .andExpect(jsonPath("$.altText").value(DEFAULT_ALT_TEXT))
            .andExpect(jsonPath("$.displayOrder").value(DEFAULT_DISPLAY_ORDER))
            .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE))
            .andExpect(jsonPath("$.startAt").value(DEFAULT_START_AT.toString()))
            .andExpect(jsonPath("$.endAt").value(DEFAULT_END_AT.toString()))
            .andExpect(jsonPath("$.openInNewTab").value(DEFAULT_OPEN_IN_NEW_TAB));
    }

    @Test
    @Transactional
    void getGalleryImagesByIdFiltering() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        Long id = galleryImage.getId();

        defaultGalleryImageFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultGalleryImageFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultGalleryImageFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByTitleIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where title equals to
        defaultGalleryImageFiltering("title.equals=" + DEFAULT_TITLE, "title.equals=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByTitleIsInShouldWork() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where title in
        defaultGalleryImageFiltering("title.in=" + DEFAULT_TITLE + "," + UPDATED_TITLE, "title.in=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByTitleIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where title is not null
        defaultGalleryImageFiltering("title.specified=true", "title.specified=false");
    }

    @Test
    @Transactional
    void getAllGalleryImagesByTitleContainsSomething() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where title contains
        defaultGalleryImageFiltering("title.contains=" + DEFAULT_TITLE, "title.contains=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByTitleNotContainsSomething() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where title does not contain
        defaultGalleryImageFiltering("title.doesNotContain=" + UPDATED_TITLE, "title.doesNotContain=" + DEFAULT_TITLE);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByImageUrlIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where imageUrl equals to
        defaultGalleryImageFiltering("imageUrl.equals=" + DEFAULT_IMAGE_URL, "imageUrl.equals=" + UPDATED_IMAGE_URL);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByImageUrlIsInShouldWork() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where imageUrl in
        defaultGalleryImageFiltering("imageUrl.in=" + DEFAULT_IMAGE_URL + "," + UPDATED_IMAGE_URL, "imageUrl.in=" + UPDATED_IMAGE_URL);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByImageUrlIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where imageUrl is not null
        defaultGalleryImageFiltering("imageUrl.specified=true", "imageUrl.specified=false");
    }

    @Test
    @Transactional
    void getAllGalleryImagesByImageUrlContainsSomething() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where imageUrl contains
        defaultGalleryImageFiltering("imageUrl.contains=" + DEFAULT_IMAGE_URL, "imageUrl.contains=" + UPDATED_IMAGE_URL);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByImageUrlNotContainsSomething() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where imageUrl does not contain
        defaultGalleryImageFiltering("imageUrl.doesNotContain=" + UPDATED_IMAGE_URL, "imageUrl.doesNotContain=" + DEFAULT_IMAGE_URL);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByLinkUrlIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where linkUrl equals to
        defaultGalleryImageFiltering("linkUrl.equals=" + DEFAULT_LINK_URL, "linkUrl.equals=" + UPDATED_LINK_URL);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByLinkUrlIsInShouldWork() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where linkUrl in
        defaultGalleryImageFiltering("linkUrl.in=" + DEFAULT_LINK_URL + "," + UPDATED_LINK_URL, "linkUrl.in=" + UPDATED_LINK_URL);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByLinkUrlIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where linkUrl is not null
        defaultGalleryImageFiltering("linkUrl.specified=true", "linkUrl.specified=false");
    }

    @Test
    @Transactional
    void getAllGalleryImagesByLinkUrlContainsSomething() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where linkUrl contains
        defaultGalleryImageFiltering("linkUrl.contains=" + DEFAULT_LINK_URL, "linkUrl.contains=" + UPDATED_LINK_URL);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByLinkUrlNotContainsSomething() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where linkUrl does not contain
        defaultGalleryImageFiltering("linkUrl.doesNotContain=" + UPDATED_LINK_URL, "linkUrl.doesNotContain=" + DEFAULT_LINK_URL);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByAltTextIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where altText equals to
        defaultGalleryImageFiltering("altText.equals=" + DEFAULT_ALT_TEXT, "altText.equals=" + UPDATED_ALT_TEXT);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByAltTextIsInShouldWork() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where altText in
        defaultGalleryImageFiltering("altText.in=" + DEFAULT_ALT_TEXT + "," + UPDATED_ALT_TEXT, "altText.in=" + UPDATED_ALT_TEXT);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByAltTextIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where altText is not null
        defaultGalleryImageFiltering("altText.specified=true", "altText.specified=false");
    }

    @Test
    @Transactional
    void getAllGalleryImagesByAltTextContainsSomething() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where altText contains
        defaultGalleryImageFiltering("altText.contains=" + DEFAULT_ALT_TEXT, "altText.contains=" + UPDATED_ALT_TEXT);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByAltTextNotContainsSomething() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where altText does not contain
        defaultGalleryImageFiltering("altText.doesNotContain=" + UPDATED_ALT_TEXT, "altText.doesNotContain=" + DEFAULT_ALT_TEXT);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByDisplayOrderIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where displayOrder equals to
        defaultGalleryImageFiltering("displayOrder.equals=" + DEFAULT_DISPLAY_ORDER, "displayOrder.equals=" + UPDATED_DISPLAY_ORDER);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByDisplayOrderIsInShouldWork() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where displayOrder in
        defaultGalleryImageFiltering(
            "displayOrder.in=" + DEFAULT_DISPLAY_ORDER + "," + UPDATED_DISPLAY_ORDER,
            "displayOrder.in=" + UPDATED_DISPLAY_ORDER
        );
    }

    @Test
    @Transactional
    void getAllGalleryImagesByDisplayOrderIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where displayOrder is not null
        defaultGalleryImageFiltering("displayOrder.specified=true", "displayOrder.specified=false");
    }

    @Test
    @Transactional
    void getAllGalleryImagesByDisplayOrderIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where displayOrder is greater than or equal to
        defaultGalleryImageFiltering(
            "displayOrder.greaterThanOrEqual=" + DEFAULT_DISPLAY_ORDER,
            "displayOrder.greaterThanOrEqual=" + UPDATED_DISPLAY_ORDER
        );
    }

    @Test
    @Transactional
    void getAllGalleryImagesByDisplayOrderIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where displayOrder is less than or equal to
        defaultGalleryImageFiltering(
            "displayOrder.lessThanOrEqual=" + DEFAULT_DISPLAY_ORDER,
            "displayOrder.lessThanOrEqual=" + SMALLER_DISPLAY_ORDER
        );
    }

    @Test
    @Transactional
    void getAllGalleryImagesByDisplayOrderIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where displayOrder is less than
        defaultGalleryImageFiltering("displayOrder.lessThan=" + UPDATED_DISPLAY_ORDER, "displayOrder.lessThan=" + DEFAULT_DISPLAY_ORDER);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByDisplayOrderIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where displayOrder is greater than
        defaultGalleryImageFiltering(
            "displayOrder.greaterThan=" + SMALLER_DISPLAY_ORDER,
            "displayOrder.greaterThan=" + DEFAULT_DISPLAY_ORDER
        );
    }

    @Test
    @Transactional
    void getAllGalleryImagesByActiveIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where active equals to
        defaultGalleryImageFiltering("active.equals=" + DEFAULT_ACTIVE, "active.equals=" + UPDATED_ACTIVE);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByActiveIsInShouldWork() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where active in
        defaultGalleryImageFiltering("active.in=" + DEFAULT_ACTIVE + "," + UPDATED_ACTIVE, "active.in=" + UPDATED_ACTIVE);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByActiveIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where active is not null
        defaultGalleryImageFiltering("active.specified=true", "active.specified=false");
    }

    @Test
    @Transactional
    void getAllGalleryImagesByStartAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where startAt equals to
        defaultGalleryImageFiltering("startAt.equals=" + DEFAULT_START_AT, "startAt.equals=" + UPDATED_START_AT);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByStartAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where startAt in
        defaultGalleryImageFiltering("startAt.in=" + DEFAULT_START_AT + "," + UPDATED_START_AT, "startAt.in=" + UPDATED_START_AT);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByStartAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where startAt is not null
        defaultGalleryImageFiltering("startAt.specified=true", "startAt.specified=false");
    }

    @Test
    @Transactional
    void getAllGalleryImagesByEndAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where endAt equals to
        defaultGalleryImageFiltering("endAt.equals=" + DEFAULT_END_AT, "endAt.equals=" + UPDATED_END_AT);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByEndAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where endAt in
        defaultGalleryImageFiltering("endAt.in=" + DEFAULT_END_AT + "," + UPDATED_END_AT, "endAt.in=" + UPDATED_END_AT);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByEndAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where endAt is not null
        defaultGalleryImageFiltering("endAt.specified=true", "endAt.specified=false");
    }

    @Test
    @Transactional
    void getAllGalleryImagesByOpenInNewTabIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where openInNewTab equals to
        defaultGalleryImageFiltering("openInNewTab.equals=" + DEFAULT_OPEN_IN_NEW_TAB, "openInNewTab.equals=" + UPDATED_OPEN_IN_NEW_TAB);
    }

    @Test
    @Transactional
    void getAllGalleryImagesByOpenInNewTabIsInShouldWork() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where openInNewTab in
        defaultGalleryImageFiltering(
            "openInNewTab.in=" + DEFAULT_OPEN_IN_NEW_TAB + "," + UPDATED_OPEN_IN_NEW_TAB,
            "openInNewTab.in=" + UPDATED_OPEN_IN_NEW_TAB
        );
    }

    @Test
    @Transactional
    void getAllGalleryImagesByOpenInNewTabIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        // Get all the galleryImageList where openInNewTab is not null
        defaultGalleryImageFiltering("openInNewTab.specified=true", "openInNewTab.specified=false");
    }

    @Test
    @Transactional
    void getAllGalleryImagesByGalleryIsEqualToSomething() throws Exception {
        Gallery gallery;
        if (TestUtil.findAll(em, Gallery.class).isEmpty()) {
            galleryImageRepository.saveAndFlush(galleryImage);
            gallery = GalleryResourceIT.createEntity();
        } else {
            gallery = TestUtil.findAll(em, Gallery.class).get(0);
        }
        em.persist(gallery);
        em.flush();
        galleryImage.setGallery(gallery);
        galleryImageRepository.saveAndFlush(galleryImage);
        Long galleryId = gallery.getId();
        // Get all the galleryImageList where gallery equals to galleryId
        defaultGalleryImageShouldBeFound("galleryId.equals=" + galleryId);

        // Get all the galleryImageList where gallery equals to (galleryId + 1)
        defaultGalleryImageShouldNotBeFound("galleryId.equals=" + (galleryId + 1));
    }

    private void defaultGalleryImageFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultGalleryImageShouldBeFound(shouldBeFound);
        defaultGalleryImageShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultGalleryImageShouldBeFound(String filter) throws Exception {
        restGalleryImageMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(galleryImage.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].imageContentType").value(hasItem(DEFAULT_IMAGE_CONTENT_TYPE)))
            .andExpect(jsonPath("$.[*].image").value(hasItem(Base64.getEncoder().encodeToString(DEFAULT_IMAGE))))
            .andExpect(jsonPath("$.[*].imageUrl").value(hasItem(DEFAULT_IMAGE_URL)))
            .andExpect(jsonPath("$.[*].linkUrl").value(hasItem(DEFAULT_LINK_URL)))
            .andExpect(jsonPath("$.[*].altText").value(hasItem(DEFAULT_ALT_TEXT)))
            .andExpect(jsonPath("$.[*].displayOrder").value(hasItem(DEFAULT_DISPLAY_ORDER)))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)))
            .andExpect(jsonPath("$.[*].startAt").value(hasItem(DEFAULT_START_AT.toString())))
            .andExpect(jsonPath("$.[*].endAt").value(hasItem(DEFAULT_END_AT.toString())))
            .andExpect(jsonPath("$.[*].openInNewTab").value(hasItem(DEFAULT_OPEN_IN_NEW_TAB)));

        // Check, that the count call also returns 1
        restGalleryImageMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultGalleryImageShouldNotBeFound(String filter) throws Exception {
        restGalleryImageMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restGalleryImageMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingGalleryImage() throws Exception {
        // Get the galleryImage
        restGalleryImageMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingGalleryImage() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the galleryImage
        GalleryImage updatedGalleryImage = galleryImageRepository.findById(galleryImage.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedGalleryImage are not directly saved in db
        em.detach(updatedGalleryImage);
        updatedGalleryImage
            .title(UPDATED_TITLE)
            .image(UPDATED_IMAGE)
            .imageContentType(UPDATED_IMAGE_CONTENT_TYPE)
            .imageUrl(UPDATED_IMAGE_URL)
            .linkUrl(UPDATED_LINK_URL)
            .altText(UPDATED_ALT_TEXT)
            .displayOrder(UPDATED_DISPLAY_ORDER)
            .active(UPDATED_ACTIVE)
            .startAt(UPDATED_START_AT)
            .endAt(UPDATED_END_AT)
            .openInNewTab(UPDATED_OPEN_IN_NEW_TAB);
        GalleryImageDTO galleryImageDTO = galleryImageMapper.toDto(updatedGalleryImage);

        restGalleryImageMockMvc
            .perform(
                put(ENTITY_API_URL_ID, galleryImageDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(galleryImageDTO))
            )
            .andExpect(status().isOk());

        // Validate the GalleryImage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedGalleryImageToMatchAllProperties(updatedGalleryImage);
    }

    @Test
    @Transactional
    void putNonExistingGalleryImage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        galleryImage.setId(longCount.incrementAndGet());

        // Create the GalleryImage
        GalleryImageDTO galleryImageDTO = galleryImageMapper.toDto(galleryImage);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restGalleryImageMockMvc
            .perform(
                put(ENTITY_API_URL_ID, galleryImageDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(galleryImageDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GalleryImage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchGalleryImage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        galleryImage.setId(longCount.incrementAndGet());

        // Create the GalleryImage
        GalleryImageDTO galleryImageDTO = galleryImageMapper.toDto(galleryImage);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGalleryImageMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(galleryImageDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GalleryImage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamGalleryImage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        galleryImage.setId(longCount.incrementAndGet());

        // Create the GalleryImage
        GalleryImageDTO galleryImageDTO = galleryImageMapper.toDto(galleryImage);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGalleryImageMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(galleryImageDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the GalleryImage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateGalleryImageWithPatch() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the galleryImage using partial update
        GalleryImage partialUpdatedGalleryImage = new GalleryImage();
        partialUpdatedGalleryImage.setId(galleryImage.getId());

        partialUpdatedGalleryImage
            .imageUrl(UPDATED_IMAGE_URL)
            .linkUrl(UPDATED_LINK_URL)
            .altText(UPDATED_ALT_TEXT)
            .active(UPDATED_ACTIVE)
            .endAt(UPDATED_END_AT)
            .openInNewTab(UPDATED_OPEN_IN_NEW_TAB);

        restGalleryImageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedGalleryImage.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedGalleryImage))
            )
            .andExpect(status().isOk());

        // Validate the GalleryImage in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertGalleryImageUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedGalleryImage, galleryImage),
            getPersistedGalleryImage(galleryImage)
        );
    }

    @Test
    @Transactional
    void fullUpdateGalleryImageWithPatch() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the galleryImage using partial update
        GalleryImage partialUpdatedGalleryImage = new GalleryImage();
        partialUpdatedGalleryImage.setId(galleryImage.getId());

        partialUpdatedGalleryImage
            .title(UPDATED_TITLE)
            .image(UPDATED_IMAGE)
            .imageContentType(UPDATED_IMAGE_CONTENT_TYPE)
            .imageUrl(UPDATED_IMAGE_URL)
            .linkUrl(UPDATED_LINK_URL)
            .altText(UPDATED_ALT_TEXT)
            .displayOrder(UPDATED_DISPLAY_ORDER)
            .active(UPDATED_ACTIVE)
            .startAt(UPDATED_START_AT)
            .endAt(UPDATED_END_AT)
            .openInNewTab(UPDATED_OPEN_IN_NEW_TAB);

        restGalleryImageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedGalleryImage.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedGalleryImage))
            )
            .andExpect(status().isOk());

        // Validate the GalleryImage in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertGalleryImageUpdatableFieldsEquals(partialUpdatedGalleryImage, getPersistedGalleryImage(partialUpdatedGalleryImage));
    }

    @Test
    @Transactional
    void patchNonExistingGalleryImage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        galleryImage.setId(longCount.incrementAndGet());

        // Create the GalleryImage
        GalleryImageDTO galleryImageDTO = galleryImageMapper.toDto(galleryImage);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restGalleryImageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, galleryImageDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(galleryImageDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GalleryImage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchGalleryImage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        galleryImage.setId(longCount.incrementAndGet());

        // Create the GalleryImage
        GalleryImageDTO galleryImageDTO = galleryImageMapper.toDto(galleryImage);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGalleryImageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(galleryImageDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the GalleryImage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamGalleryImage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        galleryImage.setId(longCount.incrementAndGet());

        // Create the GalleryImage
        GalleryImageDTO galleryImageDTO = galleryImageMapper.toDto(galleryImage);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGalleryImageMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(galleryImageDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the GalleryImage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteGalleryImage() throws Exception {
        // Initialize the database
        insertedGalleryImage = galleryImageRepository.saveAndFlush(galleryImage);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the galleryImage
        restGalleryImageMockMvc
            .perform(delete(ENTITY_API_URL_ID, galleryImage.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return galleryImageRepository.count();
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

    protected GalleryImage getPersistedGalleryImage(GalleryImage galleryImage) {
        return galleryImageRepository.findById(galleryImage.getId()).orElseThrow();
    }

    protected void assertPersistedGalleryImageToMatchAllProperties(GalleryImage expectedGalleryImage) {
        assertGalleryImageAllPropertiesEquals(expectedGalleryImage, getPersistedGalleryImage(expectedGalleryImage));
    }

    protected void assertPersistedGalleryImageToMatchUpdatableProperties(GalleryImage expectedGalleryImage) {
        assertGalleryImageAllUpdatablePropertiesEquals(expectedGalleryImage, getPersistedGalleryImage(expectedGalleryImage));
    }
}
