package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.ArticleTagAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.ArticleTag;
import com.mycompany.myapp.repository.ArticleTagRepository;
import com.mycompany.myapp.repository.search.ArticleTagSearchRepository;
import com.mycompany.myapp.service.dto.ArticleTagDTO;
import com.mycompany.myapp.service.mapper.ArticleTagMapper;
import jakarta.persistence.EntityManager;
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
 * Integration tests for the {@link ArticleTagResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ArticleTagResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_SLUG = "AAAAAAAAAA";
    private static final String UPDATED_SLUG = "BBBBBBBBBB";

    private static final Integer DEFAULT_COUNT = 0;
    private static final Integer UPDATED_COUNT = 1;

    private static final String ENTITY_API_URL = "/api/article-tags";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/article-tags/_search";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ArticleTagRepository articleTagRepository;

    @Autowired
    private ArticleTagMapper articleTagMapper;

    @Autowired
    private ArticleTagSearchRepository articleTagSearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restArticleTagMockMvc;

    private ArticleTag articleTag;

    private ArticleTag insertedArticleTag;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ArticleTag createEntity() {
        return new ArticleTag().name(DEFAULT_NAME).slug(DEFAULT_SLUG).count(DEFAULT_COUNT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ArticleTag createUpdatedEntity() {
        return new ArticleTag().name(UPDATED_NAME).slug(UPDATED_SLUG).count(UPDATED_COUNT);
    }

    @BeforeEach
    void initTest() {
        articleTag = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedArticleTag != null) {
            articleTagRepository.delete(insertedArticleTag);
            articleTagSearchRepository.delete(insertedArticleTag);
            insertedArticleTag = null;
        }
    }

    @Test
    @Transactional
    void createArticleTag() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleTagSearchRepository.findAll());
        // Create the ArticleTag
        ArticleTagDTO articleTagDTO = articleTagMapper.toDto(articleTag);
        var returnedArticleTagDTO = om.readValue(
            restArticleTagMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(articleTagDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ArticleTagDTO.class
        );

        // Validate the ArticleTag in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedArticleTag = articleTagMapper.toEntity(returnedArticleTagDTO);
        assertArticleTagUpdatableFieldsEquals(returnedArticleTag, getPersistedArticleTag(returnedArticleTag));

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleTagSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore + 1);
            });

        insertedArticleTag = returnedArticleTag;
    }

    @Test
    @Transactional
    void createArticleTagWithExistingId() throws Exception {
        // Create the ArticleTag with an existing ID
        articleTag.setId(1L);
        ArticleTagDTO articleTagDTO = articleTagMapper.toDto(articleTag);

        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleTagSearchRepository.findAll());

        // An entity with an existing ID cannot be created, so this API call must fail
        restArticleTagMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(articleTagDTO)))
            .andExpect(status().isBadRequest());

        // Validate the ArticleTag in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleTagSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleTagSearchRepository.findAll());
        // set the field null
        articleTag.setName(null);

        // Create the ArticleTag, which fails.
        ArticleTagDTO articleTagDTO = articleTagMapper.toDto(articleTag);

        restArticleTagMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(articleTagDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleTagSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkSlugIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleTagSearchRepository.findAll());
        // set the field null
        articleTag.setSlug(null);

        // Create the ArticleTag, which fails.
        ArticleTagDTO articleTagDTO = articleTagMapper.toDto(articleTag);

        restArticleTagMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(articleTagDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleTagSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void getAllArticleTags() throws Exception {
        // Initialize the database
        insertedArticleTag = articleTagRepository.saveAndFlush(articleTag);

        // Get all the articleTagList
        restArticleTagMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(articleTag.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].count").value(hasItem(DEFAULT_COUNT)));
    }

    @Test
    @Transactional
    void getArticleTag() throws Exception {
        // Initialize the database
        insertedArticleTag = articleTagRepository.saveAndFlush(articleTag);

        // Get the articleTag
        restArticleTagMockMvc
            .perform(get(ENTITY_API_URL_ID, articleTag.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(articleTag.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.slug").value(DEFAULT_SLUG))
            .andExpect(jsonPath("$.count").value(DEFAULT_COUNT));
    }

    @Test
    @Transactional
    void getNonExistingArticleTag() throws Exception {
        // Get the articleTag
        restArticleTagMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingArticleTag() throws Exception {
        // Initialize the database
        insertedArticleTag = articleTagRepository.saveAndFlush(articleTag);

        long databaseSizeBeforeUpdate = getRepositoryCount();
        articleTagSearchRepository.save(articleTag);
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleTagSearchRepository.findAll());

        // Update the articleTag
        ArticleTag updatedArticleTag = articleTagRepository.findById(articleTag.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedArticleTag are not directly saved in db
        em.detach(updatedArticleTag);
        updatedArticleTag.name(UPDATED_NAME).slug(UPDATED_SLUG).count(UPDATED_COUNT);
        ArticleTagDTO articleTagDTO = articleTagMapper.toDto(updatedArticleTag);

        restArticleTagMockMvc
            .perform(
                put(ENTITY_API_URL_ID, articleTagDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(articleTagDTO))
            )
            .andExpect(status().isOk());

        // Validate the ArticleTag in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedArticleTagToMatchAllProperties(updatedArticleTag);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleTagSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
                List<ArticleTag> articleTagSearchList = Streamable.of(articleTagSearchRepository.findAll()).toList();
                ArticleTag testArticleTagSearch = articleTagSearchList.get(searchDatabaseSizeAfter - 1);

                assertArticleTagAllPropertiesEquals(testArticleTagSearch, updatedArticleTag);
            });
    }

    @Test
    @Transactional
    void putNonExistingArticleTag() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleTagSearchRepository.findAll());
        articleTag.setId(longCount.incrementAndGet());

        // Create the ArticleTag
        ArticleTagDTO articleTagDTO = articleTagMapper.toDto(articleTag);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restArticleTagMockMvc
            .perform(
                put(ENTITY_API_URL_ID, articleTagDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(articleTagDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ArticleTag in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleTagSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithIdMismatchArticleTag() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleTagSearchRepository.findAll());
        articleTag.setId(longCount.incrementAndGet());

        // Create the ArticleTag
        ArticleTagDTO articleTagDTO = articleTagMapper.toDto(articleTag);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restArticleTagMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(articleTagDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ArticleTag in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleTagSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamArticleTag() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleTagSearchRepository.findAll());
        articleTag.setId(longCount.incrementAndGet());

        // Create the ArticleTag
        ArticleTagDTO articleTagDTO = articleTagMapper.toDto(articleTag);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restArticleTagMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(articleTagDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ArticleTag in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleTagSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void partialUpdateArticleTagWithPatch() throws Exception {
        // Initialize the database
        insertedArticleTag = articleTagRepository.saveAndFlush(articleTag);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the articleTag using partial update
        ArticleTag partialUpdatedArticleTag = new ArticleTag();
        partialUpdatedArticleTag.setId(articleTag.getId());

        partialUpdatedArticleTag.name(UPDATED_NAME);

        restArticleTagMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedArticleTag.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedArticleTag))
            )
            .andExpect(status().isOk());

        // Validate the ArticleTag in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertArticleTagUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedArticleTag, articleTag),
            getPersistedArticleTag(articleTag)
        );
    }

    @Test
    @Transactional
    void fullUpdateArticleTagWithPatch() throws Exception {
        // Initialize the database
        insertedArticleTag = articleTagRepository.saveAndFlush(articleTag);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the articleTag using partial update
        ArticleTag partialUpdatedArticleTag = new ArticleTag();
        partialUpdatedArticleTag.setId(articleTag.getId());

        partialUpdatedArticleTag.name(UPDATED_NAME).slug(UPDATED_SLUG).count(UPDATED_COUNT);

        restArticleTagMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedArticleTag.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedArticleTag))
            )
            .andExpect(status().isOk());

        // Validate the ArticleTag in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertArticleTagUpdatableFieldsEquals(partialUpdatedArticleTag, getPersistedArticleTag(partialUpdatedArticleTag));
    }

    @Test
    @Transactional
    void patchNonExistingArticleTag() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleTagSearchRepository.findAll());
        articleTag.setId(longCount.incrementAndGet());

        // Create the ArticleTag
        ArticleTagDTO articleTagDTO = articleTagMapper.toDto(articleTag);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restArticleTagMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, articleTagDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(articleTagDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ArticleTag in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleTagSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithIdMismatchArticleTag() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleTagSearchRepository.findAll());
        articleTag.setId(longCount.incrementAndGet());

        // Create the ArticleTag
        ArticleTagDTO articleTagDTO = articleTagMapper.toDto(articleTag);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restArticleTagMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(articleTagDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ArticleTag in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleTagSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamArticleTag() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleTagSearchRepository.findAll());
        articleTag.setId(longCount.incrementAndGet());

        // Create the ArticleTag
        ArticleTagDTO articleTagDTO = articleTagMapper.toDto(articleTag);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restArticleTagMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(articleTagDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ArticleTag in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleTagSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void deleteArticleTag() throws Exception {
        // Initialize the database
        insertedArticleTag = articleTagRepository.saveAndFlush(articleTag);
        articleTagRepository.save(articleTag);
        articleTagSearchRepository.save(articleTag);

        long databaseSizeBeforeDelete = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleTagSearchRepository.findAll());
        assertThat(searchDatabaseSizeBefore).isEqualTo(databaseSizeBeforeDelete);

        // Delete the articleTag
        restArticleTagMockMvc
            .perform(delete(ENTITY_API_URL_ID, articleTag.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleTagSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore - 1);
    }

    @Test
    @Transactional
    void searchArticleTag() throws Exception {
        // Initialize the database
        insertedArticleTag = articleTagRepository.saveAndFlush(articleTag);
        articleTagSearchRepository.save(articleTag);

        // Search the articleTag
        restArticleTagMockMvc
            .perform(get(ENTITY_SEARCH_API_URL + "?query=id:" + articleTag.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(articleTag.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].count").value(hasItem(DEFAULT_COUNT)));
    }

    protected long getRepositoryCount() {
        return articleTagRepository.count();
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

    protected ArticleTag getPersistedArticleTag(ArticleTag articleTag) {
        return articleTagRepository.findById(articleTag.getId()).orElseThrow();
    }

    protected void assertPersistedArticleTagToMatchAllProperties(ArticleTag expectedArticleTag) {
        assertArticleTagAllPropertiesEquals(expectedArticleTag, getPersistedArticleTag(expectedArticleTag));
    }

    protected void assertPersistedArticleTagToMatchUpdatableProperties(ArticleTag expectedArticleTag) {
        assertArticleTagAllUpdatablePropertiesEquals(expectedArticleTag, getPersistedArticleTag(expectedArticleTag));
    }
}
