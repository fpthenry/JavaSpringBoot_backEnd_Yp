package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.GalleryAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Gallery;
import com.mycompany.myapp.repository.GalleryRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.repository.search.GallerySearchRepository;
import com.mycompany.myapp.service.GalleryService;
import com.mycompany.myapp.service.dto.GalleryDTO;
import com.mycompany.myapp.service.mapper.GalleryMapper;
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
 * Integration tests for the {@link GalleryResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class GalleryResourceIT {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_SLUG = "AAAAAAAAAA";
    private static final String UPDATED_SLUG = "BBBBBBBBBB";

    private static final String DEFAULT_CONTENT = "AAAAAAAAAA";
    private static final String UPDATED_CONTENT = "BBBBBBBBBB";

    private static final String DEFAULT_STATUS = "AAAAAAAAAA";
    private static final String UPDATED_STATUS = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1790760789812L);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.ofEpochMilli(1790760789812L);

    private static final String ENTITY_API_URL = "/api/galleries";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/galleries/_search";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private GalleryRepository galleryRepository;

    @Autowired
    private UserRepository userRepository;

    @Mock
    private GalleryRepository galleryRepositoryMock;

    @Autowired
    private GalleryMapper galleryMapper;

    @Mock
    private GalleryService galleryServiceMock;

    @Autowired
    private GallerySearchRepository gallerySearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restGalleryMockMvc;

    private Gallery gallery;

    private Gallery insertedGallery;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Gallery createEntity() {
        return new Gallery()
            .title(DEFAULT_TITLE)
            .slug(DEFAULT_SLUG)
            .content(DEFAULT_CONTENT)
            .status(DEFAULT_STATUS)
            .createdAt(DEFAULT_CREATED_AT)
            .updatedAt(DEFAULT_UPDATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Gallery createUpdatedEntity() {
        return new Gallery()
            .title(UPDATED_TITLE)
            .slug(UPDATED_SLUG)
            .content(UPDATED_CONTENT)
            .status(UPDATED_STATUS)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
    }

    @BeforeEach
    void initTest() {
        gallery = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedGallery != null) {
            galleryRepository.delete(insertedGallery);
            gallerySearchRepository.delete(insertedGallery);
            insertedGallery = null;
        }
    }

    @Test
    @Transactional
    void createGallery() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(gallerySearchRepository.findAll());
        // Create the Gallery
        GalleryDTO galleryDTO = galleryMapper.toDto(gallery);
        var returnedGalleryDTO = om.readValue(
            restGalleryMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(galleryDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            GalleryDTO.class
        );

        // Validate the Gallery in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedGallery = galleryMapper.toEntity(returnedGalleryDTO);
        assertGalleryUpdatableFieldsEquals(returnedGallery, getPersistedGallery(returnedGallery));

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(gallerySearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore + 1);
            });

        insertedGallery = returnedGallery;
    }

    @Test
    @Transactional
    void createGalleryWithExistingId() throws Exception {
        // Create the Gallery with an existing ID
        gallery.setId(1L);
        GalleryDTO galleryDTO = galleryMapper.toDto(gallery);

        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(gallerySearchRepository.findAll());

        // An entity with an existing ID cannot be created, so this API call must fail
        restGalleryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(galleryDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Gallery in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(gallerySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkTitleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(gallerySearchRepository.findAll());
        // set the field null
        gallery.setTitle(null);

        // Create the Gallery, which fails.
        GalleryDTO galleryDTO = galleryMapper.toDto(gallery);

        restGalleryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(galleryDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(gallerySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void getAllGalleries() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        // Get all the galleryList
        restGalleryMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(gallery.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].content").value(hasItem(DEFAULT_CONTENT)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllGalleriesWithEagerRelationshipsIsEnabled() throws Exception {
        when(galleryServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restGalleryMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(galleryServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllGalleriesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(galleryServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restGalleryMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(galleryRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getGallery() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        // Get the gallery
        restGalleryMockMvc
            .perform(get(ENTITY_API_URL_ID, gallery.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(gallery.getId().intValue()))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
            .andExpect(jsonPath("$.slug").value(DEFAULT_SLUG))
            .andExpect(jsonPath("$.content").value(DEFAULT_CONTENT))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingGallery() throws Exception {
        // Get the gallery
        restGalleryMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingGallery() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        long databaseSizeBeforeUpdate = getRepositoryCount();
        gallerySearchRepository.save(gallery);
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(gallerySearchRepository.findAll());

        // Update the gallery
        Gallery updatedGallery = galleryRepository.findById(gallery.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedGallery are not directly saved in db
        em.detach(updatedGallery);
        updatedGallery
            .title(UPDATED_TITLE)
            .slug(UPDATED_SLUG)
            .content(UPDATED_CONTENT)
            .status(UPDATED_STATUS)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
        GalleryDTO galleryDTO = galleryMapper.toDto(updatedGallery);

        restGalleryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, galleryDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(galleryDTO))
            )
            .andExpect(status().isOk());

        // Validate the Gallery in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedGalleryToMatchAllProperties(updatedGallery);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(gallerySearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
                List<Gallery> gallerySearchList = Streamable.of(gallerySearchRepository.findAll()).toList();
                Gallery testGallerySearch = gallerySearchList.get(searchDatabaseSizeAfter - 1);

                assertGalleryAllPropertiesEquals(testGallerySearch, updatedGallery);
            });
    }

    @Test
    @Transactional
    void putNonExistingGallery() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(gallerySearchRepository.findAll());
        gallery.setId(longCount.incrementAndGet());

        // Create the Gallery
        GalleryDTO galleryDTO = galleryMapper.toDto(gallery);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restGalleryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, galleryDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(galleryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Gallery in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(gallerySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithIdMismatchGallery() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(gallerySearchRepository.findAll());
        gallery.setId(longCount.incrementAndGet());

        // Create the Gallery
        GalleryDTO galleryDTO = galleryMapper.toDto(gallery);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGalleryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(galleryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Gallery in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(gallerySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamGallery() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(gallerySearchRepository.findAll());
        gallery.setId(longCount.incrementAndGet());

        // Create the Gallery
        GalleryDTO galleryDTO = galleryMapper.toDto(gallery);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGalleryMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(galleryDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Gallery in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(gallerySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void partialUpdateGalleryWithPatch() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the gallery using partial update
        Gallery partialUpdatedGallery = new Gallery();
        partialUpdatedGallery.setId(gallery.getId());

        partialUpdatedGallery.updatedAt(UPDATED_UPDATED_AT);

        restGalleryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedGallery.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedGallery))
            )
            .andExpect(status().isOk());

        // Validate the Gallery in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertGalleryUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedGallery, gallery), getPersistedGallery(gallery));
    }

    @Test
    @Transactional
    void fullUpdateGalleryWithPatch() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the gallery using partial update
        Gallery partialUpdatedGallery = new Gallery();
        partialUpdatedGallery.setId(gallery.getId());

        partialUpdatedGallery
            .title(UPDATED_TITLE)
            .slug(UPDATED_SLUG)
            .content(UPDATED_CONTENT)
            .status(UPDATED_STATUS)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);

        restGalleryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedGallery.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedGallery))
            )
            .andExpect(status().isOk());

        // Validate the Gallery in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertGalleryUpdatableFieldsEquals(partialUpdatedGallery, getPersistedGallery(partialUpdatedGallery));
    }

    @Test
    @Transactional
    void patchNonExistingGallery() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(gallerySearchRepository.findAll());
        gallery.setId(longCount.incrementAndGet());

        // Create the Gallery
        GalleryDTO galleryDTO = galleryMapper.toDto(gallery);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restGalleryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, galleryDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(galleryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Gallery in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(gallerySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithIdMismatchGallery() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(gallerySearchRepository.findAll());
        gallery.setId(longCount.incrementAndGet());

        // Create the Gallery
        GalleryDTO galleryDTO = galleryMapper.toDto(gallery);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGalleryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(galleryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Gallery in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(gallerySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamGallery() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(gallerySearchRepository.findAll());
        gallery.setId(longCount.incrementAndGet());

        // Create the Gallery
        GalleryDTO galleryDTO = galleryMapper.toDto(gallery);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGalleryMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(galleryDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Gallery in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(gallerySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void deleteGallery() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);
        galleryRepository.save(gallery);
        gallerySearchRepository.save(gallery);

        long databaseSizeBeforeDelete = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(gallerySearchRepository.findAll());
        assertThat(searchDatabaseSizeBefore).isEqualTo(databaseSizeBeforeDelete);

        // Delete the gallery
        restGalleryMockMvc
            .perform(delete(ENTITY_API_URL_ID, gallery.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(gallerySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore - 1);
    }

    @Test
    @Transactional
    void searchGallery() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);
        gallerySearchRepository.save(gallery);

        // Search the gallery
        restGalleryMockMvc
            .perform(get(ENTITY_SEARCH_API_URL + "?query=id:" + gallery.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(gallery.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].content").value(hasItem(DEFAULT_CONTENT.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    protected long getRepositoryCount() {
        return galleryRepository.count();
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

    protected Gallery getPersistedGallery(Gallery gallery) {
        return galleryRepository.findById(gallery.getId()).orElseThrow();
    }

    protected void assertPersistedGalleryToMatchAllProperties(Gallery expectedGallery) {
        assertGalleryAllPropertiesEquals(expectedGallery, getPersistedGallery(expectedGallery));
    }

    protected void assertPersistedGalleryToMatchUpdatableProperties(Gallery expectedGallery) {
        assertGalleryAllUpdatablePropertiesEquals(expectedGallery, getPersistedGallery(expectedGallery));
    }
}
