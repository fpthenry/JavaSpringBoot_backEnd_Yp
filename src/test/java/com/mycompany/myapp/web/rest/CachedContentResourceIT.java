package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.CachedContentAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.CachedContent;
import com.mycompany.myapp.repository.CachedContentRepository;
import com.mycompany.myapp.repository.search.CachedContentSearchRepository;
import com.mycompany.myapp.service.dto.CachedContentDTO;
import com.mycompany.myapp.service.mapper.CachedContentMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.assertj.core.util.IterableUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.util.Streamable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link CachedContentResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class CachedContentResourceIT {

    private static final String DEFAULT_TERM_SLUG = "AAAAAAAAAA";
    private static final String UPDATED_TERM_SLUG = "BBBBBBBBBB";

    private static final Instant DEFAULT_EXPIRED = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_EXPIRED = Instant.ofEpochMilli(1790760789812L);

    private static final String DEFAULT_CONTENT = "AAAAAAAAAA";
    private static final String UPDATED_CONTENT = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/cached-contents";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/cached-contents/_search";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CachedContentRepository cachedContentRepository;

    @Autowired
    private CachedContentMapper cachedContentMapper;

    @Autowired
    private CachedContentSearchRepository cachedContentSearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCachedContentMockMvc;

    private CachedContent cachedContent;

    private CachedContent insertedCachedContent;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CachedContent createEntity() {
        return new CachedContent().termSlug(DEFAULT_TERM_SLUG).expired(DEFAULT_EXPIRED).content(DEFAULT_CONTENT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CachedContent createUpdatedEntity() {
        return new CachedContent().termSlug(UPDATED_TERM_SLUG).expired(UPDATED_EXPIRED).content(UPDATED_CONTENT);
    }

    @BeforeEach
    void initTest() {
        cachedContent = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedCachedContent != null) {
            cachedContentRepository.delete(insertedCachedContent);
            cachedContentSearchRepository.delete(insertedCachedContent);
            insertedCachedContent = null;
        }
    }

    @Test
    @Transactional
    void createCachedContent() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(cachedContentSearchRepository.findAll());
        // Create the CachedContent
        CachedContentDTO cachedContentDTO = cachedContentMapper.toDto(cachedContent);
        var returnedCachedContentDTO = om.readValue(
            restCachedContentMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(cachedContentDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            CachedContentDTO.class
        );

        // Validate the CachedContent in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedCachedContent = cachedContentMapper.toEntity(returnedCachedContentDTO);
        assertCachedContentUpdatableFieldsEquals(returnedCachedContent, getPersistedCachedContent(returnedCachedContent));

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(cachedContentSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore + 1);
            });

        insertedCachedContent = returnedCachedContent;
    }

    @Test
    @Transactional
    void createCachedContentWithExistingId() throws Exception {
        // Create the CachedContent with an existing ID
        cachedContent.setId(1L);
        CachedContentDTO cachedContentDTO = cachedContentMapper.toDto(cachedContent);

        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(cachedContentSearchRepository.findAll());

        // An entity with an existing ID cannot be created, so this API call must fail
        restCachedContentMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(cachedContentDTO)))
            .andExpect(status().isBadRequest());

        // Validate the CachedContent in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(cachedContentSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkTermSlugIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(cachedContentSearchRepository.findAll());
        // set the field null
        cachedContent.setTermSlug(null);

        // Create the CachedContent, which fails.
        CachedContentDTO cachedContentDTO = cachedContentMapper.toDto(cachedContent);

        restCachedContentMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(cachedContentDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(cachedContentSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void getAllCachedContents() throws Exception {
        // Initialize the database
        insertedCachedContent = cachedContentRepository.saveAndFlush(cachedContent);

        // Get all the cachedContentList
        restCachedContentMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(cachedContent.getId().intValue())))
            .andExpect(jsonPath("$.[*].termSlug").value(hasItem(DEFAULT_TERM_SLUG)))
            .andExpect(jsonPath("$.[*].expired").value(hasItem(DEFAULT_EXPIRED.toString())))
            .andExpect(jsonPath("$.[*].content").value(hasItem(DEFAULT_CONTENT)));
    }

    @Test
    @Transactional
    void getCachedContent() throws Exception {
        // Initialize the database
        insertedCachedContent = cachedContentRepository.saveAndFlush(cachedContent);

        // Get the cachedContent
        restCachedContentMockMvc
            .perform(get(ENTITY_API_URL_ID, cachedContent.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(cachedContent.getId().intValue()))
            .andExpect(jsonPath("$.termSlug").value(DEFAULT_TERM_SLUG))
            .andExpect(jsonPath("$.expired").value(DEFAULT_EXPIRED.toString()))
            .andExpect(jsonPath("$.content").value(DEFAULT_CONTENT));
    }

    @Test
    @Transactional
    void getNonExistingCachedContent() throws Exception {
        // Get the cachedContent
        restCachedContentMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingCachedContent() throws Exception {
        // Initialize the database
        insertedCachedContent = cachedContentRepository.saveAndFlush(cachedContent);

        long databaseSizeBeforeUpdate = getRepositoryCount();
        cachedContentSearchRepository.save(cachedContent);
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(cachedContentSearchRepository.findAll());

        // Update the cachedContent
        CachedContent updatedCachedContent = cachedContentRepository.findById(cachedContent.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedCachedContent are not directly saved in db
        em.detach(updatedCachedContent);
        updatedCachedContent.termSlug(UPDATED_TERM_SLUG).expired(UPDATED_EXPIRED).content(UPDATED_CONTENT);
        CachedContentDTO cachedContentDTO = cachedContentMapper.toDto(updatedCachedContent);

        restCachedContentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, cachedContentDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(cachedContentDTO))
            )
            .andExpect(status().isOk());

        // Validate the CachedContent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedCachedContentToMatchAllProperties(updatedCachedContent);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(cachedContentSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
                List<CachedContent> cachedContentSearchList = Streamable.of(cachedContentSearchRepository.findAll()).toList();
                CachedContent testCachedContentSearch = cachedContentSearchList.get(searchDatabaseSizeAfter - 1);

                assertCachedContentAllPropertiesEquals(testCachedContentSearch, updatedCachedContent);
            });
    }

    @Test
    @Transactional
    void putNonExistingCachedContent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(cachedContentSearchRepository.findAll());
        cachedContent.setId(longCount.incrementAndGet());

        // Create the CachedContent
        CachedContentDTO cachedContentDTO = cachedContentMapper.toDto(cachedContent);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCachedContentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, cachedContentDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(cachedContentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CachedContent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(cachedContentSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithIdMismatchCachedContent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(cachedContentSearchRepository.findAll());
        cachedContent.setId(longCount.incrementAndGet());

        // Create the CachedContent
        CachedContentDTO cachedContentDTO = cachedContentMapper.toDto(cachedContent);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCachedContentMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(cachedContentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CachedContent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(cachedContentSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamCachedContent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(cachedContentSearchRepository.findAll());
        cachedContent.setId(longCount.incrementAndGet());

        // Create the CachedContent
        CachedContentDTO cachedContentDTO = cachedContentMapper.toDto(cachedContent);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCachedContentMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(cachedContentDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the CachedContent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(cachedContentSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void partialUpdateCachedContentWithPatch() throws Exception {
        // Initialize the database
        insertedCachedContent = cachedContentRepository.saveAndFlush(cachedContent);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the cachedContent using partial update
        CachedContent partialUpdatedCachedContent = new CachedContent();
        partialUpdatedCachedContent.setId(cachedContent.getId());

        partialUpdatedCachedContent.termSlug(UPDATED_TERM_SLUG).expired(UPDATED_EXPIRED);

        restCachedContentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCachedContent.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCachedContent))
            )
            .andExpect(status().isOk());

        // Validate the CachedContent in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCachedContentUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedCachedContent, cachedContent),
            getPersistedCachedContent(cachedContent)
        );
    }

    @Test
    @Transactional
    void fullUpdateCachedContentWithPatch() throws Exception {
        // Initialize the database
        insertedCachedContent = cachedContentRepository.saveAndFlush(cachedContent);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the cachedContent using partial update
        CachedContent partialUpdatedCachedContent = new CachedContent();
        partialUpdatedCachedContent.setId(cachedContent.getId());

        partialUpdatedCachedContent.termSlug(UPDATED_TERM_SLUG).expired(UPDATED_EXPIRED).content(UPDATED_CONTENT);

        restCachedContentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCachedContent.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCachedContent))
            )
            .andExpect(status().isOk());

        // Validate the CachedContent in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCachedContentUpdatableFieldsEquals(partialUpdatedCachedContent, getPersistedCachedContent(partialUpdatedCachedContent));
    }

    @Test
    @Transactional
    void patchNonExistingCachedContent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(cachedContentSearchRepository.findAll());
        cachedContent.setId(longCount.incrementAndGet());

        // Create the CachedContent
        CachedContentDTO cachedContentDTO = cachedContentMapper.toDto(cachedContent);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCachedContentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, cachedContentDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(cachedContentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CachedContent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(cachedContentSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithIdMismatchCachedContent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(cachedContentSearchRepository.findAll());
        cachedContent.setId(longCount.incrementAndGet());

        // Create the CachedContent
        CachedContentDTO cachedContentDTO = cachedContentMapper.toDto(cachedContent);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCachedContentMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(cachedContentDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the CachedContent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(cachedContentSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamCachedContent() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(cachedContentSearchRepository.findAll());
        cachedContent.setId(longCount.incrementAndGet());

        // Create the CachedContent
        CachedContentDTO cachedContentDTO = cachedContentMapper.toDto(cachedContent);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCachedContentMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(cachedContentDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the CachedContent in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(cachedContentSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void deleteCachedContent() throws Exception {
        // Initialize the database
        insertedCachedContent = cachedContentRepository.saveAndFlush(cachedContent);
        cachedContentRepository.save(cachedContent);
        cachedContentSearchRepository.save(cachedContent);

        long databaseSizeBeforeDelete = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(cachedContentSearchRepository.findAll());
        assertThat(searchDatabaseSizeBefore).isEqualTo(databaseSizeBeforeDelete);

        // Delete the cachedContent
        restCachedContentMockMvc
            .perform(delete(ENTITY_API_URL_ID, cachedContent.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(cachedContentSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore - 1);
    }

    @Test
    @Transactional
    void searchCachedContent() throws Exception {
        // Initialize the database
        insertedCachedContent = cachedContentRepository.saveAndFlush(cachedContent);
        cachedContentSearchRepository.save(cachedContent);

        // Search the cachedContent
        restCachedContentMockMvc
            .perform(get(ENTITY_SEARCH_API_URL + "?query=id:" + cachedContent.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(cachedContent.getId().intValue())))
            .andExpect(jsonPath("$.[*].termSlug").value(hasItem(DEFAULT_TERM_SLUG)))
            .andExpect(jsonPath("$.[*].expired").value(hasItem(DEFAULT_EXPIRED.toString())))
            .andExpect(jsonPath("$.[*].content").value(hasItem(DEFAULT_CONTENT.toString())));
    }

    protected long getRepositoryCount() {
        return cachedContentRepository.count();
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

    protected CachedContent getPersistedCachedContent(CachedContent cachedContent) {
        return cachedContentRepository.findById(cachedContent.getId()).orElseThrow();
    }

    protected void assertPersistedCachedContentToMatchAllProperties(CachedContent expectedCachedContent) {
        assertCachedContentAllPropertiesEquals(expectedCachedContent, getPersistedCachedContent(expectedCachedContent));
    }

    protected void assertPersistedCachedContentToMatchUpdatableProperties(CachedContent expectedCachedContent) {
        assertCachedContentAllUpdatablePropertiesEquals(expectedCachedContent, getPersistedCachedContent(expectedCachedContent));
    }
}
