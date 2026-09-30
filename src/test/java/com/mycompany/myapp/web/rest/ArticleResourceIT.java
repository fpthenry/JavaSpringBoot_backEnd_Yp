package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.ArticleAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Article;
import com.mycompany.myapp.domain.ArticleCategory;
import com.mycompany.myapp.domain.ArticleTag;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.ArticleRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.repository.search.ArticleSearchRepository;
import com.mycompany.myapp.service.ArticleService;
import com.mycompany.myapp.service.dto.ArticleDTO;
import com.mycompany.myapp.service.mapper.ArticleMapper;
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
 * Integration tests for the {@link ArticleResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class ArticleResourceIT {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_SLUG = "AAAAAAAAAA";
    private static final String UPDATED_SLUG = "BBBBBBBBBB";

    private static final String DEFAULT_EXCERPT = "AAAAAAAAAA";
    private static final String UPDATED_EXCERPT = "BBBBBBBBBB";

    private static final String DEFAULT_CONTENT = "AAAAAAAAAA";
    private static final String UPDATED_CONTENT = "BBBBBBBBBB";

    private static final String DEFAULT_STATUS = "AAAAAAAAAA";
    private static final String UPDATED_STATUS = "BBBBBBBBBB";

    private static final String DEFAULT_FEATURED_IMAGE_URL = "AAAAAAAAAA";
    private static final String UPDATED_FEATURED_IMAGE_URL = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.ofEpochMilli(1790760789812L);

    private static final Instant DEFAULT_UPDATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATED_AT = Instant.ofEpochMilli(1790760789812L);

    private static final String ENTITY_API_URL = "/api/articles";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/articles/_search";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    @Mock
    private ArticleRepository articleRepositoryMock;

    @Autowired
    private ArticleMapper articleMapper;

    @Mock
    private ArticleService articleServiceMock;

    @Autowired
    private ArticleSearchRepository articleSearchRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restArticleMockMvc;

    private Article article;

    private Article insertedArticle;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Article createEntity() {
        return new Article()
            .title(DEFAULT_TITLE)
            .slug(DEFAULT_SLUG)
            .excerpt(DEFAULT_EXCERPT)
            .content(DEFAULT_CONTENT)
            .status(DEFAULT_STATUS)
            .featuredImageUrl(DEFAULT_FEATURED_IMAGE_URL)
            .createdAt(DEFAULT_CREATED_AT)
            .updatedAt(DEFAULT_UPDATED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Article createUpdatedEntity() {
        return new Article()
            .title(UPDATED_TITLE)
            .slug(UPDATED_SLUG)
            .excerpt(UPDATED_EXCERPT)
            .content(UPDATED_CONTENT)
            .status(UPDATED_STATUS)
            .featuredImageUrl(UPDATED_FEATURED_IMAGE_URL)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
    }

    @BeforeEach
    void initTest() {
        article = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedArticle != null) {
            articleRepository.delete(insertedArticle);
            articleSearchRepository.delete(insertedArticle);
            insertedArticle = null;
        }
    }

    @Test
    @Transactional
    void createArticle() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleSearchRepository.findAll());
        // Create the Article
        ArticleDTO articleDTO = articleMapper.toDto(article);
        var returnedArticleDTO = om.readValue(
            restArticleMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(articleDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ArticleDTO.class
        );

        // Validate the Article in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedArticle = articleMapper.toEntity(returnedArticleDTO);
        assertArticleUpdatableFieldsEquals(returnedArticle, getPersistedArticle(returnedArticle));

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore + 1);
            });

        insertedArticle = returnedArticle;
    }

    @Test
    @Transactional
    void createArticleWithExistingId() throws Exception {
        // Create the Article with an existing ID
        article.setId(1L);
        ArticleDTO articleDTO = articleMapper.toDto(article);

        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleSearchRepository.findAll());

        // An entity with an existing ID cannot be created, so this API call must fail
        restArticleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(articleDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Article in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkTitleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleSearchRepository.findAll());
        // set the field null
        article.setTitle(null);

        // Create the Article, which fails.
        ArticleDTO articleDTO = articleMapper.toDto(article);

        restArticleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(articleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void checkSlugIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleSearchRepository.findAll());
        // set the field null
        article.setSlug(null);

        // Create the Article, which fails.
        ArticleDTO articleDTO = articleMapper.toDto(article);

        restArticleMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(articleDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void getAllArticles() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        // Get all the articleList
        restArticleMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(article.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].excerpt").value(hasItem(DEFAULT_EXCERPT)))
            .andExpect(jsonPath("$.[*].content").value(hasItem(DEFAULT_CONTENT)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS)))
            .andExpect(jsonPath("$.[*].featuredImageUrl").value(hasItem(DEFAULT_FEATURED_IMAGE_URL)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllArticlesWithEagerRelationshipsIsEnabled() throws Exception {
        when(articleServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restArticleMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(articleServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllArticlesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(articleServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restArticleMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(articleRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getArticle() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        // Get the article
        restArticleMockMvc
            .perform(get(ENTITY_API_URL_ID, article.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(article.getId().intValue()))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE))
            .andExpect(jsonPath("$.slug").value(DEFAULT_SLUG))
            .andExpect(jsonPath("$.excerpt").value(DEFAULT_EXCERPT))
            .andExpect(jsonPath("$.content").value(DEFAULT_CONTENT))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS))
            .andExpect(jsonPath("$.featuredImageUrl").value(DEFAULT_FEATURED_IMAGE_URL))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.updatedAt").value(DEFAULT_UPDATED_AT.toString()));
    }

    @Test
    @Transactional
    void getArticlesByIdFiltering() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        Long id = article.getId();

        defaultArticleFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultArticleFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultArticleFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllArticlesByTitleIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        // Get all the articleList where title equals to
        defaultArticleFiltering("title.equals=" + DEFAULT_TITLE, "title.equals=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllArticlesByTitleIsInShouldWork() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        // Get all the articleList where title in
        defaultArticleFiltering("title.in=" + DEFAULT_TITLE + "," + UPDATED_TITLE, "title.in=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllArticlesByTitleIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        // Get all the articleList where title is not null
        defaultArticleFiltering("title.specified=true", "title.specified=false");
    }

    @Test
    @Transactional
    void getAllArticlesByTitleContainsSomething() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        // Get all the articleList where title contains
        defaultArticleFiltering("title.contains=" + DEFAULT_TITLE, "title.contains=" + UPDATED_TITLE);
    }

    @Test
    @Transactional
    void getAllArticlesByTitleNotContainsSomething() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        // Get all the articleList where title does not contain
        defaultArticleFiltering("title.doesNotContain=" + UPDATED_TITLE, "title.doesNotContain=" + DEFAULT_TITLE);
    }

    @Test
    @Transactional
    void getAllArticlesBySlugIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        // Get all the articleList where slug equals to
        defaultArticleFiltering("slug.equals=" + DEFAULT_SLUG, "slug.equals=" + UPDATED_SLUG);
    }

    @Test
    @Transactional
    void getAllArticlesBySlugIsInShouldWork() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        // Get all the articleList where slug in
        defaultArticleFiltering("slug.in=" + DEFAULT_SLUG + "," + UPDATED_SLUG, "slug.in=" + UPDATED_SLUG);
    }

    @Test
    @Transactional
    void getAllArticlesBySlugIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        // Get all the articleList where slug is not null
        defaultArticleFiltering("slug.specified=true", "slug.specified=false");
    }

    @Test
    @Transactional
    void getAllArticlesBySlugContainsSomething() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        // Get all the articleList where slug contains
        defaultArticleFiltering("slug.contains=" + DEFAULT_SLUG, "slug.contains=" + UPDATED_SLUG);
    }

    @Test
    @Transactional
    void getAllArticlesBySlugNotContainsSomething() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        // Get all the articleList where slug does not contain
        defaultArticleFiltering("slug.doesNotContain=" + UPDATED_SLUG, "slug.doesNotContain=" + DEFAULT_SLUG);
    }

    @Test
    @Transactional
    void getAllArticlesByStatusIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        // Get all the articleList where status equals to
        defaultArticleFiltering("status.equals=" + DEFAULT_STATUS, "status.equals=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllArticlesByStatusIsInShouldWork() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        // Get all the articleList where status in
        defaultArticleFiltering("status.in=" + DEFAULT_STATUS + "," + UPDATED_STATUS, "status.in=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllArticlesByStatusIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        // Get all the articleList where status is not null
        defaultArticleFiltering("status.specified=true", "status.specified=false");
    }

    @Test
    @Transactional
    void getAllArticlesByStatusContainsSomething() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        // Get all the articleList where status contains
        defaultArticleFiltering("status.contains=" + DEFAULT_STATUS, "status.contains=" + UPDATED_STATUS);
    }

    @Test
    @Transactional
    void getAllArticlesByStatusNotContainsSomething() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        // Get all the articleList where status does not contain
        defaultArticleFiltering("status.doesNotContain=" + UPDATED_STATUS, "status.doesNotContain=" + DEFAULT_STATUS);
    }

    @Test
    @Transactional
    void getAllArticlesByCreatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        // Get all the articleList where createdAt equals to
        defaultArticleFiltering("createdAt.equals=" + DEFAULT_CREATED_AT, "createdAt.equals=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllArticlesByCreatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        // Get all the articleList where createdAt in
        defaultArticleFiltering("createdAt.in=" + DEFAULT_CREATED_AT + "," + UPDATED_CREATED_AT, "createdAt.in=" + UPDATED_CREATED_AT);
    }

    @Test
    @Transactional
    void getAllArticlesByCreatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        // Get all the articleList where createdAt is not null
        defaultArticleFiltering("createdAt.specified=true", "createdAt.specified=false");
    }

    @Test
    @Transactional
    void getAllArticlesByUpdatedAtIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        // Get all the articleList where updatedAt equals to
        defaultArticleFiltering("updatedAt.equals=" + DEFAULT_UPDATED_AT, "updatedAt.equals=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllArticlesByUpdatedAtIsInShouldWork() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        // Get all the articleList where updatedAt in
        defaultArticleFiltering("updatedAt.in=" + DEFAULT_UPDATED_AT + "," + UPDATED_UPDATED_AT, "updatedAt.in=" + UPDATED_UPDATED_AT);
    }

    @Test
    @Transactional
    void getAllArticlesByUpdatedAtIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        // Get all the articleList where updatedAt is not null
        defaultArticleFiltering("updatedAt.specified=true", "updatedAt.specified=false");
    }

    @Test
    @Transactional
    void getAllArticlesByAuthorIsEqualToSomething() throws Exception {
        User author;
        if (TestUtil.findAll(em, User.class).isEmpty()) {
            articleRepository.saveAndFlush(article);
            author = UserResourceIT.createEntity();
        } else {
            author = TestUtil.findAll(em, User.class).get(0);
        }
        em.persist(author);
        em.flush();
        article.setAuthor(author);
        articleRepository.saveAndFlush(article);
        Long authorId = author.getId();
        // Get all the articleList where author equals to authorId
        defaultArticleShouldBeFound("authorId.equals=" + authorId);

        // Get all the articleList where author equals to (authorId + 1)
        defaultArticleShouldNotBeFound("authorId.equals=" + (authorId + 1));
    }

    @Test
    @Transactional
    void getAllArticlesByCategoriesIsEqualToSomething() throws Exception {
        ArticleCategory categories;
        if (TestUtil.findAll(em, ArticleCategory.class).isEmpty()) {
            articleRepository.saveAndFlush(article);
            categories = ArticleCategoryResourceIT.createEntity();
        } else {
            categories = TestUtil.findAll(em, ArticleCategory.class).get(0);
        }
        em.persist(categories);
        em.flush();
        article.addCategories(categories);
        articleRepository.saveAndFlush(article);
        Long categoriesId = categories.getId();
        // Get all the articleList where categories equals to categoriesId
        defaultArticleShouldBeFound("categoriesId.equals=" + categoriesId);

        // Get all the articleList where categories equals to (categoriesId + 1)
        defaultArticleShouldNotBeFound("categoriesId.equals=" + (categoriesId + 1));
    }

    @Test
    @Transactional
    void getAllArticlesByTagsIsEqualToSomething() throws Exception {
        ArticleTag tags;
        if (TestUtil.findAll(em, ArticleTag.class).isEmpty()) {
            articleRepository.saveAndFlush(article);
            tags = ArticleTagResourceIT.createEntity();
        } else {
            tags = TestUtil.findAll(em, ArticleTag.class).get(0);
        }
        em.persist(tags);
        em.flush();
        article.addTags(tags);
        articleRepository.saveAndFlush(article);
        Long tagsId = tags.getId();
        // Get all the articleList where tags equals to tagsId
        defaultArticleShouldBeFound("tagsId.equals=" + tagsId);

        // Get all the articleList where tags equals to (tagsId + 1)
        defaultArticleShouldNotBeFound("tagsId.equals=" + (tagsId + 1));
    }

    private void defaultArticleFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultArticleShouldBeFound(shouldBeFound);
        defaultArticleShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultArticleShouldBeFound(String filter) throws Exception {
        restArticleMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(article.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].excerpt").value(hasItem(DEFAULT_EXCERPT)))
            .andExpect(jsonPath("$.[*].content").value(hasItem(DEFAULT_CONTENT)))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS)))
            .andExpect(jsonPath("$.[*].featuredImageUrl").value(hasItem(DEFAULT_FEATURED_IMAGE_URL)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));

        // Check, that the count call also returns 1
        restArticleMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultArticleShouldNotBeFound(String filter) throws Exception {
        restArticleMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restArticleMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingArticle() throws Exception {
        // Get the article
        restArticleMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingArticle() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        long databaseSizeBeforeUpdate = getRepositoryCount();
        articleSearchRepository.save(article);
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleSearchRepository.findAll());

        // Update the article
        Article updatedArticle = articleRepository.findById(article.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedArticle are not directly saved in db
        em.detach(updatedArticle);
        updatedArticle
            .title(UPDATED_TITLE)
            .slug(UPDATED_SLUG)
            .excerpt(UPDATED_EXCERPT)
            .content(UPDATED_CONTENT)
            .status(UPDATED_STATUS)
            .featuredImageUrl(UPDATED_FEATURED_IMAGE_URL)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);
        ArticleDTO articleDTO = articleMapper.toDto(updatedArticle);

        restArticleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, articleDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(articleDTO))
            )
            .andExpect(status().isOk());

        // Validate the Article in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedArticleToMatchAllProperties(updatedArticle);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleSearchRepository.findAll());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
                List<Article> articleSearchList = Streamable.of(articleSearchRepository.findAll()).toList();
                Article testArticleSearch = articleSearchList.get(searchDatabaseSizeAfter - 1);

                assertArticleAllPropertiesEquals(testArticleSearch, updatedArticle);
            });
    }

    @Test
    @Transactional
    void putNonExistingArticle() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleSearchRepository.findAll());
        article.setId(longCount.incrementAndGet());

        // Create the Article
        ArticleDTO articleDTO = articleMapper.toDto(article);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restArticleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, articleDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(articleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Article in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithIdMismatchArticle() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleSearchRepository.findAll());
        article.setId(longCount.incrementAndGet());

        // Create the Article
        ArticleDTO articleDTO = articleMapper.toDto(article);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restArticleMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(articleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Article in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamArticle() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleSearchRepository.findAll());
        article.setId(longCount.incrementAndGet());

        // Create the Article
        ArticleDTO articleDTO = articleMapper.toDto(article);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restArticleMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(articleDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Article in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void partialUpdateArticleWithPatch() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the article using partial update
        Article partialUpdatedArticle = new Article();
        partialUpdatedArticle.setId(article.getId());

        partialUpdatedArticle.excerpt(UPDATED_EXCERPT).updatedAt(UPDATED_UPDATED_AT);

        restArticleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedArticle.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedArticle))
            )
            .andExpect(status().isOk());

        // Validate the Article in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertArticleUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedArticle, article), getPersistedArticle(article));
    }

    @Test
    @Transactional
    void fullUpdateArticleWithPatch() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the article using partial update
        Article partialUpdatedArticle = new Article();
        partialUpdatedArticle.setId(article.getId());

        partialUpdatedArticle
            .title(UPDATED_TITLE)
            .slug(UPDATED_SLUG)
            .excerpt(UPDATED_EXCERPT)
            .content(UPDATED_CONTENT)
            .status(UPDATED_STATUS)
            .featuredImageUrl(UPDATED_FEATURED_IMAGE_URL)
            .createdAt(UPDATED_CREATED_AT)
            .updatedAt(UPDATED_UPDATED_AT);

        restArticleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedArticle.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedArticle))
            )
            .andExpect(status().isOk());

        // Validate the Article in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertArticleUpdatableFieldsEquals(partialUpdatedArticle, getPersistedArticle(partialUpdatedArticle));
    }

    @Test
    @Transactional
    void patchNonExistingArticle() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleSearchRepository.findAll());
        article.setId(longCount.incrementAndGet());

        // Create the Article
        ArticleDTO articleDTO = articleMapper.toDto(article);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restArticleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, articleDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(articleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Article in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithIdMismatchArticle() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleSearchRepository.findAll());
        article.setId(longCount.incrementAndGet());

        // Create the Article
        ArticleDTO articleDTO = articleMapper.toDto(article);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restArticleMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(articleDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Article in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamArticle() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleSearchRepository.findAll());
        article.setId(longCount.incrementAndGet());

        // Create the Article
        ArticleDTO articleDTO = articleMapper.toDto(article);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restArticleMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(articleDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Article in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    @Transactional
    void deleteArticle() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);
        articleRepository.save(article);
        articleSearchRepository.save(article);

        long databaseSizeBeforeDelete = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(articleSearchRepository.findAll());
        assertThat(searchDatabaseSizeBefore).isEqualTo(databaseSizeBeforeDelete);

        // Delete the article
        restArticleMockMvc
            .perform(delete(ENTITY_API_URL_ID, article.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(articleSearchRepository.findAll());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore - 1);
    }

    @Test
    @Transactional
    void searchArticle() throws Exception {
        // Initialize the database
        insertedArticle = articleRepository.saveAndFlush(article);
        articleSearchRepository.save(article);

        // Search the article
        restArticleMockMvc
            .perform(get(ENTITY_SEARCH_API_URL + "?query=id:" + article.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(article.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE)))
            .andExpect(jsonPath("$.[*].slug").value(hasItem(DEFAULT_SLUG)))
            .andExpect(jsonPath("$.[*].excerpt").value(hasItem(DEFAULT_EXCERPT.toString())))
            .andExpect(jsonPath("$.[*].content").value(hasItem(DEFAULT_CONTENT.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS)))
            .andExpect(jsonPath("$.[*].featuredImageUrl").value(hasItem(DEFAULT_FEATURED_IMAGE_URL.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].updatedAt").value(hasItem(DEFAULT_UPDATED_AT.toString())));
    }

    protected long getRepositoryCount() {
        return articleRepository.count();
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

    protected Article getPersistedArticle(Article article) {
        return articleRepository.findById(article.getId()).orElseThrow();
    }

    protected void assertPersistedArticleToMatchAllProperties(Article expectedArticle) {
        assertArticleAllPropertiesEquals(expectedArticle, getPersistedArticle(expectedArticle));
    }

    protected void assertPersistedArticleToMatchUpdatableProperties(Article expectedArticle) {
        assertArticleAllUpdatablePropertiesEquals(expectedArticle, getPersistedArticle(expectedArticle));
    }
}
