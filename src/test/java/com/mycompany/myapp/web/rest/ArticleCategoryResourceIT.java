package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.ArticleCategoryAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.ArticleCategory;
import com.mycompany.myapp.repository.ArticleCategoryRepository;
import com.mycompany.myapp.repository.search.ArticleCategorySearchRepository;
import com.mycompany.myapp.service.ArticleCategoryService;
import com.mycompany.myapp.service.dto.ArticleCategoryDTO;
import com.mycompany.myapp.service.mapper.ArticleCategoryMapper;
import jakarta.persistence.EntityManager;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Streamable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link ArticleCategoryResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ArticleCategoryResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_SLUG = "AAAAAAAAAA";
    private static final String UPDATED_SLUG = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final Integer DEFAULT_COUNT = 0;
    private static final Integer UPDATED_COUNT = 1;

    private static final String ENTITY_API_URL = "/api/article-categories";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/article-categories/_search";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ArticleCategoryRepository articleCategoryRepository;

    @Mock
    private ArticleCategoryRepository articleCategoryRepositoryMock;

    @Autowired
    private ArticleCategoryMapper articleCategoryMapper;

    @Mock
    private ArticleCategoryService articleCategoryServiceMock;

    @Autowired
    private ArticleCategorySearchRepository articleCategorySearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restArticleCategoryMockMvc;

    private ArticleCategory articleCategory;

    private ArticleCategory insertedArticleCategory;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ArticleCategory createEntity() {
        return new ArticleCategory().name(DEFAULT_NAME).slug(DEFAULT_SLUG).description(DEFAULT_DESCRIPTION).count(DEFAULT_COUNT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ArticleCategory createUpdatedEntity() {
        return new ArticleCategory().name(UPDATED_NAME).slug(UPDATED_SLUG).description(UPDATED_DESCRIPTION).count(UPDATED_COUNT);
    }

    @BeforeEach
    void initTest() {
        articleCategory = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedArticleCategory != null) {
            articleCategoryRepository.delete(insertedArticleCategory);
            articleCategorySearchRepository.delete(insertedArticleCategory);
            insertedArticleCategory = null;
        }
    }

    @Test
    @Transactional
    void createArticleCategory() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleCategorySearchRepository.findAll());
        // Create the ArticleCategory
        ArticleCategoryDTO articleCategoryDTO = articleCategoryMapper.toDto(articleCategory);
        var returnedArticleCategoryDTO = om.readValue(
            restArticleCategoryMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(articleCategoryDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ArticleCategoryDTO.class
        );

        // Validate the ArticleCategory in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedArticleCategory = articleCategoryMapper.toEntity(returnedArticleCategoryDTO);
        assertArticleCategoryUpdatableFieldsEquals(returnedArticleCategory, getPersistedArticleCategory(returnedArticleCategory));

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleCategorySearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore + 1);
            });

        insertedArticleCategory = returnedArticleCategory;
    }

    @Test
    @Transactional
    void createArticleCategoryWithExistingId() throws Exception {
        // Create the ArticleCategory with an existing ID
        articleCategory.setId(1L);
        ArticleCategoryDTO articleCategoryDTO = articleCategoryMapper.toDto(articleCategory);

        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleCategorySearchRepository.findAll());

        // An entity with an existing ID cannot be created, so this API call must fail
        restArticleCategoryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(articleCategoryDTO)))
            .andExpect(status().isBadRequest());

        // Validate the ArticleCategory in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleCategorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleCategorySearchRepository.findAll());
        // set the field null
        articleCategory.setName(null);

        // Create the ArticleCategory, which fails.
        ArticleCategoryDTO articleCategoryDTO = articleCategoryMapper.toDto(articleCategory);

        restArticleCategoryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(articleCategoryDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleCategorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkSlugIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleCategorySearchRepository.findAll());
        // set the field null
        articleCategory.setSlug(null);

        // Create the ArticleCategory, which fails.
        ArticleCategoryDTO articleCategoryDTO = articleCategoryMapper.toDto(articleCategory);

        restArticleCategoryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(articleCategoryDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleCategorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void getAllArticleCategories() throws Exception {
        // Initialize the database
        insertedArticleCategory = articleCategoryRepository.saveAndFlush(articleCategory);

        // Get all the articleCategoryList
        restArticleCategoryMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(articleCategory.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].count").value(hasItem(DEFAULT_COUNT)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllArticleCategoriesWithEagerRelationshipsIsEnabled() throws Exception {
        when(articleCategoryServiceMock.findAllWithEagerRelationships()).thenReturn(new ArrayList<>());

        restArticleCategoryMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(articleCategoryServiceMock, times(1)).findAllWithEagerRelationships();
    }

    @SuppressWarnings({ "unchecked" })
    void getAllArticleCategoriesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(articleCategoryServiceMock.findAllWithEagerRelationships()).thenReturn(new ArrayList<>());

        restArticleCategoryMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(articleCategoryRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getArticleCategory() throws Exception {
        // Initialize the database
        insertedArticleCategory = articleCategoryRepository.saveAndFlush(articleCategory);

        // Get the articleCategory
        restArticleCategoryMockMvc
            .perform(get(ENTITY_API_URL_ID, articleCategory.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(articleCategory.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.slug").value(DEFAULT_SLUG))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.count").value(DEFAULT_COUNT));
    }

    @Test
    @Transactional
    void getNonExistingArticleCategory() throws Exception {
        // Get the articleCategory
        restArticleCategoryMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingArticleCategory() throws Exception {
        // Initialize the database
        insertedArticleCategory = articleCategoryRepository.saveAndFlush(articleCategory);

        long databaseSizeBeforeUpdate = getRepositoryCount();
        articleCategorySearchRepository.save(articleCategory);
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleCategorySearchRepository.findAll());

        // Update the articleCategory
        ArticleCategory updatedArticleCategory = articleCategoryRepository.findById(articleCategory.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedArticleCategory are not directly saved in db
        em.detach(updatedArticleCategory);
        updatedArticleCategory.name(UPDATED_NAME).slug(UPDATED_SLUG).description(UPDATED_DESCRIPTION).count(UPDATED_COUNT);
        ArticleCategoryDTO articleCategoryDTO = articleCategoryMapper.toDto(updatedArticleCategory);

        restArticleCategoryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, articleCategoryDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(articleCategoryDTO))
            )
            .andExpect(status().isOk());

        // Validate the ArticleCategory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedArticleCategoryToMatchAllProperties(updatedArticleCategory);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleCategorySearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
                List<ArticleCategory> articleCategorySearchList = Streamable.of(articleCategorySearchRepository.findAll()).toList();
                ArticleCategory testArticleCategorySearch = articleCategorySearchList.get(searchDatabaseSizeAfter - 1);

                assertArticleCategoryAllPropertiesEquals(testArticleCategorySearch, updatedArticleCategory);
            });
    }

    @Test
    @Transactional
    void putNonExistingArticleCategory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleCategorySearchRepository.findAll());
        articleCategory.setId(longCount.incrementAndGet());

        // Create the ArticleCategory
        ArticleCategoryDTO articleCategoryDTO = articleCategoryMapper.toDto(articleCategory);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restArticleCategoryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, articleCategoryDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(articleCategoryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ArticleCategory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleCategorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithIdMismatchArticleCategory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleCategorySearchRepository.findAll());
        articleCategory.setId(longCount.incrementAndGet());

        // Create the ArticleCategory
        ArticleCategoryDTO articleCategoryDTO = articleCategoryMapper.toDto(articleCategory);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restArticleCategoryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(articleCategoryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ArticleCategory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleCategorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamArticleCategory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleCategorySearchRepository.findAll());
        articleCategory.setId(longCount.incrementAndGet());

        // Create the ArticleCategory
        ArticleCategoryDTO articleCategoryDTO = articleCategoryMapper.toDto(articleCategory);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restArticleCategoryMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(articleCategoryDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ArticleCategory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleCategorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void partialUpdateArticleCategoryWithPatch() throws Exception {
        // Initialize the database
        insertedArticleCategory = articleCategoryRepository.saveAndFlush(articleCategory);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the articleCategory using partial update
        ArticleCategory partialUpdatedArticleCategory = new ArticleCategory();
        partialUpdatedArticleCategory.setId(articleCategory.getId());

        partialUpdatedArticleCategory.name(UPDATED_NAME).description(UPDATED_DESCRIPTION).count(UPDATED_COUNT);

        restArticleCategoryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedArticleCategory.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedArticleCategory))
            )
            .andExpect(status().isOk());

        // Validate the ArticleCategory in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertArticleCategoryUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedArticleCategory, articleCategory),
            getPersistedArticleCategory(articleCategory)
        );
    }

    @Test
    @Transactional
    void fullUpdateArticleCategoryWithPatch() throws Exception {
        // Initialize the database
        insertedArticleCategory = articleCategoryRepository.saveAndFlush(articleCategory);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the articleCategory using partial update
        ArticleCategory partialUpdatedArticleCategory = new ArticleCategory();
        partialUpdatedArticleCategory.setId(articleCategory.getId());

        partialUpdatedArticleCategory.name(UPDATED_NAME).slug(UPDATED_SLUG).description(UPDATED_DESCRIPTION).count(UPDATED_COUNT);

        restArticleCategoryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedArticleCategory.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedArticleCategory))
            )
            .andExpect(status().isOk());

        // Validate the ArticleCategory in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertArticleCategoryUpdatableFieldsEquals(
            partialUpdatedArticleCategory,
            getPersistedArticleCategory(partialUpdatedArticleCategory)
        );
    }

    @Test
    @Transactional
    void patchNonExistingArticleCategory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleCategorySearchRepository.findAll());
        articleCategory.setId(longCount.incrementAndGet());

        // Create the ArticleCategory
        ArticleCategoryDTO articleCategoryDTO = articleCategoryMapper.toDto(articleCategory);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restArticleCategoryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, articleCategoryDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(articleCategoryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ArticleCategory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleCategorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithIdMismatchArticleCategory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleCategorySearchRepository.findAll());
        articleCategory.setId(longCount.incrementAndGet());

        // Create the ArticleCategory
        ArticleCategoryDTO articleCategoryDTO = articleCategoryMapper.toDto(articleCategory);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restArticleCategoryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(articleCategoryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ArticleCategory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleCategorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamArticleCategory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleCategorySearchRepository.findAll());
        articleCategory.setId(longCount.incrementAndGet());

        // Create the ArticleCategory
        ArticleCategoryDTO articleCategoryDTO = articleCategoryMapper.toDto(articleCategory);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restArticleCategoryMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(articleCategoryDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ArticleCategory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleCategorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void deleteArticleCategory() throws Exception {
        // Initialize the database
        insertedArticleCategory = articleCategoryRepository.saveAndFlush(articleCategory);
        articleCategoryRepository.save(articleCategory);
        articleCategorySearchRepository.save(articleCategory);

        long databaseSizeBeforeDelete = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleCategorySearchRepository.findAll());
        assertThat(searchDatabaseSizeBefore).isEqualTo(databaseSizeBeforeDelete);

        // Delete the articleCategory
        restArticleCategoryMockMvc
            .perform(delete(ENTITY_API_URL_ID, articleCategory.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleCategorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore - 1);
    }

    @Test
    @Transactional
    void searchArticleCategory() throws Exception {
        // Initialize the database
        insertedArticleCategory = articleCategoryRepository.saveAndFlush(articleCategory);
        articleCategorySearchRepository.save(articleCategory);

        // Search the articleCategory
        restArticleCategoryMockMvc
            .perform(get(ENTITY_SEARCH_API_URL + "?query=id:" + articleCategory.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(articleCategory.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION.toString())))
            .andExpect(jsonPath("$.[*].count").value(hasItem(DEFAULT_COUNT)));
    }

    protected long getRepositoryCount() {
        return articleCategoryRepository.count();
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

    protected ArticleCategory getPersistedArticleCategory(ArticleCategory articleCategory) {
        return articleCategoryRepository.findById(articleCategory.getId()).orElseThrow();
    }

    protected void assertPersistedArticleCategoryToMatchAllProperties(ArticleCategory expectedArticleCategory) {
        assertArticleCategoryAllPropertiesEquals(expectedArticleCategory, getPersistedArticleCategory(expectedArticleCategory));
    }

    protected void assertPersistedArticleCategoryToMatchUpdatableProperties(ArticleCategory expectedArticleCategory) {
        assertArticleCategoryAllUpdatablePropertiesEquals(expectedArticleCategory, getPersistedArticleCategory(expectedArticleCategory));
    }
}
