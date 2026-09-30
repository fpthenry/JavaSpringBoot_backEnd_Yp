package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.RedirectRuleAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.RedirectRule;
import com.mycompany.myapp.repository.RedirectRuleRepository;
import com.mycompany.myapp.repository.search.RedirectRuleSearchRepository;
import com.mycompany.myapp.service.dto.RedirectRuleDTO;
import com.mycompany.myapp.service.mapper.RedirectRuleMapper;
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
 * Integration tests for the {@link RedirectRuleResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class RedirectRuleResourceIT {

    private static final Long DEFAULT_SOURCE_ID = 1L;
    private static final Long UPDATED_SOURCE_ID = 2L;
    private static final Long SMALLER_SOURCE_ID = 1L - 1L;

    private static final String DEFAULT_SOURCE_SLUG = "AAAAAAAAAA";
    private static final String UPDATED_SOURCE_SLUG = "BBBBBBBBBB";

    private static final Long DEFAULT_DESTINATION_ID = 1L;
    private static final Long UPDATED_DESTINATION_ID = 2L;
    private static final Long SMALLER_DESTINATION_ID = 1L - 1L;

    private static final String DEFAULT_DESTINATION_SLUG = "AAAAAAAAAA";
    private static final String UPDATED_DESTINATION_SLUG = "BBBBBBBBBB";

    private static final String DEFAULT_OBJECT_TYPE = "AAAAAAAAAA";
    private static final String UPDATED_OBJECT_TYPE = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/redirect-rules";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/redirect-rules/_search";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RedirectRuleRepository redirectRuleRepository;

    @Autowired
    private RedirectRuleMapper redirectRuleMapper;

    @Autowired
    private RedirectRuleSearchRepository redirectRuleSearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restRedirectRuleMockMvc;

    private RedirectRule redirectRule;

    private RedirectRule insertedRedirectRule;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RedirectRule createEntity() {
        return new RedirectRule()
            .sourceId(DEFAULT_SOURCE_ID)
            .sourceSlug(DEFAULT_SOURCE_SLUG)
            .destinationId(DEFAULT_DESTINATION_ID)
            .destinationSlug(DEFAULT_DESTINATION_SLUG)
            .objectType(DEFAULT_OBJECT_TYPE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RedirectRule createUpdatedEntity() {
        return new RedirectRule()
            .sourceId(UPDATED_SOURCE_ID)
            .sourceSlug(UPDATED_SOURCE_SLUG)
            .destinationId(UPDATED_DESTINATION_ID)
            .destinationSlug(UPDATED_DESTINATION_SLUG)
            .objectType(UPDATED_OBJECT_TYPE);
    }

    @BeforeEach
    void initTest() {
        redirectRule = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedRedirectRule != null) {
            redirectRuleRepository.delete(insertedRedirectRule);
            redirectRuleSearchRepository.delete(insertedRedirectRule);
            insertedRedirectRule = null;
        }
    }

    @Test
    @Transactional
    void createRedirectRule() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());
        // Create the RedirectRule
        RedirectRuleDTO redirectRuleDTO = redirectRuleMapper.toDto(redirectRule);
        var returnedRedirectRuleDTO = om.readValue(
            restRedirectRuleMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(redirectRuleDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            RedirectRuleDTO.class
        );

        // Validate the RedirectRule in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedRedirectRule = redirectRuleMapper.toEntity(returnedRedirectRuleDTO);
        assertRedirectRuleUpdatableFieldsEquals(returnedRedirectRule, getPersistedRedirectRule(returnedRedirectRule));

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore + 1);
            });

        insertedRedirectRule = returnedRedirectRule;
    }

    @Test
    @Transactional
    void createRedirectRuleWithExistingId() throws Exception {
        // Create the RedirectRule with an existing ID
        redirectRule.setId(1L);
        RedirectRuleDTO redirectRuleDTO = redirectRuleMapper.toDto(redirectRule);

        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());

        // An entity with an existing ID cannot be created, so this API call must fail
        restRedirectRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(redirectRuleDTO)))
            .andExpect(status().isBadRequest());

        // Validate the RedirectRule in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkSourceSlugIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());
        // set the field null
        redirectRule.setSourceSlug(null);

        // Create the RedirectRule, which fails.
        RedirectRuleDTO redirectRuleDTO = redirectRuleMapper.toDto(redirectRule);

        restRedirectRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(redirectRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkDestinationSlugIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());
        // set the field null
        redirectRule.setDestinationSlug(null);

        // Create the RedirectRule, which fails.
        RedirectRuleDTO redirectRuleDTO = redirectRuleMapper.toDto(redirectRule);

        restRedirectRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(redirectRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkObjectTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());
        // set the field null
        redirectRule.setObjectType(null);

        // Create the RedirectRule, which fails.
        RedirectRuleDTO redirectRuleDTO = redirectRuleMapper.toDto(redirectRule);

        restRedirectRuleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(redirectRuleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void getAllRedirectRules() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList
        restRedirectRuleMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(redirectRule.getId().intValue())))
            .andExpect(jsonPath("$.[*].sourceId").value(hasItem(DEFAULT_SOURCE_ID.intValue())))
            .andExpect(jsonPath("$.[*].sourceSlug").value(hasItem(DEFAULT_SOURCE_SLUG)))
            .andExpect(jsonPath("$.[*].destinationId").value(hasItem(DEFAULT_DESTINATION_ID.intValue())))
            .andExpect(jsonPath("$.[*].destinationSlug").value(hasItem(DEFAULT_DESTINATION_SLUG)))
            .andExpect(jsonPath("$.[*].objectType").value(hasItem(DEFAULT_OBJECT_TYPE)));
    }

    @Test
    @Transactional
    void getRedirectRule() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get the redirectRule
        restRedirectRuleMockMvc
            .perform(get(ENTITY_API_URL_ID, redirectRule.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(redirectRule.getId().intValue()))
            .andExpect(jsonPath("$.sourceId").value(DEFAULT_SOURCE_ID.intValue()))
            .andExpect(jsonPath("$.sourceSlug").value(DEFAULT_SOURCE_SLUG))
            .andExpect(jsonPath("$.destinationId").value(DEFAULT_DESTINATION_ID.intValue()))
            .andExpect(jsonPath("$.destinationSlug").value(DEFAULT_DESTINATION_SLUG))
            .andExpect(jsonPath("$.objectType").value(DEFAULT_OBJECT_TYPE));
    }

    @Test
    @Transactional
    void getRedirectRulesByIdFiltering() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        Long id = redirectRule.getId();

        defaultRedirectRuleFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultRedirectRuleFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultRedirectRuleFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllRedirectRulesBySourceIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where sourceId equals to
        defaultRedirectRuleFiltering("sourceId.equals=" + DEFAULT_SOURCE_ID, "sourceId.equals=" + UPDATED_SOURCE_ID);
    }

    @Test
    @Transactional
    void getAllRedirectRulesBySourceIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where sourceId in
        defaultRedirectRuleFiltering("sourceId.in=" + DEFAULT_SOURCE_ID + "," + UPDATED_SOURCE_ID, "sourceId.in=" + UPDATED_SOURCE_ID);
    }

    @Test
    @Transactional
    void getAllRedirectRulesBySourceIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where sourceId is not null
        defaultRedirectRuleFiltering("sourceId.specified=true", "sourceId.specified=false");
    }

    @Test
    @Transactional
    void getAllRedirectRulesBySourceIdIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where sourceId is greater than or equal to
        defaultRedirectRuleFiltering(
            "sourceId.greaterThanOrEqual=" + DEFAULT_SOURCE_ID,
            "sourceId.greaterThanOrEqual=" + UPDATED_SOURCE_ID
        );
    }

    @Test
    @Transactional
    void getAllRedirectRulesBySourceIdIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where sourceId is less than or equal to
        defaultRedirectRuleFiltering("sourceId.lessThanOrEqual=" + DEFAULT_SOURCE_ID, "sourceId.lessThanOrEqual=" + SMALLER_SOURCE_ID);
    }

    @Test
    @Transactional
    void getAllRedirectRulesBySourceIdIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where sourceId is less than
        defaultRedirectRuleFiltering("sourceId.lessThan=" + UPDATED_SOURCE_ID, "sourceId.lessThan=" + DEFAULT_SOURCE_ID);
    }

    @Test
    @Transactional
    void getAllRedirectRulesBySourceIdIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where sourceId is greater than
        defaultRedirectRuleFiltering("sourceId.greaterThan=" + SMALLER_SOURCE_ID, "sourceId.greaterThan=" + DEFAULT_SOURCE_ID);
    }

    @Test
    @Transactional
    void getAllRedirectRulesBySourceSlugIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where sourceSlug equals to
        defaultRedirectRuleFiltering("sourceSlug.equals=" + DEFAULT_SOURCE_SLUG, "sourceSlug.equals=" + UPDATED_SOURCE_SLUG);
    }

    @Test
    @Transactional
    void getAllRedirectRulesBySourceSlugIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where sourceSlug in
        defaultRedirectRuleFiltering(
            "sourceSlug.in=" + DEFAULT_SOURCE_SLUG + "," + UPDATED_SOURCE_SLUG,
            "sourceSlug.in=" + UPDATED_SOURCE_SLUG
        );
    }

    @Test
    @Transactional
    void getAllRedirectRulesBySourceSlugIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where sourceSlug is not null
        defaultRedirectRuleFiltering("sourceSlug.specified=true", "sourceSlug.specified=false");
    }

    @Test
    @Transactional
    void getAllRedirectRulesBySourceSlugContainsSomething() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where sourceSlug contains
        defaultRedirectRuleFiltering("sourceSlug.contains=" + DEFAULT_SOURCE_SLUG, "sourceSlug.contains=" + UPDATED_SOURCE_SLUG);
    }

    @Test
    @Transactional
    void getAllRedirectRulesBySourceSlugNotContainsSomething() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where sourceSlug does not contain
        defaultRedirectRuleFiltering(
            "sourceSlug.doesNotContain=" + UPDATED_SOURCE_SLUG,
            "sourceSlug.doesNotContain=" + DEFAULT_SOURCE_SLUG
        );
    }

    @Test
    @Transactional
    void getAllRedirectRulesByDestinationIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where destinationId equals to
        defaultRedirectRuleFiltering("destinationId.equals=" + DEFAULT_DESTINATION_ID, "destinationId.equals=" + UPDATED_DESTINATION_ID);
    }

    @Test
    @Transactional
    void getAllRedirectRulesByDestinationIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where destinationId in
        defaultRedirectRuleFiltering(
            "destinationId.in=" + DEFAULT_DESTINATION_ID + "," + UPDATED_DESTINATION_ID,
            "destinationId.in=" + UPDATED_DESTINATION_ID
        );
    }

    @Test
    @Transactional
    void getAllRedirectRulesByDestinationIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where destinationId is not null
        defaultRedirectRuleFiltering("destinationId.specified=true", "destinationId.specified=false");
    }

    @Test
    @Transactional
    void getAllRedirectRulesByDestinationIdIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where destinationId is greater than or equal to
        defaultRedirectRuleFiltering(
            "destinationId.greaterThanOrEqual=" + DEFAULT_DESTINATION_ID,
            "destinationId.greaterThanOrEqual=" + UPDATED_DESTINATION_ID
        );
    }

    @Test
    @Transactional
    void getAllRedirectRulesByDestinationIdIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where destinationId is less than or equal to
        defaultRedirectRuleFiltering(
            "destinationId.lessThanOrEqual=" + DEFAULT_DESTINATION_ID,
            "destinationId.lessThanOrEqual=" + SMALLER_DESTINATION_ID
        );
    }

    @Test
    @Transactional
    void getAllRedirectRulesByDestinationIdIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where destinationId is less than
        defaultRedirectRuleFiltering(
            "destinationId.lessThan=" + UPDATED_DESTINATION_ID,
            "destinationId.lessThan=" + DEFAULT_DESTINATION_ID
        );
    }

    @Test
    @Transactional
    void getAllRedirectRulesByDestinationIdIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where destinationId is greater than
        defaultRedirectRuleFiltering(
            "destinationId.greaterThan=" + SMALLER_DESTINATION_ID,
            "destinationId.greaterThan=" + DEFAULT_DESTINATION_ID
        );
    }

    @Test
    @Transactional
    void getAllRedirectRulesByDestinationSlugIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where destinationSlug equals to
        defaultRedirectRuleFiltering(
            "destinationSlug.equals=" + DEFAULT_DESTINATION_SLUG,
            "destinationSlug.equals=" + UPDATED_DESTINATION_SLUG
        );
    }

    @Test
    @Transactional
    void getAllRedirectRulesByDestinationSlugIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where destinationSlug in
        defaultRedirectRuleFiltering(
            "destinationSlug.in=" + DEFAULT_DESTINATION_SLUG + "," + UPDATED_DESTINATION_SLUG,
            "destinationSlug.in=" + UPDATED_DESTINATION_SLUG
        );
    }

    @Test
    @Transactional
    void getAllRedirectRulesByDestinationSlugIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where destinationSlug is not null
        defaultRedirectRuleFiltering("destinationSlug.specified=true", "destinationSlug.specified=false");
    }

    @Test
    @Transactional
    void getAllRedirectRulesByDestinationSlugContainsSomething() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where destinationSlug contains
        defaultRedirectRuleFiltering(
            "destinationSlug.contains=" + DEFAULT_DESTINATION_SLUG,
            "destinationSlug.contains=" + UPDATED_DESTINATION_SLUG
        );
    }

    @Test
    @Transactional
    void getAllRedirectRulesByDestinationSlugNotContainsSomething() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where destinationSlug does not contain
        defaultRedirectRuleFiltering(
            "destinationSlug.doesNotContain=" + UPDATED_DESTINATION_SLUG,
            "destinationSlug.doesNotContain=" + DEFAULT_DESTINATION_SLUG
        );
    }

    @Test
    @Transactional
    void getAllRedirectRulesByObjectTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where objectType equals to
        defaultRedirectRuleFiltering("objectType.equals=" + DEFAULT_OBJECT_TYPE, "objectType.equals=" + UPDATED_OBJECT_TYPE);
    }

    @Test
    @Transactional
    void getAllRedirectRulesByObjectTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where objectType in
        defaultRedirectRuleFiltering(
            "objectType.in=" + DEFAULT_OBJECT_TYPE + "," + UPDATED_OBJECT_TYPE,
            "objectType.in=" + UPDATED_OBJECT_TYPE
        );
    }

    @Test
    @Transactional
    void getAllRedirectRulesByObjectTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where objectType is not null
        defaultRedirectRuleFiltering("objectType.specified=true", "objectType.specified=false");
    }

    @Test
    @Transactional
    void getAllRedirectRulesByObjectTypeContainsSomething() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where objectType contains
        defaultRedirectRuleFiltering("objectType.contains=" + DEFAULT_OBJECT_TYPE, "objectType.contains=" + UPDATED_OBJECT_TYPE);
    }

    @Test
    @Transactional
    void getAllRedirectRulesByObjectTypeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        // Get all the redirectRuleList where objectType does not contain
        defaultRedirectRuleFiltering(
            "objectType.doesNotContain=" + UPDATED_OBJECT_TYPE,
            "objectType.doesNotContain=" + DEFAULT_OBJECT_TYPE
        );
    }

    private void defaultRedirectRuleFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultRedirectRuleShouldBeFound(shouldBeFound);
        defaultRedirectRuleShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultRedirectRuleShouldBeFound(String filter) throws Exception {
        restRedirectRuleMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(redirectRule.getId().intValue())))
            .andExpect(jsonPath("$.[*].sourceId").value(hasItem(DEFAULT_SOURCE_ID.intValue())))
            .andExpect(jsonPath("$.[*].sourceSlug").value(hasItem(DEFAULT_SOURCE_SLUG)))
            .andExpect(jsonPath("$.[*].destinationId").value(hasItem(DEFAULT_DESTINATION_ID.intValue())))
            .andExpect(jsonPath("$.[*].destinationSlug").value(hasItem(DEFAULT_DESTINATION_SLUG)))
            .andExpect(jsonPath("$.[*].objectType").value(hasItem(DEFAULT_OBJECT_TYPE)));

        // Check, that the count call also returns 1
        restRedirectRuleMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultRedirectRuleShouldNotBeFound(String filter) throws Exception {
        restRedirectRuleMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restRedirectRuleMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingRedirectRule() throws Exception {
        // Get the redirectRule
        restRedirectRuleMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingRedirectRule() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        long databaseSizeBeforeUpdate = getRepositoryCount();
        redirectRuleSearchRepository.save(redirectRule);
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());

        // Update the redirectRule
        RedirectRule updatedRedirectRule = redirectRuleRepository.findById(redirectRule.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedRedirectRule are not directly saved in db
        em.detach(updatedRedirectRule);
        updatedRedirectRule
            .sourceId(UPDATED_SOURCE_ID)
            .sourceSlug(UPDATED_SOURCE_SLUG)
            .destinationId(UPDATED_DESTINATION_ID)
            .destinationSlug(UPDATED_DESTINATION_SLUG)
            .objectType(UPDATED_OBJECT_TYPE);
        RedirectRuleDTO redirectRuleDTO = redirectRuleMapper.toDto(updatedRedirectRule);

        restRedirectRuleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, redirectRuleDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(redirectRuleDTO))
            )
            .andExpect(status().isOk());

        // Validate the RedirectRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedRedirectRuleToMatchAllProperties(updatedRedirectRule);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
                List<RedirectRule> redirectRuleSearchList = Streamable.of(redirectRuleSearchRepository.findAll()).toList();
                RedirectRule testRedirectRuleSearch = redirectRuleSearchList.get(searchDatabaseSizeAfter - 1);

                assertRedirectRuleAllPropertiesEquals(testRedirectRuleSearch, updatedRedirectRule);
            });
    }

    @Test
    @Transactional
    void putNonExistingRedirectRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());
        redirectRule.setId(longCount.incrementAndGet());

        // Create the RedirectRule
        RedirectRuleDTO redirectRuleDTO = redirectRuleMapper.toDto(redirectRule);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRedirectRuleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, redirectRuleDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(redirectRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RedirectRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithIdMismatchRedirectRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());
        redirectRule.setId(longCount.incrementAndGet());

        // Create the RedirectRule
        RedirectRuleDTO redirectRuleDTO = redirectRuleMapper.toDto(redirectRule);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRedirectRuleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(redirectRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RedirectRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamRedirectRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());
        redirectRule.setId(longCount.incrementAndGet());

        // Create the RedirectRule
        RedirectRuleDTO redirectRuleDTO = redirectRuleMapper.toDto(redirectRule);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRedirectRuleMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(redirectRuleDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the RedirectRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void partialUpdateRedirectRuleWithPatch() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the redirectRule using partial update
        RedirectRule partialUpdatedRedirectRule = new RedirectRule();
        partialUpdatedRedirectRule.setId(redirectRule.getId());

        partialUpdatedRedirectRule.destinationId(UPDATED_DESTINATION_ID);

        restRedirectRuleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRedirectRule.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRedirectRule))
            )
            .andExpect(status().isOk());

        // Validate the RedirectRule in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRedirectRuleUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedRedirectRule, redirectRule),
            getPersistedRedirectRule(redirectRule)
        );
    }

    @Test
    @Transactional
    void fullUpdateRedirectRuleWithPatch() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the redirectRule using partial update
        RedirectRule partialUpdatedRedirectRule = new RedirectRule();
        partialUpdatedRedirectRule.setId(redirectRule.getId());

        partialUpdatedRedirectRule
            .sourceId(UPDATED_SOURCE_ID)
            .sourceSlug(UPDATED_SOURCE_SLUG)
            .destinationId(UPDATED_DESTINATION_ID)
            .destinationSlug(UPDATED_DESTINATION_SLUG)
            .objectType(UPDATED_OBJECT_TYPE);

        restRedirectRuleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRedirectRule.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRedirectRule))
            )
            .andExpect(status().isOk());

        // Validate the RedirectRule in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRedirectRuleUpdatableFieldsEquals(partialUpdatedRedirectRule, getPersistedRedirectRule(partialUpdatedRedirectRule));
    }

    @Test
    @Transactional
    void patchNonExistingRedirectRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());
        redirectRule.setId(longCount.incrementAndGet());

        // Create the RedirectRule
        RedirectRuleDTO redirectRuleDTO = redirectRuleMapper.toDto(redirectRule);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRedirectRuleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, redirectRuleDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(redirectRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RedirectRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithIdMismatchRedirectRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());
        redirectRule.setId(longCount.incrementAndGet());

        // Create the RedirectRule
        RedirectRuleDTO redirectRuleDTO = redirectRuleMapper.toDto(redirectRule);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRedirectRuleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(redirectRuleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RedirectRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamRedirectRule() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());
        redirectRule.setId(longCount.incrementAndGet());

        // Create the RedirectRule
        RedirectRuleDTO redirectRuleDTO = redirectRuleMapper.toDto(redirectRule);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRedirectRuleMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(redirectRuleDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the RedirectRule in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void deleteRedirectRule() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);
        redirectRuleRepository.save(redirectRule);
        redirectRuleSearchRepository.save(redirectRule);

        long databaseSizeBeforeDelete = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());
        assertThat(searchDatabaseSizeBefore).isEqualTo(databaseSizeBeforeDelete);

        // Delete the redirectRule
        restRedirectRuleMockMvc
            .perform(delete(ENTITY_API_URL_ID, redirectRule.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(redirectRuleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore - 1);
    }

    @Test
    @Transactional
    void searchRedirectRule() throws Exception {
        // Initialize the database
        insertedRedirectRule = redirectRuleRepository.saveAndFlush(redirectRule);
        redirectRuleSearchRepository.save(redirectRule);

        // Search the redirectRule
        restRedirectRuleMockMvc
            .perform(get(ENTITY_SEARCH_API_URL + "?query=id:" + redirectRule.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(redirectRule.getId().intValue())))
            .andExpect(jsonPath("$.[*].sourceId").value(hasItem(DEFAULT_SOURCE_ID.intValue())))
            .andExpect(jsonPath("$.[*].sourceSlug").value(hasItem(DEFAULT_SOURCE_SLUG)))
            .andExpect(jsonPath("$.[*].destinationId").value(hasItem(DEFAULT_DESTINATION_ID.intValue())))
            .andExpect(jsonPath("$.[*].destinationSlug").value(hasItem(DEFAULT_DESTINATION_SLUG)))
            .andExpect(jsonPath("$.[*].objectType").value(hasItem(DEFAULT_OBJECT_TYPE)));
    }

    protected long getRepositoryCount() {
        return redirectRuleRepository.count();
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

    protected RedirectRule getPersistedRedirectRule(RedirectRule redirectRule) {
        return redirectRuleRepository.findById(redirectRule.getId()).orElseThrow();
    }

    protected void assertPersistedRedirectRuleToMatchAllProperties(RedirectRule expectedRedirectRule) {
        assertRedirectRuleAllPropertiesEquals(expectedRedirectRule, getPersistedRedirectRule(expectedRedirectRule));
    }

    protected void assertPersistedRedirectRuleToMatchUpdatableProperties(RedirectRule expectedRedirectRule) {
        assertRedirectRuleAllUpdatablePropertiesEquals(expectedRedirectRule, getPersistedRedirectRule(expectedRedirectRule));
    }
}
