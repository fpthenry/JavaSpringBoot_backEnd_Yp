package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.BlogCategoryAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.BlogCategory;
import com.mycompany.myapp.domain.BlogCategory;
import com.mycompany.myapp.domain.BlogPost;
import com.mycompany.myapp.repository.BlogCategoryRepository;
import com.mycompany.myapp.repository.search.BlogCategorySearchRepository;
import com.mycompany.myapp.service.BlogCategoryService;
import com.mycompany.myapp.service.dto.BlogCategoryDTO;
import com.mycompany.myapp.service.mapper.BlogCategoryMapper;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Streamable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link BlogCategoryResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class BlogCategoryResourceIT {

    private static final Long DEFAULT_WP_TERM_ID = 1L;
    private static final Long UPDATED_WP_TERM_ID = 2L;
    private static final Long SMALLER_WP_TERM_ID = 1L - 1L;

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_SLUG = "AAAAAAAAAA";
    private static final String UPDATED_SLUG = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final Integer DEFAULT_POST_COUNT = 1;
    private static final Integer UPDATED_POST_COUNT = 2;
    private static final Integer SMALLER_POST_COUNT = 1 - 1;

    private static final String ENTITY_API_URL = "/api/blog-categories";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/blog-categories/_search";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private BlogCategoryRepository blogCategoryRepository;

    @Mock
    private BlogCategoryRepository blogCategoryRepositoryMock;

    @Autowired
    private BlogCategoryMapper blogCategoryMapper;

    @Mock
    private BlogCategoryService blogCategoryServiceMock;

    @Autowired
    private BlogCategorySearchRepository blogCategorySearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restBlogCategoryMockMvc;

    private BlogCategory blogCategory;

    private BlogCategory insertedBlogCategory;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static BlogCategory createEntity() {
        return new BlogCategory()
            .wpTermId(DEFAULT_WP_TERM_ID)
            .name(DEFAULT_NAME)
            .slug(DEFAULT_SLUG)
            .description(DEFAULT_DESCRIPTION)
            .postCount(DEFAULT_POST_COUNT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static BlogCategory createUpdatedEntity() {
        return new BlogCategory()
            .wpTermId(UPDATED_WP_TERM_ID)
            .name(UPDATED_NAME)
            .slug(UPDATED_SLUG)
            .description(UPDATED_DESCRIPTION)
            .postCount(UPDATED_POST_COUNT);
    }

    @BeforeEach
    void initTest() {
        blogCategory = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedBlogCategory != null) {
            blogCategoryRepository.delete(insertedBlogCategory);
            blogCategorySearchRepository.delete(insertedBlogCategory);
            insertedBlogCategory = null;
        }
    }

    @Test
    @Transactional
    void createBlogCategory() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(blogCategorySearchRepository.findAll());
        // Create the BlogCategory
        BlogCategoryDTO blogCategoryDTO = blogCategoryMapper.toDto(blogCategory);
        var returnedBlogCategoryDTO = om.readValue(
            restBlogCategoryMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(blogCategoryDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            BlogCategoryDTO.class
        );

        // Validate the BlogCategory in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedBlogCategory = blogCategoryMapper.toEntity(returnedBlogCategoryDTO);
        assertBlogCategoryUpdatableFieldsEquals(returnedBlogCategory, getPersistedBlogCategory(returnedBlogCategory));

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(blogCategorySearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore + 1);
            });

        insertedBlogCategory = returnedBlogCategory;
    }

    @Test
    @Transactional
    void createBlogCategoryWithExistingId() throws Exception {
        // Create the BlogCategory with an existing ID
        blogCategory.setId(1L);
        BlogCategoryDTO blogCategoryDTO = blogCategoryMapper.toDto(blogCategory);

        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(blogCategorySearchRepository.findAll());

        // An entity with an existing ID cannot be created, so this API call must fail
        restBlogCategoryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(blogCategoryDTO)))
            .andExpect(status().isBadRequest());

        // Validate the BlogCategory in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(blogCategorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(blogCategorySearchRepository.findAll());
        // set the field null
        blogCategory.setName(null);

        // Create the BlogCategory, which fails.
        BlogCategoryDTO blogCategoryDTO = blogCategoryMapper.toDto(blogCategory);

        restBlogCategoryMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(blogCategoryDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(blogCategorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void getAllBlogCategories() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList
        restBlogCategoryMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(blogCategory.getId().intValue())))
            .andExpect(jsonPath("$.[*].wpTermId").value(hasItem(DEFAULT_WP_TERM_ID.intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].postCount").value(hasItem(DEFAULT_POST_COUNT)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllBlogCategoriesWithEagerRelationshipsIsEnabled() throws Exception {
        when(blogCategoryServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restBlogCategoryMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(blogCategoryServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllBlogCategoriesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(blogCategoryServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restBlogCategoryMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(blogCategoryRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getBlogCategory() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get the blogCategory
        restBlogCategoryMockMvc
            .perform(get(ENTITY_API_URL_ID, blogCategory.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(blogCategory.getId().intValue()))
            .andExpect(jsonPath("$.wpTermId").value(DEFAULT_WP_TERM_ID.intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.slug").value(DEFAULT_SLUG))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION))
            .andExpect(jsonPath("$.postCount").value(DEFAULT_POST_COUNT));
    }

    @Test
    @Transactional
    void getBlogCategoriesByIdFiltering() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        Long id = blogCategory.getId();

        defaultBlogCategoryFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultBlogCategoryFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultBlogCategoryFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllBlogCategoriesByWpTermIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList where wpTermId equals to
        defaultBlogCategoryFiltering("wpTermId.equals=" + DEFAULT_WP_TERM_ID, "wpTermId.equals=" + UPDATED_WP_TERM_ID);
    }

    @Test
    @Transactional
    void getAllBlogCategoriesByWpTermIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList where wpTermId in
        defaultBlogCategoryFiltering("wpTermId.in=" + DEFAULT_WP_TERM_ID + "," + UPDATED_WP_TERM_ID, "wpTermId.in=" + UPDATED_WP_TERM_ID);
    }

    @Test
    @Transactional
    void getAllBlogCategoriesByWpTermIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList where wpTermId is not null
        defaultBlogCategoryFiltering("wpTermId.specified=true", "wpTermId.specified=false");
    }

    @Test
    @Transactional
    void getAllBlogCategoriesByWpTermIdIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList where wpTermId is greater than or equal to
        defaultBlogCategoryFiltering(
            "wpTermId.greaterThanOrEqual=" + DEFAULT_WP_TERM_ID,
            "wpTermId.greaterThanOrEqual=" + UPDATED_WP_TERM_ID
        );
    }

    @Test
    @Transactional
    void getAllBlogCategoriesByWpTermIdIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList where wpTermId is less than or equal to
        defaultBlogCategoryFiltering("wpTermId.lessThanOrEqual=" + DEFAULT_WP_TERM_ID, "wpTermId.lessThanOrEqual=" + SMALLER_WP_TERM_ID);
    }

    @Test
    @Transactional
    void getAllBlogCategoriesByWpTermIdIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList where wpTermId is less than
        defaultBlogCategoryFiltering("wpTermId.lessThan=" + UPDATED_WP_TERM_ID, "wpTermId.lessThan=" + DEFAULT_WP_TERM_ID);
    }

    @Test
    @Transactional
    void getAllBlogCategoriesByWpTermIdIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList where wpTermId is greater than
        defaultBlogCategoryFiltering("wpTermId.greaterThan=" + SMALLER_WP_TERM_ID, "wpTermId.greaterThan=" + DEFAULT_WP_TERM_ID);
    }

    @Test
    @Transactional
    void getAllBlogCategoriesByNameIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList where name equals to
        defaultBlogCategoryFiltering("name.equals=" + DEFAULT_NAME, "name.equals=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllBlogCategoriesByNameIsInShouldWork() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList where name in
        defaultBlogCategoryFiltering("name.in=" + DEFAULT_NAME + "," + UPDATED_NAME, "name.in=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllBlogCategoriesByNameIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList where name is not null
        defaultBlogCategoryFiltering("name.specified=true", "name.specified=false");
    }

    @Test
    @Transactional
    void getAllBlogCategoriesByNameContainsSomething() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList where name contains
        defaultBlogCategoryFiltering("name.contains=" + DEFAULT_NAME, "name.contains=" + UPDATED_NAME);
    }

    @Test
    @Transactional
    void getAllBlogCategoriesByNameNotContainsSomething() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList where name does not contain
        defaultBlogCategoryFiltering("name.doesNotContain=" + UPDATED_NAME, "name.doesNotContain=" + DEFAULT_NAME);
    }

    @Test
    @Transactional
    void getAllBlogCategoriesBySlugIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList where slug equals to
        defaultBlogCategoryFiltering("slug.equals=" + DEFAULT_SLUG, "slug.equals=" + UPDATED_SLUG);
    }

    @Test
    @Transactional
    void getAllBlogCategoriesBySlugIsInShouldWork() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList where slug in
        defaultBlogCategoryFiltering("slug.in=" + DEFAULT_SLUG + "," + UPDATED_SLUG, "slug.in=" + UPDATED_SLUG);
    }

    @Test
    @Transactional
    void getAllBlogCategoriesBySlugIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList where slug is not null
        defaultBlogCategoryFiltering("slug.specified=true", "slug.specified=false");
    }

    @Test
    @Transactional
    void getAllBlogCategoriesBySlugContainsSomething() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList where slug contains
        defaultBlogCategoryFiltering("slug.contains=" + DEFAULT_SLUG, "slug.contains=" + UPDATED_SLUG);
    }

    @Test
    @Transactional
    void getAllBlogCategoriesBySlugNotContainsSomething() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList where slug does not contain
        defaultBlogCategoryFiltering("slug.doesNotContain=" + UPDATED_SLUG, "slug.doesNotContain=" + DEFAULT_SLUG);
    }

    @Test
    @Transactional
    void getAllBlogCategoriesByPostCountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList where postCount equals to
        defaultBlogCategoryFiltering("postCount.equals=" + DEFAULT_POST_COUNT, "postCount.equals=" + UPDATED_POST_COUNT);
    }

    @Test
    @Transactional
    void getAllBlogCategoriesByPostCountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList where postCount in
        defaultBlogCategoryFiltering("postCount.in=" + DEFAULT_POST_COUNT + "," + UPDATED_POST_COUNT, "postCount.in=" + UPDATED_POST_COUNT);
    }

    @Test
    @Transactional
    void getAllBlogCategoriesByPostCountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList where postCount is not null
        defaultBlogCategoryFiltering("postCount.specified=true", "postCount.specified=false");
    }

    @Test
    @Transactional
    void getAllBlogCategoriesByPostCountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList where postCount is greater than or equal to
        defaultBlogCategoryFiltering(
            "postCount.greaterThanOrEqual=" + DEFAULT_POST_COUNT,
            "postCount.greaterThanOrEqual=" + UPDATED_POST_COUNT
        );
    }

    @Test
    @Transactional
    void getAllBlogCategoriesByPostCountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList where postCount is less than or equal to
        defaultBlogCategoryFiltering("postCount.lessThanOrEqual=" + DEFAULT_POST_COUNT, "postCount.lessThanOrEqual=" + SMALLER_POST_COUNT);
    }

    @Test
    @Transactional
    void getAllBlogCategoriesByPostCountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList where postCount is less than
        defaultBlogCategoryFiltering("postCount.lessThan=" + UPDATED_POST_COUNT, "postCount.lessThan=" + DEFAULT_POST_COUNT);
    }

    @Test
    @Transactional
    void getAllBlogCategoriesByPostCountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        // Get all the blogCategoryList where postCount is greater than
        defaultBlogCategoryFiltering("postCount.greaterThan=" + SMALLER_POST_COUNT, "postCount.greaterThan=" + DEFAULT_POST_COUNT);
    }

    @Test
    @Transactional
    void getAllBlogCategoriesByParentIsEqualToSomething() throws Exception {
        BlogCategory parent;
        if (TestUtil.findAll(em, BlogCategory.class).isEmpty()) {
            blogCategoryRepository.saveAndFlush(blogCategory);
            parent = BlogCategoryResourceIT.createEntity();
        } else {
            parent = TestUtil.findAll(em, BlogCategory.class).get(0);
        }
        em.persist(parent);
        em.flush();
        blogCategory.setParent(parent);
        blogCategoryRepository.saveAndFlush(blogCategory);
        Long parentId = parent.getId();
        // Get all the blogCategoryList where parent equals to parentId
        defaultBlogCategoryShouldBeFound("parentId.equals=" + parentId);

        // Get all the blogCategoryList where parent equals to (parentId + 1)
        defaultBlogCategoryShouldNotBeFound("parentId.equals=" + (parentId + 1));
    }

    @Test
    @Transactional
    void getAllBlogCategoriesByBlogPostIsEqualToSomething() throws Exception {
        BlogPost blogPost;
        if (TestUtil.findAll(em, BlogPost.class).isEmpty()) {
            blogCategoryRepository.saveAndFlush(blogCategory);
            blogPost = BlogPostResourceIT.createEntity();
        } else {
            blogPost = TestUtil.findAll(em, BlogPost.class).get(0);
        }
        em.persist(blogPost);
        em.flush();
        blogCategory.addBlogPost(blogPost);
        blogCategoryRepository.saveAndFlush(blogCategory);
        Long blogPostId = blogPost.getId();
        // Get all the blogCategoryList where blogPost equals to blogPostId
        defaultBlogCategoryShouldBeFound("blogPostId.equals=" + blogPostId);

        // Get all the blogCategoryList where blogPost equals to (blogPostId + 1)
        defaultBlogCategoryShouldNotBeFound("blogPostId.equals=" + (blogPostId + 1));
    }

    private void defaultBlogCategoryFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultBlogCategoryShouldBeFound(shouldBeFound);
        defaultBlogCategoryShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultBlogCategoryShouldBeFound(String filter) throws Exception {
        restBlogCategoryMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(blogCategory.getId().intValue())))
            .andExpect(jsonPath("$.[*].wpTermId").value(hasItem(DEFAULT_WP_TERM_ID.intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION)))
            .andExpect(jsonPath("$.[*].postCount").value(hasItem(DEFAULT_POST_COUNT)));

        // Check, that the count call also returns 1
        restBlogCategoryMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultBlogCategoryShouldNotBeFound(String filter) throws Exception {
        restBlogCategoryMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restBlogCategoryMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingBlogCategory() throws Exception {
        // Get the blogCategory
        restBlogCategoryMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingBlogCategory() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        long databaseSizeBeforeUpdate = getRepositoryCount();
        blogCategorySearchRepository.save(blogCategory);
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(blogCategorySearchRepository.findAll());

        // Update the blogCategory
        BlogCategory updatedBlogCategory = blogCategoryRepository.findById(blogCategory.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedBlogCategory are not directly saved in db
        em.detach(updatedBlogCategory);
        updatedBlogCategory
            .wpTermId(UPDATED_WP_TERM_ID)
            .name(UPDATED_NAME)
            .slug(UPDATED_SLUG)
            .description(UPDATED_DESCRIPTION)
            .postCount(UPDATED_POST_COUNT);
        BlogCategoryDTO blogCategoryDTO = blogCategoryMapper.toDto(updatedBlogCategory);

        restBlogCategoryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, blogCategoryDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(blogCategoryDTO))
            )
            .andExpect(status().isOk());

        // Validate the BlogCategory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedBlogCategoryToMatchAllProperties(updatedBlogCategory);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(blogCategorySearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
                List<BlogCategory> blogCategorySearchList = Streamable.of(blogCategorySearchRepository.findAll()).toList();
                BlogCategory testBlogCategorySearch = blogCategorySearchList.get(searchDatabaseSizeAfter - 1);

                assertBlogCategoryAllPropertiesEquals(testBlogCategorySearch, updatedBlogCategory);
            });
    }

    @Test
    @Transactional
    void putNonExistingBlogCategory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(blogCategorySearchRepository.findAll());
        blogCategory.setId(longCount.incrementAndGet());

        // Create the BlogCategory
        BlogCategoryDTO blogCategoryDTO = blogCategoryMapper.toDto(blogCategory);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restBlogCategoryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, blogCategoryDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(blogCategoryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the BlogCategory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(blogCategorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithIdMismatchBlogCategory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(blogCategorySearchRepository.findAll());
        blogCategory.setId(longCount.incrementAndGet());

        // Create the BlogCategory
        BlogCategoryDTO blogCategoryDTO = blogCategoryMapper.toDto(blogCategory);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restBlogCategoryMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(blogCategoryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the BlogCategory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(blogCategorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamBlogCategory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(blogCategorySearchRepository.findAll());
        blogCategory.setId(longCount.incrementAndGet());

        // Create the BlogCategory
        BlogCategoryDTO blogCategoryDTO = blogCategoryMapper.toDto(blogCategory);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restBlogCategoryMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(blogCategoryDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the BlogCategory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(blogCategorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void partialUpdateBlogCategoryWithPatch() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the blogCategory using partial update
        BlogCategory partialUpdatedBlogCategory = new BlogCategory();
        partialUpdatedBlogCategory.setId(blogCategory.getId());

        partialUpdatedBlogCategory.slug(UPDATED_SLUG).description(UPDATED_DESCRIPTION);

        restBlogCategoryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedBlogCategory.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedBlogCategory))
            )
            .andExpect(status().isOk());

        // Validate the BlogCategory in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertBlogCategoryUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedBlogCategory, blogCategory),
            getPersistedBlogCategory(blogCategory)
        );
    }

    @Test
    @Transactional
    void fullUpdateBlogCategoryWithPatch() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the blogCategory using partial update
        BlogCategory partialUpdatedBlogCategory = new BlogCategory();
        partialUpdatedBlogCategory.setId(blogCategory.getId());

        partialUpdatedBlogCategory
            .wpTermId(UPDATED_WP_TERM_ID)
            .name(UPDATED_NAME)
            .slug(UPDATED_SLUG)
            .description(UPDATED_DESCRIPTION)
            .postCount(UPDATED_POST_COUNT);

        restBlogCategoryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedBlogCategory.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedBlogCategory))
            )
            .andExpect(status().isOk());

        // Validate the BlogCategory in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertBlogCategoryUpdatableFieldsEquals(partialUpdatedBlogCategory, getPersistedBlogCategory(partialUpdatedBlogCategory));
    }

    @Test
    @Transactional
    void patchNonExistingBlogCategory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(blogCategorySearchRepository.findAll());
        blogCategory.setId(longCount.incrementAndGet());

        // Create the BlogCategory
        BlogCategoryDTO blogCategoryDTO = blogCategoryMapper.toDto(blogCategory);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restBlogCategoryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, blogCategoryDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(blogCategoryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the BlogCategory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(blogCategorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithIdMismatchBlogCategory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(blogCategorySearchRepository.findAll());
        blogCategory.setId(longCount.incrementAndGet());

        // Create the BlogCategory
        BlogCategoryDTO blogCategoryDTO = blogCategoryMapper.toDto(blogCategory);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restBlogCategoryMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(blogCategoryDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the BlogCategory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(blogCategorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamBlogCategory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(blogCategorySearchRepository.findAll());
        blogCategory.setId(longCount.incrementAndGet());

        // Create the BlogCategory
        BlogCategoryDTO blogCategoryDTO = blogCategoryMapper.toDto(blogCategory);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restBlogCategoryMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(blogCategoryDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the BlogCategory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(blogCategorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void deleteBlogCategory() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);
        blogCategoryRepository.save(blogCategory);
        blogCategorySearchRepository.save(blogCategory);

        long databaseSizeBeforeDelete = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(blogCategorySearchRepository.findAll());
        assertThat(searchDatabaseSizeBefore).isEqualTo(databaseSizeBeforeDelete);

        // Delete the blogCategory
        restBlogCategoryMockMvc
            .perform(delete(ENTITY_API_URL_ID, blogCategory.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(blogCategorySearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore - 1);
    }

    @Test
    @Transactional
    void searchBlogCategory() throws Exception {
        // Initialize the database
        insertedBlogCategory = blogCategoryRepository.saveAndFlush(blogCategory);
        blogCategorySearchRepository.save(blogCategory);

        // Search the blogCategory
        restBlogCategoryMockMvc
            .perform(get(ENTITY_SEARCH_API_URL + "?query=id:" + blogCategory.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(blogCategory.getId().intValue())))
            .andExpect(jsonPath("$.[*].wpTermId").value(hasItem(DEFAULT_WP_TERM_ID.intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION.toString())))
            .andExpect(jsonPath("$.[*].postCount").value(hasItem(DEFAULT_POST_COUNT)));
    }

    protected long getRepositoryCount() {
        return blogCategoryRepository.count();
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

    protected BlogCategory getPersistedBlogCategory(BlogCategory blogCategory) {
        return blogCategoryRepository.findById(blogCategory.getId()).orElseThrow();
    }

    protected void assertPersistedBlogCategoryToMatchAllProperties(BlogCategory expectedBlogCategory) {
        assertBlogCategoryAllPropertiesEquals(expectedBlogCategory, getPersistedBlogCategory(expectedBlogCategory));
    }

    protected void assertPersistedBlogCategoryToMatchUpdatableProperties(BlogCategory expectedBlogCategory) {
        assertBlogCategoryAllUpdatablePropertiesEquals(expectedBlogCategory, getPersistedBlogCategory(expectedBlogCategory));
    }
}
