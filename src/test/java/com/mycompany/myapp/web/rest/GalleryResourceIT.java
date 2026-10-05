package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.GalleryAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Gallery;
import com.mycompany.myapp.repository.GalleryRepository;
import com.mycompany.myapp.service.dto.GalleryDTO;
import com.mycompany.myapp.service.mapper.GalleryMapper;
import jakarta.persistence.EntityManager;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link GalleryResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class GalleryResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_CODE = "s-4ze1";
    private static final String UPDATED_CODE = "yg7";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final Boolean DEFAULT_ACTIVE = false;
    private static final Boolean UPDATED_ACTIVE = true;

    private static final Long DEFAULT_WP_ID = 1L;
    private static final Long UPDATED_WP_ID = 2L;
    private static final Long SMALLER_WP_ID = 1L - 1L;

    private static final String ENTITY_API_URL = "/api/galleries";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private GalleryRepository galleryRepository;

    @Autowired
    private GalleryMapper galleryMapper;

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
            .name(DEFAULT_NAME)
            .code(DEFAULT_CODE)
            .description(DEFAULT_DESCRIPTION)
            .active(DEFAULT_ACTIVE)
            .wpId(DEFAULT_WP_ID);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Gallery createUpdatedEntity() {
        return new Gallery()
            .name(UPDATED_NAME)
            .code(UPDATED_CODE)
            .description(UPDATED_DESCRIPTION)
            .active(UPDATED_ACTIVE)
            .wpId(UPDATED_WP_ID);
    }

    @BeforeEach
    void initTest() {
        gallery = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedGallery != null) {
            galleryRepository.delete(insertedGallery);
            insertedGallery = null;
        }
    }

    @Test
    @Transactional
    void createGallery() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
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

        insertedGallery = returnedGallery;
    }

    @Test
    @Transactional
    void createGalleryWithExistingId() throws Exception {
        // Create the Gallery with an existing ID
        gallery.setId(1L);
        GalleryDTO galleryDTO = galleryMapper.toDto(gallery);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restGalleryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(galleryDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Gallery in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        gallery.setName(null);

        // Create the Gallery, which fails.
        GalleryDTO galleryDTO = galleryMapper.toDto(gallery);

        restGalleryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(galleryDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCodeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        gallery.setCode(null);

        // Create the Gallery, which fails.
        GalleryDTO galleryDTO = galleryMapper.toDto(gallery);

        restGalleryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(galleryDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkActiveIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        gallery.setActive(null);

        // Create the Gallery, which fails.
        GalleryDTO galleryDTO = galleryMapper.toDto(gallery);

        restGalleryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(galleryDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
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
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)))
            .andExpect(jsonPath("$.[*].wpId").value(hasItem(DEFAULT_WP_ID.intValue())));
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
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.code").value(DEFAULT_CODE))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.active").value(DEFAULT_ACTIVE))
            .andExpect(jsonPath("$.wpId").value(DEFAULT_WP_ID.intValue()));
    }

    @Test
    @Transactional
    void getGalleriesByIdFiltering() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        Long id = gallery.getId();

        defaultGalleryFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultGalleryFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultGalleryFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllGalleriesByNameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        // Get all the galleryList where name equals to
        defaultGalleryFiltering("name.equals=" + DEFAULT_NAME, "name.equals=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllGalleriesByNameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        // Get all the galleryList where name in
        defaultGalleryFiltering("name.in=" + DEFAULT_NAME + "," + UPDATED_NAME, "name.in=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllGalleriesByNameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        // Get all the galleryList where name is not null
        defaultGalleryFiltering("name.specified=true", "name.specified=false");
    }

    @Test
    @Transactional
    void getAllGalleriesByNameContainsSomething() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        // Get all the galleryList where name contains
        defaultGalleryFiltering("name.contains=" + DEFAULT_NAME, "name.contains=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllGalleriesByNameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        // Get all the galleryList where name does not contain
        defaultGalleryFiltering("name.doesNotContain=" + UPDATED_NAME, "name.doesNotContain=" + DEFAULT_NAME);
    }

    @Test
    @Transactional
    void getAllGalleriesByCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        // Get all the galleryList where code equals to
        defaultGalleryFiltering("code.equals=" + DEFAULT_CODE, "code.equals=" + UPDATED_CODE);
    }

    @Test
    @Transactional
    void getAllGalleriesByCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        // Get all the galleryList where code in
        defaultGalleryFiltering("code.in=" + DEFAULT_CODE + "," + UPDATED_CODE, "code.in=" + UPDATED_CODE);
    }

    @Test
    @Transactional
    void getAllGalleriesByCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        // Get all the galleryList where code is not null
        defaultGalleryFiltering("code.specified=true", "code.specified=false");
    }

    @Test
    @Transactional
    void getAllGalleriesByCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        // Get all the galleryList where code contains
        defaultGalleryFiltering("code.contains=" + DEFAULT_CODE, "code.contains=" + UPDATED_CODE);
    }

    @Test
    @Transactional
    void getAllGalleriesByCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        // Get all the galleryList where code does not contain
        defaultGalleryFiltering("code.doesNotContain=" + UPDATED_CODE, "code.doesNotContain=" + DEFAULT_CODE);
    }

    @Test
    @Transactional
    void getAllGalleriesByActiveIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        // Get all the galleryList where active equals to
        defaultGalleryFiltering("active.equals=" + DEFAULT_ACTIVE, "active.equals=" + UPDATED_ACTIVE);
    }

    @Test
    @Transactional
    void getAllGalleriesByActiveIsInShouldWork() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        // Get all the galleryList where active in
        defaultGalleryFiltering("active.in=" + DEFAULT_ACTIVE + "," + UPDATED_ACTIVE, "active.in=" + UPDATED_ACTIVE);
    }

    @Test
    @Transactional
    void getAllGalleriesByActiveIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        // Get all the galleryList where active is not null
        defaultGalleryFiltering("active.specified=true", "active.specified=false");
    }

    @Test
    @Transactional
    void getAllGalleriesByWpIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        // Get all the galleryList where wpId equals to
        defaultGalleryFiltering("wpId.equals=" + DEFAULT_WP_ID, "wpId.equals=" + UPDATED_WP_ID);
    }

    @Test
    @Transactional
    void getAllGalleriesByWpIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        // Get all the galleryList where wpId in
        defaultGalleryFiltering("wpId.in=" + DEFAULT_WP_ID + "," + UPDATED_WP_ID, "wpId.in=" + UPDATED_WP_ID);
    }

    @Test
    @Transactional
    void getAllGalleriesByWpIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        // Get all the galleryList where wpId is not null
        defaultGalleryFiltering("wpId.specified=true", "wpId.specified=false");
    }

    @Test
    @Transactional
    void getAllGalleriesByWpIdIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        // Get all the galleryList where wpId is greater than or equal to
        defaultGalleryFiltering("wpId.greaterThanOrEqual=" + DEFAULT_WP_ID, "wpId.greaterThanOrEqual=" + UPDATED_WP_ID);
    }

    @Test
    @Transactional
    void getAllGalleriesByWpIdIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        // Get all the galleryList where wpId is less than or equal to
        defaultGalleryFiltering("wpId.lessThanOrEqual=" + DEFAULT_WP_ID, "wpId.lessThanOrEqual=" + SMALLER_WP_ID);
    }

    @Test
    @Transactional
    void getAllGalleriesByWpIdIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        // Get all the galleryList where wpId is less than
        defaultGalleryFiltering("wpId.lessThan=" + UPDATED_WP_ID, "wpId.lessThan=" + DEFAULT_WP_ID);
    }

    @Test
    @Transactional
    void getAllGalleriesByWpIdIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        // Get all the galleryList where wpId is greater than
        defaultGalleryFiltering("wpId.greaterThan=" + SMALLER_WP_ID, "wpId.greaterThan=" + DEFAULT_WP_ID);
    }

    private void defaultGalleryFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultGalleryShouldBeFound(shouldBeFound);
        defaultGalleryShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultGalleryShouldBeFound(String filter) throws Exception {
        restGalleryMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(gallery.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].code").value(hasItem(DEFAULT_CODE)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].active").value(hasItem(DEFAULT_ACTIVE)))
            .andExpect(jsonPath("$.[*].wpId").value(hasItem(DEFAULT_WP_ID.intValue())));

        // Check, that the count call also returns 1
        restGalleryMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultGalleryShouldNotBeFound(String filter) throws Exception {
        restGalleryMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restGalleryMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
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

        // Update the gallery
        Gallery updatedGallery = galleryRepository.findById(gallery.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedGallery are not directly saved in db
        em.detach(updatedGallery);
        updatedGallery.name(UPDATED_NAME).code(UPDATED_CODE).description(UPDATED_DESCRIPTION).active(UPDATED_ACTIVE).wpId(UPDATED_WP_ID);
        GalleryDTO galleryDTO = galleryMapper.toDto(updatedGallery);

        restGalleryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, galleryDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(galleryDTO))
            )
            .andExpect(status().isOk());

        // Validate the Gallery in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedGalleryToMatchAllProperties(updatedGallery);
    }

    @Test
    @Transactional
    void putNonExistingGallery() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
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
    }

    @Test
    @Transactional
    void putWithIdMismatchGallery() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
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
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamGallery() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        gallery.setId(longCount.incrementAndGet());

        // Create the Gallery
        GalleryDTO galleryDTO = galleryMapper.toDto(gallery);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGalleryMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(galleryDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Gallery in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
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
            .name(UPDATED_NAME)
            .code(UPDATED_CODE)
            .description(UPDATED_DESCRIPTION)
            .active(UPDATED_ACTIVE)
            .wpId(UPDATED_WP_ID);

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
    }

    @Test
    @Transactional
    void patchWithIdMismatchGallery() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
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
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamGallery() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        gallery.setId(longCount.incrementAndGet());

        // Create the Gallery
        GalleryDTO galleryDTO = galleryMapper.toDto(gallery);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restGalleryMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(galleryDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Gallery in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteGallery() throws Exception {
        // Initialize the database
        insertedGallery = galleryRepository.saveAndFlush(gallery);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the gallery
        restGalleryMockMvc
            .perform(delete(ENTITY_API_URL_ID, gallery.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
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
