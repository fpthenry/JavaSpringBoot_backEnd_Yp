package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.BlogPostAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.BlogPost;
import com.mycompany.myapp.repository.BlogPostRepository;
import com.mycompany.myapp.repository.search.BlogPostSearchRepository;
import com.mycompany.myapp.service.dto.BlogPostDTO;
import com.mycompany.myapp.service.mapper.BlogPostMapper;
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
 * Integration tests for the {@link BlogPostResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class BlogPostResourceIT {

    private static final Long DEFAULT_WP_ID = 1L;
    private static final Long UPDATED_WP_ID = 2L;
    private static final Long SMALLER_WP_ID = 1L - 1L;

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_SLUG = "AAAAAAAAAA";
    private static final String UPDATED_SLUG = "BBBBBBBBBB";

    private static final String DEFAULT_CONTENT = "AAAAAAAAAA";
    private static final String UPDATED_CONTENT = "BBBBBBBBBB";

    private static final String DEFAULT_EXCERPT = "AAAAAAAAAA";
    private static final String UPDATED_EXCERPT = "BBBBBBBBBB";

    private static final String DEFAULT_THUMBNAIL = "AAAAAAAAAA";
    private static final String UPDATED_THUMBNAIL = "BBBBBBBBBB";

    private static final String DEFAULT_STATUS = "AAAAAAAAAA";
    private static final String UPDATED_STATUS = "BBBBBBBBBB";

    private static final Integer DEFAULT_VIEW_COUNT = 1;
    private static final Integer UPDATED_VIEW_COUNT = 2;
    private static final Integer SMALLER_VIEW_COUNT = 1 - 1;

    private static final Instant DEFAULT_PUBLISHED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_PUBLISHED_AT = Instant.ofEpochMilli(1790760789812L);

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1790760789812L);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.ofEpochMilli(1790760789812L);

    private static final String ENTITY_API_URL = "/api/blog-posts";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/blog-posts/_search";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private BlogPostRepository blogPostRepository;

    @Autowired
    private BlogPostMapper blogPostMapper;

    @Autowired
    private BlogPostSearchRepository blogPostSearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restBlogPostMockMvc;

    private BlogPost blogPost;

    private BlogPost insertedBlogPost;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static BlogPost createEntity() {
        return new BlogPost()
            .wpId(DEFAULT_WP_ID)
            .title(DEFAULT_TITLE)
            .slug(DEFAULT_SLUG)
            .content(DEFAULT_CONTENT)
            .excerpt(DEFAULT_EXCERPT)
            .thumbnail(DEFAULT_THUMBNAIL)
            .status(DEFAULT_STATUS)
            .viewCount(DEFAULT_VIEW_COUNT)
            .publishedAt(DEFAULT_PUBLISHED_AT)
            .createdAt(DEFAULT_CREATED_AT)
            .updatedAt(DEFAULT_UPDATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static BlogPost createUpdatedEntity() {
        return new BlogPost()
            .wpId(UPDATED_WP_ID)
            .title(UPDATED_TITLE)
            .slug(UPDATED_SLUG)
            .content(UPDATED_CONTENT)
            .excerpt(UPDATED_EXCERPT)
            .thumbnail(UPDATED_THUMBNAIL)
            .status(UPDATED_STATUS)
            .viewCount(UPDATED_VIEW_COUNT)
            .publishedAt(UPDATED_PUBLISHED_AT)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
    }

    @BeforeEach
    void initTest() {
        blogPost = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedBlogPost != null) {
            blogPostRepository.delete(insertedBlogPost);
            blogPostSearchRepository.delete(insertedBlogPost);
            insertedBlogPost = null;
        }
    }

    @Test
    @Transactional
    void createBlogPost() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(blogPostSearchRepository.findAll());
        // Create the BlogPost
        BlogPostDTO blogPostDTO = blogPostMapper.toDto(blogPost);
        var returnedBlogPostDTO = om.readValue(
            restBlogPostMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(blogPostDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            BlogPostDTO.class
        );

        // Validate the BlogPost in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedBlogPost = blogPostMapper.toEntity(returnedBlogPostDTO);
        assertBlogPostUpdatableFieldsEquals(returnedBlogPost, getPersistedBlogPost(returnedBlogPost));

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(blogPostSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore + 1);
            });

        insertedBlogPost = returnedBlogPost;
    }

    @Test
    @Transactional
    void createBlogPostWithExistingId() throws Exception {
        // Create the BlogPost with an existing ID
        blogPost.setId(1L);
        BlogPostDTO blogPostDTO = blogPostMapper.toDto(blogPost);

        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(blogPostSearchRepository.findAll());

        // An entity with an existing ID cannot be created, so this API call must fail
        restBlogPostMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(blogPostDTO)))
            .andExpect(status().isBadRequest());

        // Validate the BlogPost in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(blogPostSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkWpIdIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(blogPostSearchRepository.findAll());
        // set the field null
        blogPost.setWpId(null);

        // Create the BlogPost, which fails.
        BlogPostDTO blogPostDTO = blogPostMapper.toDto(blogPost);

        restBlogPostMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(blogPostDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(blogPostSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkTitleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(blogPostSearchRepository.findAll());
        // set the field null
        blogPost.setTitle(null);

        // Create the BlogPost, which fails.
        BlogPostDTO blogPostDTO = blogPostMapper.toDto(blogPost);

        restBlogPostMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(blogPostDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(blogPostSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void getAllBlogPosts() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList
        restBlogPostMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(blogPost.getId().intValue())))
            .andExpect(jsonPath("$.[*].wpId").value(hasItem(DEFAULT_WP_ID.intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].content").value(hasItem(DEFAULT_CONTENT)))
            .andExpect(jsonPath("$.[*].excerpt").value(hasItem(DEFAULT_EXCERPT)))
            .andExpect(jsonPath("$.[*].thumbnail").value(hasItem(DEFAULT_THUMBNAIL)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS)))
            .andExpect(jsonPath("$.[*].viewCount").value(hasItem(DEFAULT_VIEW_COUNT)))
            .andExpect(jsonPath("$.[*].publishedAt").value(hasItem(DEFAULT_PUBLISHED_AT.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    @Test
    @Transactional
    void getBlogPost() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get the blogPost
        restBlogPostMockMvc
            .perform(get(ENTITY_API_URL_ID, blogPost.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(blogPost.getId().intValue()))
            .andExpect(jsonPath("$.wpId").value(DEFAULT_WP_ID.intValue()))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
            .andExpect(jsonPath("$.slug").value(DEFAULT_SLUG))
            .andExpect(jsonPath("$.content").value(DEFAULT_CONTENT))
            .andExpect(jsonPath("$.excerpt").value(DEFAULT_EXCERPT))
            .andExpect(jsonPath("$.thumbnail").value(DEFAULT_THUMBNAIL))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS))
            .andExpect(jsonPath("$.viewCount").value(DEFAULT_VIEW_COUNT))
            .andExpect(jsonPath("$.publishedAt").value(DEFAULT_PUBLISHED_AT.toString()))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()));
    }

    @Test
    @Transactional
    void getBlogPostsByIdFiltering() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        Long id = blogPost.getId();

        defaultBlogPostFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultBlogPostFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultBlogPostFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllBlogPostsByWpIdIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where wpId equals to
        defaultBlogPostFiltering("wpId.equals=" + DEFAULT_WP_ID, "wpId.equals=" + UPDATED_WP_ID);
    }

    @Test
    @Transactional
    void getAllBlogPostsByWpIdIsInShouldWork() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where wpId in
        defaultBlogPostFiltering("wpId.in=" + DEFAULT_WP_ID + "," + UPDATED_WP_ID, "wpId.in=" + UPDATED_WP_ID);
    }

    @Test
    @Transactional
    void getAllBlogPostsByWpIdIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where wpId is not null
        defaultBlogPostFiltering("wpId.specified=true", "wpId.specified=false");
    }

    @Test
    @Transactional
    void getAllBlogPostsByWpIdIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where wpId is greater than or equal to
        defaultBlogPostFiltering("wpId.greaterThanOrEqual=" + DEFAULT_WP_ID, "wpId.greaterThanOrEqual=" + UPDATED_WP_ID);
    }

    @Test
    @Transactional
    void getAllBlogPostsByWpIdIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where wpId is less than or equal to
        defaultBlogPostFiltering("wpId.lessThanOrEqual=" + DEFAULT_WP_ID, "wpId.lessThanOrEqual=" + SMALLER_WP_ID);
    }

    @Test
    @Transactional
    void getAllBlogPostsByWpIdIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where wpId is less than
        defaultBlogPostFiltering("wpId.lessThan=" + UPDATED_WP_ID, "wpId.lessThan=" + DEFAULT_WP_ID);
    }

    @Test
    @Transactional
    void getAllBlogPostsByWpIdIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where wpId is greater than
        defaultBlogPostFiltering("wpId.greaterThan=" + SMALLER_WP_ID, "wpId.greaterThan=" + DEFAULT_WP_ID);
    }

    @Test
    @Transactional
    void getAllBlogPostsByTitleIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where title equals to
        defaultBlogPostFiltering("title.equals=" + DEFAULT_TITLE, "title.equals=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllBlogPostsByTitleIsInShouldWork() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where title in
        defaultBlogPostFiltering("title.in=" + DEFAULT_TITLE + "," + UPDATED_TITLE, "title.in=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllBlogPostsByTitleIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where title is not null
        defaultBlogPostFiltering("title.specified=true", "title.specified=false");
    }

    @Test
    @Transactional
    void getAllBlogPostsByTitleContainsSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where title contains
        defaultBlogPostFiltering("title.contains=" + DEFAULT_TITLE, "title.contains=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllBlogPostsByTitleNotContainsSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where title does not contain
        defaultBlogPostFiltering("title.doesNotContain=" + UPDATED_TITLE, "title.doesNotContain=" + DEFAULT_TITLE);
    }

    @Test
    @Transactional
    void getAllBlogPostsBySlugIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where slug equals to
        defaultBlogPostFiltering("slug.equals=" + DEFAULT_SLUG, "slug.equals=" + UPDATED_SLUG);
    }

    @Test
    @Transactional
    void getAllBlogPostsBySlugIsInShouldWork() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where slug in
        defaultBlogPostFiltering("slug.in=" + DEFAULT_SLUG + "," + UPDATED_SLUG, "slug.in=" + UPDATED_SLUG);
    }

    @Test
    @Transactional
    void getAllBlogPostsBySlugIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where slug is not null
        defaultBlogPostFiltering("slug.specified=true", "slug.specified=false");
    }

    @Test
    @Transactional
    void getAllBlogPostsBySlugContainsSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where slug contains
        defaultBlogPostFiltering("slug.contains=" + DEFAULT_SLUG, "slug.contains=" + UPDATED_SLUG);
    }

    @Test
    @Transactional
    void getAllBlogPostsBySlugNotContainsSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where slug does not contain
        defaultBlogPostFiltering("slug.doesNotContain=" + UPDATED_SLUG, "slug.doesNotContain=" + DEFAULT_SLUG);
    }

    @Test
    @Transactional
    void getAllBlogPostsByThumbnailIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where thumbnail equals to
        defaultBlogPostFiltering("thumbnail.equals=" + DEFAULT_THUMBNAIL, "thumbnail.equals=" + UPDATED_THUMBNAIL);
    }

    @Test
    @Transactional
    void getAllBlogPostsByThumbnailIsInShouldWork() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where thumbnail in
        defaultBlogPostFiltering("thumbnail.in=" + DEFAULT_THUMBNAIL + "," + UPDATED_THUMBNAIL, "thumbnail.in=" + UPDATED_THUMBNAIL);
    }

    @Test
    @Transactional
    void getAllBlogPostsByThumbnailIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where thumbnail is not null
        defaultBlogPostFiltering("thumbnail.specified=true", "thumbnail.specified=false");
    }

    @Test
    @Transactional
    void getAllBlogPostsByThumbnailContainsSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where thumbnail contains
        defaultBlogPostFiltering("thumbnail.contains=" + DEFAULT_THUMBNAIL, "thumbnail.contains=" + UPDATED_THUMBNAIL);
    }

    @Test
    @Transactional
    void getAllBlogPostsByThumbnailNotContainsSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where thumbnail does not contain
        defaultBlogPostFiltering("thumbnail.doesNotContain=" + UPDATED_THUMBNAIL, "thumbnail.doesNotContain=" + DEFAULT_THUMBNAIL);
    }

    @Test
    @Transactional
    void getAllBlogPostsByStatusIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where status equals to
        defaultBlogPostFiltering("status.equals=" + DEFAULT_STATUS, "status.equals=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllBlogPostsByStatusIsInShouldWork() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where status in
        defaultBlogPostFiltering("status.in=" + DEFAULT_STATUS + "," + UPDATED_STATUS, "status.in=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllBlogPostsByStatusIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where status is not null
        defaultBlogPostFiltering("status.specified=true", "status.specified=false");
    }

    @Test
    @Transactional
    void getAllBlogPostsByStatusContainsSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where status contains
        defaultBlogPostFiltering("status.contains=" + DEFAULT_STATUS, "status.contains=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllBlogPostsByStatusNotContainsSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where status does not contain
        defaultBlogPostFiltering("status.doesNotContain=" + UPDATED_STATUS, "status.doesNotContain=" + DEFAULT_STATUS);
    }

    @Test
    @Transactional
    void getAllBlogPostsByViewCountIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where viewCount equals to
        defaultBlogPostFiltering("viewCount.equals=" + DEFAULT_VIEW_COUNT, "viewCount.equals=" + UPDATED_VIEW_COUNT);
    }

    @Test
    @Transactional
    void getAllBlogPostsByViewCountIsInShouldWork() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where viewCount in
        defaultBlogPostFiltering("viewCount.in=" + DEFAULT_VIEW_COUNT + "," + UPDATED_VIEW_COUNT, "viewCount.in=" + UPDATED_VIEW_COUNT);
    }

    @Test
    @Transactional
    void getAllBlogPostsByViewCountIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where viewCount is not null
        defaultBlogPostFiltering("viewCount.specified=true", "viewCount.specified=false");
    }

    @Test
    @Transactional
    void getAllBlogPostsByViewCountIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where viewCount is greater than or equal to
        defaultBlogPostFiltering(
            "viewCount.greaterThanOrEqual=" + DEFAULT_VIEW_COUNT,
            "viewCount.greaterThanOrEqual=" + UPDATED_VIEW_COUNT
        );
    }

    @Test
    @Transactional
    void getAllBlogPostsByViewCountIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where viewCount is less than or equal to
        defaultBlogPostFiltering("viewCount.lessThanOrEqual=" + DEFAULT_VIEW_COUNT, "viewCount.lessThanOrEqual=" + SMALLER_VIEW_COUNT);
    }

    @Test
    @Transactional
    void getAllBlogPostsByViewCountIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where viewCount is less than
        defaultBlogPostFiltering("viewCount.lessThan=" + UPDATED_VIEW_COUNT, "viewCount.lessThan=" + DEFAULT_VIEW_COUNT);
    }

    @Test
    @Transactional
    void getAllBlogPostsByViewCountIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where viewCount is greater than
        defaultBlogPostFiltering("viewCount.greaterThan=" + SMALLER_VIEW_COUNT, "viewCount.greaterThan=" + DEFAULT_VIEW_COUNT);
    }

    @Test
    @Transactional
    void getAllBlogPostsByPublishedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where publishedAt equals to
        defaultBlogPostFiltering("publishedAt.equals=" + DEFAULT_PUBLISHED_AT, "publishedAt.equals=" + UPDATED_PUBLISHED_AT);
    }

    @Test
    @Transactional
    void getAllBlogPostsByPublishedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where publishedAt in
        defaultBlogPostFiltering(
            "publishedAt.in=" + DEFAULT_PUBLISHED_AT + "," + UPDATED_PUBLISHED_AT,
            "publishedAt.in=" + UPDATED_PUBLISHED_AT
        );
    }

    @Test
    @Transactional
    void getAllBlogPostsByPublishedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where publishedAt is not null
        defaultBlogPostFiltering("publishedAt.specified=true", "publishedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllBlogPostsByCreatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where createdAt equals to
        defaultBlogPostFiltering("createdAt.equals=" + DEFAULT_CREATED_AT, "createdAt.equals=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllBlogPostsByCreatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where createdAt in
        defaultBlogPostFiltering("createdAt.in=" + DEFAULT_CREATED_AT + "," + UPDATED_CREATED_AT, "createdAt.in=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllBlogPostsByCreatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where createdAt is not null
        defaultBlogPostFiltering("createdAt.specified=true", "createdAt.specified=false");
    }

    @Test
    @Transactional
    void getAllBlogPostsByUpdatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where updatedAt equals to
        defaultBlogPostFiltering("updatedAt.equals=" + DEFAULT_UPDATED_AT, "updatedAt.equals=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllBlogPostsByUpdatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where updatedAt in
        defaultBlogPostFiltering("updatedAt.in=" + DEFAULT_UPDATED_AT + "," + UPDATED_UPDATED_AT, "updatedAt.in=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllBlogPostsByUpdatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        // Get all the blogPostList where updatedAt is not null
        defaultBlogPostFiltering("updatedAt.specified=true", "updatedAt.specified=false");
    }

    private void defaultBlogPostFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultBlogPostShouldBeFound(shouldBeFound);
        defaultBlogPostShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultBlogPostShouldBeFound(String filter) throws Exception {
        restBlogPostMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(blogPost.getId().intValue())))
            .andExpect(jsonPath("$.[*].wpId").value(hasItem(DEFAULT_WP_ID.intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].content").value(hasItem(DEFAULT_CONTENT)))
            .andExpect(jsonPath("$.[*].excerpt").value(hasItem(DEFAULT_EXCERPT)))
            .andExpect(jsonPath("$.[*].thumbnail").value(hasItem(DEFAULT_THUMBNAIL)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS)))
            .andExpect(jsonPath("$.[*].viewCount").value(hasItem(DEFAULT_VIEW_COUNT)))
            .andExpect(jsonPath("$.[*].publishedAt").value(hasItem(DEFAULT_PUBLISHED_AT.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));

        // Check, that the count call also returns 1
        restBlogPostMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultBlogPostShouldNotBeFound(String filter) throws Exception {
        restBlogPostMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restBlogPostMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingBlogPost() throws Exception {
        // Get the blogPost
        restBlogPostMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingBlogPost() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        long databaseSizeBeforeUpdate = getRepositoryCount();
        blogPostSearchRepository.save(blogPost);
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(blogPostSearchRepository.findAll());

        // Update the blogPost
        BlogPost updatedBlogPost = blogPostRepository.findById(blogPost.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedBlogPost are not directly saved in db
        em.detach(updatedBlogPost);
        updatedBlogPost
            .wpId(UPDATED_WP_ID)
            .title(UPDATED_TITLE)
            .slug(UPDATED_SLUG)
            .content(UPDATED_CONTENT)
            .excerpt(UPDATED_EXCERPT)
            .thumbnail(UPDATED_THUMBNAIL)
            .status(UPDATED_STATUS)
            .viewCount(UPDATED_VIEW_COUNT)
            .publishedAt(UPDATED_PUBLISHED_AT)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
        BlogPostDTO blogPostDTO = blogPostMapper.toDto(updatedBlogPost);

        restBlogPostMockMvc
            .perform(
                put(ENTITY_API_URL_ID, blogPostDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(blogPostDTO))
            )
            .andExpect(status().isOk());

        // Validate the BlogPost in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedBlogPostToMatchAllProperties(updatedBlogPost);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(blogPostSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
                List<BlogPost> blogPostSearchList = Streamable.of(blogPostSearchRepository.findAll()).toList();
                BlogPost testBlogPostSearch = blogPostSearchList.get(searchDatabaseSizeAfter - 1);

                assertBlogPostAllPropertiesEquals(testBlogPostSearch, updatedBlogPost);
            });
    }

    @Test
    @Transactional
    void putNonExistingBlogPost() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(blogPostSearchRepository.findAll());
        blogPost.setId(longCount.incrementAndGet());

        // Create the BlogPost
        BlogPostDTO blogPostDTO = blogPostMapper.toDto(blogPost);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restBlogPostMockMvc
            .perform(
                put(ENTITY_API_URL_ID, blogPostDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(blogPostDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the BlogPost in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(blogPostSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithIdMismatchBlogPost() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(blogPostSearchRepository.findAll());
        blogPost.setId(longCount.incrementAndGet());

        // Create the BlogPost
        BlogPostDTO blogPostDTO = blogPostMapper.toDto(blogPost);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restBlogPostMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(blogPostDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the BlogPost in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(blogPostSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamBlogPost() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(blogPostSearchRepository.findAll());
        blogPost.setId(longCount.incrementAndGet());

        // Create the BlogPost
        BlogPostDTO blogPostDTO = blogPostMapper.toDto(blogPost);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restBlogPostMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(blogPostDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the BlogPost in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(blogPostSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void partialUpdateBlogPostWithPatch() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the blogPost using partial update
        BlogPost partialUpdatedBlogPost = new BlogPost();
        partialUpdatedBlogPost.setId(blogPost.getId());

        partialUpdatedBlogPost
            .slug(UPDATED_SLUG)
            .content(UPDATED_CONTENT)
            .excerpt(UPDATED_EXCERPT)
            .viewCount(UPDATED_VIEW_COUNT)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);

        restBlogPostMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedBlogPost.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedBlogPost))
            )
            .andExpect(status().isOk());

        // Validate the BlogPost in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertBlogPostUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedBlogPost, blogPost), getPersistedBlogPost(blogPost));
    }

    @Test
    @Transactional
    void fullUpdateBlogPostWithPatch() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the blogPost using partial update
        BlogPost partialUpdatedBlogPost = new BlogPost();
        partialUpdatedBlogPost.setId(blogPost.getId());

        partialUpdatedBlogPost
            .wpId(UPDATED_WP_ID)
            .title(UPDATED_TITLE)
            .slug(UPDATED_SLUG)
            .content(UPDATED_CONTENT)
            .excerpt(UPDATED_EXCERPT)
            .thumbnail(UPDATED_THUMBNAIL)
            .status(UPDATED_STATUS)
            .viewCount(UPDATED_VIEW_COUNT)
            .publishedAt(UPDATED_PUBLISHED_AT)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);

        restBlogPostMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedBlogPost.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedBlogPost))
            )
            .andExpect(status().isOk());

        // Validate the BlogPost in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertBlogPostUpdatableFieldsEquals(partialUpdatedBlogPost, getPersistedBlogPost(partialUpdatedBlogPost));
    }

    @Test
    @Transactional
    void patchNonExistingBlogPost() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(blogPostSearchRepository.findAll());
        blogPost.setId(longCount.incrementAndGet());

        // Create the BlogPost
        BlogPostDTO blogPostDTO = blogPostMapper.toDto(blogPost);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restBlogPostMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, blogPostDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(blogPostDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the BlogPost in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(blogPostSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithIdMismatchBlogPost() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(blogPostSearchRepository.findAll());
        blogPost.setId(longCount.incrementAndGet());

        // Create the BlogPost
        BlogPostDTO blogPostDTO = blogPostMapper.toDto(blogPost);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restBlogPostMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(blogPostDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the BlogPost in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(blogPostSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamBlogPost() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(blogPostSearchRepository.findAll());
        blogPost.setId(longCount.incrementAndGet());

        // Create the BlogPost
        BlogPostDTO blogPostDTO = blogPostMapper.toDto(blogPost);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restBlogPostMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(blogPostDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the BlogPost in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(blogPostSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void deleteBlogPost() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);
        blogPostRepository.save(blogPost);
        blogPostSearchRepository.save(blogPost);

        long databaseSizeBeforeDelete = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(blogPostSearchRepository.findAll());
        assertThat(searchDatabaseSizeBefore).isEqualTo(databaseSizeBeforeDelete);

        // Delete the blogPost
        restBlogPostMockMvc
            .perform(delete(ENTITY_API_URL_ID, blogPost.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(blogPostSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore - 1);
    }

    @Test
    @Transactional
    void searchBlogPost() throws Exception {
        // Initialize the database
        insertedBlogPost = blogPostRepository.saveAndFlush(blogPost);
        blogPostSearchRepository.save(blogPost);

        // Search the blogPost
        restBlogPostMockMvc
            .perform(get(ENTITY_SEARCH_API_URL + "?query=id:" + blogPost.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(blogPost.getId().intValue())))
            .andExpect(jsonPath("$.[*].wpId").value(hasItem(DEFAULT_WP_ID.intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].content").value(hasItem(DEFAULT_CONTENT.toString())))
            .andExpect(jsonPath("$.[*].excerpt").value(hasItem(DEFAULT_EXCERPT.toString())))
            .andExpect(jsonPath("$.[*].thumbnail").value(hasItem(DEFAULT_THUMBNAIL)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS)))
            .andExpect(jsonPath("$.[*].viewCount").value(hasItem(DEFAULT_VIEW_COUNT)))
            .andExpect(jsonPath("$.[*].publishedAt").value(hasItem(DEFAULT_PUBLISHED_AT.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    protected long getRepositoryCount() {
        return blogPostRepository.count();
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

    protected BlogPost getPersistedBlogPost(BlogPost blogPost) {
        return blogPostRepository.findById(blogPost.getId()).orElseThrow();
    }

    protected void assertPersistedBlogPostToMatchAllProperties(BlogPost expectedBlogPost) {
        assertBlogPostAllPropertiesEquals(expectedBlogPost, getPersistedBlogPost(expectedBlogPost));
    }

    protected void assertPersistedBlogPostToMatchUpdatableProperties(BlogPost expectedBlogPost) {
        assertBlogPostAllUpdatablePropertiesEquals(expectedBlogPost, getPersistedBlogPost(expectedBlogPost));
    }
}
