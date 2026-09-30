package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.StaticPageAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.StaticPage;
import com.mycompany.myapp.repository.StaticPageRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.repository.search.StaticPageSearchRepository;
import com.mycompany.myapp.service.StaticPageService;
import com.mycompany.myapp.service.dto.StaticPageDTO;
import com.mycompany.myapp.service.mapper.StaticPageMapper;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Streamable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link StaticPageResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class StaticPageResourceIT {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_SLUG = "AAAAAAAAAA";
    private static final String UPDATED_SLUG = "BBBBBBBBBB";

    private static final String DEFAULT_CONTENT = "AAAAAAAAAA";
    private static final String UPDATED_CONTENT = "BBBBBBBBBB";

    private static final String DEFAULT_STATUS = "AAAAAAAAAA";
    private static final String UPDATED_STATUS = "BBBBBBBBBB";

    private static final String DEFAULT_TEMPLATE = "AAAAAAAAAA";
    private static final String UPDATED_TEMPLATE = "BBBBBBBBBB";

    private static final Integer DEFAULT_MENU_ORDER = 1;
    private static final Integer UPDATED_MENU_ORDER = 2;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1790760789812L);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.ofEpochMilli(1790760789812L);

    private static final String ENTITY_API_URL = "/api/static-pages";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/static-pages/_search";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private StaticPageRepository staticPageRepository;

    @Autowired
    private UserRepository userRepository;

    @Mock
    private StaticPageRepository staticPageRepositoryMock;

    @Autowired
    private StaticPageMapper staticPageMapper;

    @Mock
    private StaticPageService staticPageServiceMock;

    @Autowired
    private StaticPageSearchRepository staticPageSearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restStaticPageMockMvc;

    private StaticPage staticPage;

    private StaticPage insertedStaticPage;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static StaticPage createEntity() {
        return new StaticPage()
            .title(DEFAULT_TITLE)
            .slug(DEFAULT_SLUG)
            .content(DEFAULT_CONTENT)
            .status(DEFAULT_STATUS)
            .template(DEFAULT_TEMPLATE)
            .menuOrder(DEFAULT_MENU_ORDER)
            .createdAt(DEFAULT_CREATED_AT)
            .updatedAt(DEFAULT_UPDATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static StaticPage createUpdatedEntity() {
        return new StaticPage()
            .title(UPDATED_TITLE)
            .slug(UPDATED_SLUG)
            .content(UPDATED_CONTENT)
            .status(UPDATED_STATUS)
            .template(UPDATED_TEMPLATE)
            .menuOrder(UPDATED_MENU_ORDER)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
    }

    @BeforeEach
    void initTest() {
        staticPage = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedStaticPage != null) {
            staticPageRepository.delete(insertedStaticPage);
            staticPageSearchRepository.delete(insertedStaticPage);
            insertedStaticPage = null;
        }
    }

    @Test
    @Transactional
    void createStaticPage() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(staticPageSearchRepository.findAll());
        // Create the StaticPage
        StaticPageDTO staticPageDTO = staticPageMapper.toDto(staticPage);
        var returnedStaticPageDTO = om.readValue(
            restStaticPageMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(staticPageDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            StaticPageDTO.class
        );

        // Validate the StaticPage in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedStaticPage = staticPageMapper.toEntity(returnedStaticPageDTO);
        assertStaticPageUpdatableFieldsEquals(returnedStaticPage, getPersistedStaticPage(returnedStaticPage));

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(staticPageSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore + 1);
            });

        insertedStaticPage = returnedStaticPage;
    }

    @Test
    @Transactional
    void createStaticPageWithExistingId() throws Exception {
        // Create the StaticPage with an existing ID
        staticPage.setId(1L);
        StaticPageDTO staticPageDTO = staticPageMapper.toDto(staticPage);

        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(staticPageSearchRepository.findAll());

        // An entity with an existing ID cannot be created, so this API call must fail
        restStaticPageMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(staticPageDTO)))
            .andExpect(status().isBadRequest());

        // Validate the StaticPage in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(staticPageSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkTitleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(staticPageSearchRepository.findAll());
        // set the field null
        staticPage.setTitle(null);

        // Create the StaticPage, which fails.
        StaticPageDTO staticPageDTO = staticPageMapper.toDto(staticPage);

        restStaticPageMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(staticPageDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(staticPageSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkSlugIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(staticPageSearchRepository.findAll());
        // set the field null
        staticPage.setSlug(null);

        // Create the StaticPage, which fails.
        StaticPageDTO staticPageDTO = staticPageMapper.toDto(staticPage);

        restStaticPageMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(staticPageDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(staticPageSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void getAllStaticPages() throws Exception {
        // Initialize the database
        insertedStaticPage = staticPageRepository.saveAndFlush(staticPage);

        // Get all the staticPageList
        restStaticPageMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(staticPage.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].content").value(hasItem(DEFAULT_CONTENT)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS)))
            .andExpect(jsonPath("$.[*].template").value(hasItem(DEFAULT_TEMPLATE)))
            .andExpect(jsonPath("$.[*].menuOrder").value(hasItem(DEFAULT_MENU_ORDER)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllStaticPagesWithEagerRelationshipsIsEnabled() throws Exception {
        when(staticPageServiceMock.findAllWithEagerRelationships()).thenReturn(new ArrayList<>());

        restStaticPageMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(staticPageServiceMock, times(1)).findAllWithEagerRelationships();
    }

    @SuppressWarnings({ "unchecked" })
    void getAllStaticPagesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(staticPageServiceMock.findAllWithEagerRelationships()).thenReturn(new ArrayList<>());

        restStaticPageMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(staticPageRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getStaticPage() throws Exception {
        // Initialize the database
        insertedStaticPage = staticPageRepository.saveAndFlush(staticPage);

        // Get the staticPage
        restStaticPageMockMvc
            .perform(get(ENTITY_API_URL_ID, staticPage.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(staticPage.getId().intValue()))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
            .andExpect(jsonPath("$.slug").value(DEFAULT_SLUG))
            .andExpect(jsonPath("$.content").value(DEFAULT_CONTENT))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS))
            .andExpect(jsonPath("$.template").value(DEFAULT_TEMPLATE))
            .andExpect(jsonPath("$.menuOrder").value(DEFAULT_MENU_ORDER))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingStaticPage() throws Exception {
        // Get the staticPage
        restStaticPageMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingStaticPage() throws Exception {
        // Initialize the database
        insertedStaticPage = staticPageRepository.saveAndFlush(staticPage);

        long databaseSizeBeforeUpdate = getRepositoryCount();
        staticPageSearchRepository.save(staticPage);
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(staticPageSearchRepository.findAll());

        // Update the staticPage
        StaticPage updatedStaticPage = staticPageRepository.findById(staticPage.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedStaticPage are not directly saved in db
        em.detach(updatedStaticPage);
        updatedStaticPage
            .title(UPDATED_TITLE)
            .slug(UPDATED_SLUG)
            .content(UPDATED_CONTENT)
            .status(UPDATED_STATUS)
            .template(UPDATED_TEMPLATE)
            .menuOrder(UPDATED_MENU_ORDER)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
        StaticPageDTO staticPageDTO = staticPageMapper.toDto(updatedStaticPage);

        restStaticPageMockMvc
            .perform(
                put(ENTITY_API_URL_ID, staticPageDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(staticPageDTO))
            )
            .andExpect(status().isOk());

        // Validate the StaticPage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedStaticPageToMatchAllProperties(updatedStaticPage);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(staticPageSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
                List<StaticPage> staticPageSearchList = Streamable.of(staticPageSearchRepository.findAll()).toList();
                StaticPage testStaticPageSearch = staticPageSearchList.get(searchDatabaseSizeAfter - 1);

                assertStaticPageAllPropertiesEquals(testStaticPageSearch, updatedStaticPage);
            });
    }

    @Test
    @Transactional
    void putNonExistingStaticPage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(staticPageSearchRepository.findAll());
        staticPage.setId(longCount.incrementAndGet());

        // Create the StaticPage
        StaticPageDTO staticPageDTO = staticPageMapper.toDto(staticPage);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restStaticPageMockMvc
            .perform(
                put(ENTITY_API_URL_ID, staticPageDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(staticPageDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the StaticPage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(staticPageSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithIdMismatchStaticPage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(staticPageSearchRepository.findAll());
        staticPage.setId(longCount.incrementAndGet());

        // Create the StaticPage
        StaticPageDTO staticPageDTO = staticPageMapper.toDto(staticPage);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restStaticPageMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(staticPageDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the StaticPage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(staticPageSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamStaticPage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(staticPageSearchRepository.findAll());
        staticPage.setId(longCount.incrementAndGet());

        // Create the StaticPage
        StaticPageDTO staticPageDTO = staticPageMapper.toDto(staticPage);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restStaticPageMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(staticPageDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the StaticPage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(staticPageSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void partialUpdateStaticPageWithPatch() throws Exception {
        // Initialize the database
        insertedStaticPage = staticPageRepository.saveAndFlush(staticPage);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the staticPage using partial update
        StaticPage partialUpdatedStaticPage = new StaticPage();
        partialUpdatedStaticPage.setId(staticPage.getId());

        partialUpdatedStaticPage
            .slug(UPDATED_SLUG)
            .content(UPDATED_CONTENT)
            .template(UPDATED_TEMPLATE)
            .menuOrder(UPDATED_MENU_ORDER)
            .createdAt(UPDATED_CREATED_AT);

        restStaticPageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedStaticPage.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedStaticPage))
            )
            .andExpect(status().isOk());

        // Validate the StaticPage in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertStaticPageUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedStaticPage, staticPage),
            getPersistedStaticPage(staticPage)
        );
    }

    @Test
    @Transactional
    void fullUpdateStaticPageWithPatch() throws Exception {
        // Initialize the database
        insertedStaticPage = staticPageRepository.saveAndFlush(staticPage);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the staticPage using partial update
        StaticPage partialUpdatedStaticPage = new StaticPage();
        partialUpdatedStaticPage.setId(staticPage.getId());

        partialUpdatedStaticPage
            .title(UPDATED_TITLE)
            .slug(UPDATED_SLUG)
            .content(UPDATED_CONTENT)
            .status(UPDATED_STATUS)
            .template(UPDATED_TEMPLATE)
            .menuOrder(UPDATED_MENU_ORDER)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);

        restStaticPageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedStaticPage.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedStaticPage))
            )
            .andExpect(status().isOk());

        // Validate the StaticPage in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertStaticPageUpdatableFieldsEquals(partialUpdatedStaticPage, getPersistedStaticPage(partialUpdatedStaticPage));
    }

    @Test
    @Transactional
    void patchNonExistingStaticPage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(staticPageSearchRepository.findAll());
        staticPage.setId(longCount.incrementAndGet());

        // Create the StaticPage
        StaticPageDTO staticPageDTO = staticPageMapper.toDto(staticPage);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restStaticPageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, staticPageDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(staticPageDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the StaticPage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(staticPageSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithIdMismatchStaticPage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(staticPageSearchRepository.findAll());
        staticPage.setId(longCount.incrementAndGet());

        // Create the StaticPage
        StaticPageDTO staticPageDTO = staticPageMapper.toDto(staticPage);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restStaticPageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(staticPageDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the StaticPage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(staticPageSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamStaticPage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(staticPageSearchRepository.findAll());
        staticPage.setId(longCount.incrementAndGet());

        // Create the StaticPage
        StaticPageDTO staticPageDTO = staticPageMapper.toDto(staticPage);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restStaticPageMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(staticPageDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the StaticPage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(staticPageSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void deleteStaticPage() throws Exception {
        // Initialize the database
        insertedStaticPage = staticPageRepository.saveAndFlush(staticPage);
        staticPageRepository.save(staticPage);
        staticPageSearchRepository.save(staticPage);

        long databaseSizeBeforeDelete = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(staticPageSearchRepository.findAll());
        assertThat(searchDatabaseSizeBefore).isEqualTo(databaseSizeBeforeDelete);

        // Delete the staticPage
        restStaticPageMockMvc
            .perform(delete(ENTITY_API_URL_ID, staticPage.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(staticPageSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore - 1);
    }

    @Test
    @Transactional
    void searchStaticPage() throws Exception {
        // Initialize the database
        insertedStaticPage = staticPageRepository.saveAndFlush(staticPage);
        staticPageSearchRepository.save(staticPage);

        // Search the staticPage
        restStaticPageMockMvc
            .perform(get(ENTITY_SEARCH_API_URL + "?query=id:" + staticPage.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(staticPage.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].content").value(hasItem(DEFAULT_CONTENT.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS)))
            .andExpect(jsonPath("$.[*].template").value(hasItem(DEFAULT_TEMPLATE)))
            .andExpect(jsonPath("$.[*].menuOrder").value(hasItem(DEFAULT_MENU_ORDER)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    protected long getRepositoryCount() {
        return staticPageRepository.count();
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

    protected StaticPage getPersistedStaticPage(StaticPage staticPage) {
        return staticPageRepository.findById(staticPage.getId()).orElseThrow();
    }

    protected void assertPersistedStaticPageToMatchAllProperties(StaticPage expectedStaticPage) {
        assertStaticPageAllPropertiesEquals(expectedStaticPage, getPersistedStaticPage(expectedStaticPage));
    }

    protected void assertPersistedStaticPageToMatchUpdatableProperties(StaticPage expectedStaticPage) {
        assertStaticPageAllUpdatablePropertiesEquals(expectedStaticPage, getPersistedStaticPage(expectedStaticPage));
    }
}
