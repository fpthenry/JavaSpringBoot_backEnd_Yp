package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.CategoryAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Category;
import com.mycompany.myapp.domain.Category;
import com.mycompany.myapp.domain.Listing;
import com.mycompany.myapp.repository.CategoryRepository;
import com.mycompany.myapp.repository.search.CategorySearchRepository;
import com.mycompany.myapp.service.CategoryService;
import com.mycompany.myapp.service.dto.CategoryDTO;
import com.mycompany.myapp.service.mapper.CategoryMapper;
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
 * Integration tests for the {@link CategoryResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class CategoryResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_SLUG = "AAAAAAAAAA";
    private static final String UPDATED_SLUG = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final Integer DEFAULT_COUNT = 0;
    private static final Integer UPDATED_COUNT = 1;
    private static final Integer SMALLER_COUNT = 0 - 1;

    private static final String DEFAULT_INDUSTRY_CODE = "AAAAAAAAAA";
    private static final String UPDATED_INDUSTRY_CODE = "BBBBBBBBBB";

    private static final Integer DEFAULT_DISPLAY_ORDER = 0;
    private static final Integer UPDATED_DISPLAY_ORDER = 1;
    private static final Integer SMALLER_DISPLAY_ORDER = 0 - 1;

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1790760789812L);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.ofEpochMilli(1790760789812L);

    private static final String ENTITY_API_URL = "/api/categories";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/categories/_search";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryRepository categoryRepositoryMock;

    @Autowired
    private CategoryMapper categoryMapper;

    @Mock
    private CategoryService categoryServiceMock;

    @Autowired
    private CategorySearchRepository categorySearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restCategoryMockMvc;

    private Category category;

    private Category insertedCategory;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Category createEntity() {
        return new Category()
            .name(DEFAULT_NAME)
            .slug(DEFAULT_SLUG)
            .description(DEFAULT_DESCRIPTION)
            .count(DEFAULT_COUNT)
            .industryCode(DEFAULT_INDUSTRY_CODE)
            .displayOrder(DEFAULT_DISPLAY_ORDER)
            .createdAt(DEFAULT_CREATED_AT)
            .updatedAt(DEFAULT_UPDATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Category createUpdatedEntity() {
        return new Category()
            .name(UPDATED_NAME)
            .slug(UPDATED_SLUG)
            .description(UPDATED_DESCRIPTION)
            .count(UPDATED_COUNT)
            .industryCode(UPDATED_INDUSTRY_CODE)
            .displayOrder(UPDATED_DISPLAY_ORDER)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
    }

    @BeforeEach
    void initTest() {
        category = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedCategory != null) {
            categoryRepository.delete(insertedCategory);
            categorySearchRepository.delete(insertedCategory);
            insertedCategory = null;
        }
    }

    @Test
    @Transactional
    void createCategory() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(categorySearchRepository.findAll());
        // Create the Category
        CategoryDTO categoryDTO = categoryMapper.toDto(category);
        var returnedCategoryDTO = om.readValue(
            restCategoryMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(categoryDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            CategoryDTO.class
        );

        // Validate the Category in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedCategory = categoryMapper.toEntity(returnedCategoryDTO);
        assertCategoryUpdatableFieldsEquals(returnedCategory, getPersistedCategory(returnedCategory));

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(categorySearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore + 1);
            });

        insertedCategory = returnedCategory;
    }

    @Test
    @Transactional
    void createCategoryWithExistingId() throws Exception {
        // Create the Category with an existing ID
        category.setId(1L);
        CategoryDTO categoryDTO = categoryMapper.toDto(category);

        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(categorySearchRepository.findAll());

        // An entity with an existing ID cannot be created, so this API call must fail
        restCategoryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(categoryDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Category in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(categorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(categorySearchRepository.findAll());
        // set the field null
        category.setName(null);

        // Create the Category, which fails.
        CategoryDTO categoryDTO = categoryMapper.toDto(category);

        restCategoryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(categoryDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(categorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkSlugIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(categorySearchRepository.findAll());
        // set the field null
        category.setSlug(null);

        // Create the Category, which fails.
        CategoryDTO categoryDTO = categoryMapper.toDto(category);

        restCategoryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(categoryDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(categorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void getAllCategories() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList
        restCategoryMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(category.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].count").value(hasItem(DEFAULT_COUNT)))
            .andExpect(jsonPath("$.[*].industryCode").value(hasItem(DEFAULT_INDUSTRY_CODE)))
            .andExpect(jsonPath("$.[*].displayOrder").value(hasItem(DEFAULT_DISPLAY_ORDER)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllCategoriesWithEagerRelationshipsIsEnabled() throws Exception {
        when(categoryServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restCategoryMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(categoryServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllCategoriesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(categoryServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restCategoryMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(categoryRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getCategory() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get the category
        restCategoryMockMvc
            .perform(get(ENTITY_API_URL_ID, category.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(category.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.slug").value(DEFAULT_SLUG))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.count").value(DEFAULT_COUNT))
            .andExpect(jsonPath("$.industryCode").value(DEFAULT_INDUSTRY_CODE))
            .andExpect(jsonPath("$.displayOrder").value(DEFAULT_DISPLAY_ORDER))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()));
    }

    @Test
    @Transactional
    void getCategoriesByIdFiltering() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        Long id = category.getId();

        defaultCategoryFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultCategoryFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultCategoryFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllCategoriesByNameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where name equals to
        defaultCategoryFiltering("name.equals=" + DEFAULT_NAME, "name.equals=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllCategoriesByNameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where name in
        defaultCategoryFiltering("name.in=" + DEFAULT_NAME + "," + UPDATED_NAME, "name.in=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllCategoriesByNameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where name is not null
        defaultCategoryFiltering("name.specified=true", "name.specified=false");
    }

    @Test
    @Transactional
    void getAllCategoriesByNameContainsSomething() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where name contains
        defaultCategoryFiltering("name.contains=" + DEFAULT_NAME, "name.contains=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllCategoriesByNameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where name does not contain
        defaultCategoryFiltering("name.doesNotContain=" + UPDATED_NAME, "name.doesNotContain=" + DEFAULT_NAME);
    }

    @Test
    @Transactional
    void getAllCategoriesBySlugIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where slug equals to
        defaultCategoryFiltering("slug.equals=" + DEFAULT_SLUG, "slug.equals=" + UPDATED_SLUG);
    }

    @Test
    @Transactional
    void getAllCategoriesBySlugIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where slug in
        defaultCategoryFiltering("slug.in=" + DEFAULT_SLUG + "," + UPDATED_SLUG, "slug.in=" + UPDATED_SLUG);
    }

    @Test
    @Transactional
    void getAllCategoriesBySlugIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where slug is not null
        defaultCategoryFiltering("slug.specified=true", "slug.specified=false");
    }

    @Test
    @Transactional
    void getAllCategoriesBySlugContainsSomething() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where slug contains
        defaultCategoryFiltering("slug.contains=" + DEFAULT_SLUG, "slug.contains=" + UPDATED_SLUG);
    }

    @Test
    @Transactional
    void getAllCategoriesBySlugNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where slug does not contain
        defaultCategoryFiltering("slug.doesNotContain=" + UPDATED_SLUG, "slug.doesNotContain=" + DEFAULT_SLUG);
    }

    @Test
    @Transactional
    void getAllCategoriesByCountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where count equals to
        defaultCategoryFiltering("count.equals=" + DEFAULT_COUNT, "count.equals=" + UPDATED_COUNT);
    }

    @Test
    @Transactional
    void getAllCategoriesByCountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where count in
        defaultCategoryFiltering("count.in=" + DEFAULT_COUNT + "," + UPDATED_COUNT, "count.in=" + UPDATED_COUNT);
    }

    @Test
    @Transactional
    void getAllCategoriesByCountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where count is not null
        defaultCategoryFiltering("count.specified=true", "count.specified=false");
    }

    @Test
    @Transactional
    void getAllCategoriesByCountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where count is greater than or equal to
        defaultCategoryFiltering("count.greaterThanOrEqual=" + DEFAULT_COUNT, "count.greaterThanOrEqual=" + UPDATED_COUNT);
    }

    @Test
    @Transactional
    void getAllCategoriesByCountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where count is less than or equal to
        defaultCategoryFiltering("count.lessThanOrEqual=" + DEFAULT_COUNT, "count.lessThanOrEqual=" + SMALLER_COUNT);
    }

    @Test
    @Transactional
    void getAllCategoriesByCountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where count is less than
        defaultCategoryFiltering("count.lessThan=" + UPDATED_COUNT, "count.lessThan=" + DEFAULT_COUNT);
    }

    @Test
    @Transactional
    void getAllCategoriesByCountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where count is greater than
        defaultCategoryFiltering("count.greaterThan=" + SMALLER_COUNT, "count.greaterThan=" + DEFAULT_COUNT);
    }

    @Test
    @Transactional
    void getAllCategoriesByIndustryCodeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where industryCode equals to
        defaultCategoryFiltering("industryCode.equals=" + DEFAULT_INDUSTRY_CODE, "industryCode.equals=" + UPDATED_INDUSTRY_CODE);
    }

    @Test
    @Transactional
    void getAllCategoriesByIndustryCodeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where industryCode in
        defaultCategoryFiltering(
            "industryCode.in=" + DEFAULT_INDUSTRY_CODE + "," + UPDATED_INDUSTRY_CODE,
            "industryCode.in=" + UPDATED_INDUSTRY_CODE
        );
    }

    @Test
    @Transactional
    void getAllCategoriesByIndustryCodeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where industryCode is not null
        defaultCategoryFiltering("industryCode.specified=true", "industryCode.specified=false");
    }

    @Test
    @Transactional
    void getAllCategoriesByIndustryCodeContainsSomething() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where industryCode contains
        defaultCategoryFiltering("industryCode.contains=" + DEFAULT_INDUSTRY_CODE, "industryCode.contains=" + UPDATED_INDUSTRY_CODE);
    }

    @Test
    @Transactional
    void getAllCategoriesByIndustryCodeNotContainsSomething() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where industryCode does not contain
        defaultCategoryFiltering(
            "industryCode.doesNotContain=" + UPDATED_INDUSTRY_CODE,
            "industryCode.doesNotContain=" + DEFAULT_INDUSTRY_CODE
        );
    }

    @Test
    @Transactional
    void getAllCategoriesByDisplayOrderIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where displayOrder equals to
        defaultCategoryFiltering("displayOrder.equals=" + DEFAULT_DISPLAY_ORDER, "displayOrder.equals=" + UPDATED_DISPLAY_ORDER);
    }

    @Test
    @Transactional
    void getAllCategoriesByDisplayOrderIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where displayOrder in
        defaultCategoryFiltering(
            "displayOrder.in=" + DEFAULT_DISPLAY_ORDER + "," + UPDATED_DISPLAY_ORDER,
            "displayOrder.in=" + UPDATED_DISPLAY_ORDER
        );
    }

    @Test
    @Transactional
    void getAllCategoriesByDisplayOrderIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where displayOrder is not null
        defaultCategoryFiltering("displayOrder.specified=true", "displayOrder.specified=false");
    }

    @Test
    @Transactional
    void getAllCategoriesByDisplayOrderIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where displayOrder is greater than or equal to
        defaultCategoryFiltering(
            "displayOrder.greaterThanOrEqual=" + DEFAULT_DISPLAY_ORDER,
            "displayOrder.greaterThanOrEqual=" + UPDATED_DISPLAY_ORDER
        );
    }

    @Test
    @Transactional
    void getAllCategoriesByDisplayOrderIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where displayOrder is less than or equal to
        defaultCategoryFiltering(
            "displayOrder.lessThanOrEqual=" + DEFAULT_DISPLAY_ORDER,
            "displayOrder.lessThanOrEqual=" + SMALLER_DISPLAY_ORDER
        );
    }

    @Test
    @Transactional
    void getAllCategoriesByDisplayOrderIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where displayOrder is less than
        defaultCategoryFiltering("displayOrder.lessThan=" + UPDATED_DISPLAY_ORDER, "displayOrder.lessThan=" + DEFAULT_DISPLAY_ORDER);
    }

    @Test
    @Transactional
    void getAllCategoriesByDisplayOrderIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where displayOrder is greater than
        defaultCategoryFiltering("displayOrder.greaterThan=" + SMALLER_DISPLAY_ORDER, "displayOrder.greaterThan=" + DEFAULT_DISPLAY_ORDER);
    }

    @Test
    @Transactional
    void getAllCategoriesByCreatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where createdAt equals to
        defaultCategoryFiltering("createdAt.equals=" + DEFAULT_CREATED_AT, "createdAt.equals=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllCategoriesByCreatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where createdAt in
        defaultCategoryFiltering("createdAt.in=" + DEFAULT_CREATED_AT + "," + UPDATED_CREATED_AT, "createdAt.in=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllCategoriesByCreatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where createdAt is not null
        defaultCategoryFiltering("createdAt.specified=true", "createdAt.specified=false");
    }

    @Test
    @Transactional
    void getAllCategoriesByUpdatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where updatedAt equals to
        defaultCategoryFiltering("updatedAt.equals=" + DEFAULT_UPDATED_AT, "updatedAt.equals=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllCategoriesByUpdatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where updatedAt in
        defaultCategoryFiltering("updatedAt.in=" + DEFAULT_UPDATED_AT + "," + UPDATED_UPDATED_AT, "updatedAt.in=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllCategoriesByUpdatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        // Get all the categoryList where updatedAt is not null
        defaultCategoryFiltering("updatedAt.specified=true", "updatedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllCategoriesByParentIsEqualToSomething() throws Exception {
        Category parent;
        if (TestUtil.findAll(em, Category.class).isEmpty()) {
            categoryRepository.saveAndFlush(category);
            parent = CategoryResourceIT.createEntity();
        } else {
            parent = TestUtil.findAll(em, Category.class).get(0);
        }
        em.persist(parent);
        em.flush();
        category.setParent(parent);
        categoryRepository.saveAndFlush(category);
        Long parentId = parent.getId();
        // Get all the categoryList where parent equals to parentId
        defaultCategoryShouldBeFound("parentId.equals=" + parentId);

        // Get all the categoryList where parent equals to (parentId + 1)
        defaultCategoryShouldNotBeFound("parentId.equals=" + (parentId + 1));
    }

    @Test
    @Transactional
    void getAllCategoriesByListingsIsEqualToSomething() throws Exception {
        Listing listings;
        if (TestUtil.findAll(em, Listing.class).isEmpty()) {
            categoryRepository.saveAndFlush(category);
            listings = ListingResourceIT.createEntity();
        } else {
            listings = TestUtil.findAll(em, Listing.class).get(0);
        }
        em.persist(listings);
        em.flush();
        category.addListings(listings);
        categoryRepository.saveAndFlush(category);
        Long listingsId = listings.getId();
        // Get all the categoryList where listings equals to listingsId
        defaultCategoryShouldBeFound("listingsId.equals=" + listingsId);

        // Get all the categoryList where listings equals to (listingsId + 1)
        defaultCategoryShouldNotBeFound("listingsId.equals=" + (listingsId + 1));
    }

    private void defaultCategoryFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultCategoryShouldBeFound(shouldBeFound);
        defaultCategoryShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultCategoryShouldBeFound(String filter) throws Exception {
        restCategoryMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(category.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].count").value(hasItem(DEFAULT_COUNT)))
            .andExpect(jsonPath("$.[*].industryCode").value(hasItem(DEFAULT_INDUSTRY_CODE)))
            .andExpect(jsonPath("$.[*].displayOrder").value(hasItem(DEFAULT_DISPLAY_ORDER)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));

        // Check, that the count call also returns 1
        restCategoryMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultCategoryShouldNotBeFound(String filter) throws Exception {
        restCategoryMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restCategoryMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingCategory() throws Exception {
        // Get the category
        restCategoryMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingCategory() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        long databaseSizeBeforeUpdate = getRepositoryCount();
        categorySearchRepository.save(category);
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(categorySearchRepository.findAll());

        // Update the category
        Category updatedCategory = categoryRepository.findById(category.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedCategory are not directly saved in db
        em.detach(updatedCategory);
        updatedCategory
            .name(UPDATED_NAME)
            .slug(UPDATED_SLUG)
            .description(UPDATED_DESCRIPTION)
            .count(UPDATED_COUNT)
            .industryCode(UPDATED_INDUSTRY_CODE)
            .displayOrder(UPDATED_DISPLAY_ORDER)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
        CategoryDTO categoryDTO = categoryMapper.toDto(updatedCategory);

        restCategoryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, categoryDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(categoryDTO))
            )
            .andExpect(status().isOk());

        // Validate the Category in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedCategoryToMatchAllProperties(updatedCategory);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(categorySearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
                List<Category> categorySearchList = Streamable.of(categorySearchRepository.findAll()).toList();
                Category testCategorySearch = categorySearchList.get(searchDatabaseSizeAfter - 1);

                assertCategoryAllPropertiesEquals(testCategorySearch, updatedCategory);
            });
    }

    @Test
    @Transactional
    void putNonExistingCategory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(categorySearchRepository.findAll());
        category.setId(longCount.incrementAndGet());

        // Create the Category
        CategoryDTO categoryDTO = categoryMapper.toDto(category);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCategoryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, categoryDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(categoryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Category in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(categorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithIdMismatchCategory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(categorySearchRepository.findAll());
        category.setId(longCount.incrementAndGet());

        // Create the Category
        CategoryDTO categoryDTO = categoryMapper.toDto(category);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCategoryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(categoryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Category in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(categorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamCategory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(categorySearchRepository.findAll());
        category.setId(longCount.incrementAndGet());

        // Create the Category
        CategoryDTO categoryDTO = categoryMapper.toDto(category);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCategoryMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(categoryDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Category in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(categorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void partialUpdateCategoryWithPatch() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the category using partial update
        Category partialUpdatedCategory = new Category();
        partialUpdatedCategory.setId(category.getId());

        partialUpdatedCategory
            .name(UPDATED_NAME)
            .slug(UPDATED_SLUG)
            .description(UPDATED_DESCRIPTION)
            .count(UPDATED_COUNT)
            .industryCode(UPDATED_INDUSTRY_CODE)
            .updatedAt(UPDATED_UPDATED_AT);

        restCategoryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCategory.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCategory))
            )
            .andExpect(status().isOk());

        // Validate the Category in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCategoryUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedCategory, category), getPersistedCategory(category));
    }

    @Test
    @Transactional
    void fullUpdateCategoryWithPatch() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the category using partial update
        Category partialUpdatedCategory = new Category();
        partialUpdatedCategory.setId(category.getId());

        partialUpdatedCategory
            .name(UPDATED_NAME)
            .slug(UPDATED_SLUG)
            .description(UPDATED_DESCRIPTION)
            .count(UPDATED_COUNT)
            .industryCode(UPDATED_INDUSTRY_CODE)
            .displayOrder(UPDATED_DISPLAY_ORDER)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);

        restCategoryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedCategory.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedCategory))
            )
            .andExpect(status().isOk());

        // Validate the Category in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCategoryUpdatableFieldsEquals(partialUpdatedCategory, getPersistedCategory(partialUpdatedCategory));
    }

    @Test
    @Transactional
    void patchNonExistingCategory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(categorySearchRepository.findAll());
        category.setId(longCount.incrementAndGet());

        // Create the Category
        CategoryDTO categoryDTO = categoryMapper.toDto(category);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCategoryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, categoryDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(categoryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Category in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(categorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithIdMismatchCategory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(categorySearchRepository.findAll());
        category.setId(longCount.incrementAndGet());

        // Create the Category
        CategoryDTO categoryDTO = categoryMapper.toDto(category);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCategoryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(categoryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Category in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(categorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamCategory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(categorySearchRepository.findAll());
        category.setId(longCount.incrementAndGet());

        // Create the Category
        CategoryDTO categoryDTO = categoryMapper.toDto(category);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restCategoryMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(categoryDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Category in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(categorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void deleteCategory() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);
        categoryRepository.save(category);
        categorySearchRepository.save(category);

        long databaseSizeBeforeDelete = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(categorySearchRepository.findAll());
        assertThat(searchDatabaseSizeBefore).isEqualTo(databaseSizeBeforeDelete);

        // Delete the category
        restCategoryMockMvc
            .perform(delete(ENTITY_API_URL_ID, category.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(categorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore - 1);
    }

    @Test
    @Transactional
    void searchCategory() throws Exception {
        // Initialize the database
        insertedCategory = categoryRepository.saveAndFlush(category);
        categorySearchRepository.save(category);

        // Search the category
        restCategoryMockMvc
            .perform(get(ENTITY_SEARCH_API_URL + "?query=id:" + category.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(category.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION.toString())))
            .andExpect(jsonPath("$.[*].count").value(hasItem(DEFAULT_COUNT)))
            .andExpect(jsonPath("$.[*].industryCode").value(hasItem(DEFAULT_INDUSTRY_CODE)))
            .andExpect(jsonPath("$.[*].displayOrder").value(hasItem(DEFAULT_DISPLAY_ORDER)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    protected long getRepositoryCount() {
        return categoryRepository.count();
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

    protected Category getPersistedCategory(Category category) {
        return categoryRepository.findById(category.getId()).orElseThrow();
    }

    protected void assertPersistedCategoryToMatchAllProperties(Category expectedCategory) {
        assertCategoryAllPropertiesEquals(expectedCategory, getPersistedCategory(expectedCategory));
    }

    protected void assertPersistedCategoryToMatchUpdatableProperties(Category expectedCategory) {
        assertCategoryAllUpdatablePropertiesEquals(expectedCategory, getPersistedCategory(expectedCategory));
    }
}
