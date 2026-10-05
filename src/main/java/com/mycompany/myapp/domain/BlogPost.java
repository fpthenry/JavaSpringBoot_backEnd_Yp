package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Bài viết tin tức (bảng blog_post)
 */
@Entity
@Table(name = "blog_post")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "blogpost")
// Sửa tay: analyzer tiếng Việt (không dấu, bỏ HTML). Đổi file này thì phải reindex blogpost.
@org.springframework.data.elasticsearch.annotations.Setting(settingPath = "config/elasticsearch/blogpost-settings.json")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class BlogPost implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Long)
    private Long id;

    /**
     * wp_posts.ID
     */
    // Sửa tay: bài soạn mới trên trang quản trị không có id WordPress, nên wpId không bắt buộc
    @Column(name = "wp_id", unique = true)
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Long)
    private Long wpId;

    @NotNull
    @Size(max = 500)
    @Column(name = "title", length = 500, nullable = false)
    // Sửa tay: title tìm không dấu; title.exact giữ dấu để bài đúng dấu xếp trước; title.keyword để sắp xếp
    @org.springframework.data.elasticsearch.annotations.MultiField(
        mainField = @org.springframework.data.elasticsearch.annotations.Field(
            type = org.springframework.data.elasticsearch.annotations.FieldType.Text,
            analyzer = "vi_folding"
        ),
        otherFields = {
            @org.springframework.data.elasticsearch.annotations.InnerField(
                suffix = "exact",
                type = org.springframework.data.elasticsearch.annotations.FieldType.Text,
                analyzer = "vi_exact"
            ),
            @org.springframework.data.elasticsearch.annotations.InnerField(
                suffix = "keyword",
                type = org.springframework.data.elasticsearch.annotations.FieldType.Keyword,
                ignoreAbove = 256
            ),
        }
    )
    private String title;

    @Size(max = 500)
    @Column(name = "slug", length = 500)
    @org.springframework.data.elasticsearch.annotations.MultiField(
        mainField = @org.springframework.data.elasticsearch.annotations.Field(
            type = org.springframework.data.elasticsearch.annotations.FieldType.Text
        ),
        otherFields = {
            @org.springframework.data.elasticsearch.annotations.InnerField(
                suffix = "keyword",
                type = org.springframework.data.elasticsearch.annotations.FieldType.Keyword,
                ignoreAbove = 256
            ),
        }
    )
    private String slug;

    @Lob
    @Column(name = "content")
    // Sửa tay: nội dung là HTML, bỏ thẻ khi index (html_strip) rồi tìm không dấu; .exact giữ dấu
    @org.springframework.data.elasticsearch.annotations.MultiField(
        mainField = @org.springframework.data.elasticsearch.annotations.Field(
            type = org.springframework.data.elasticsearch.annotations.FieldType.Text,
            analyzer = "vi_html_folding",
            searchAnalyzer = "vi_folding"
        ),
        otherFields = {
            @org.springframework.data.elasticsearch.annotations.InnerField(
                suffix = "exact",
                type = org.springframework.data.elasticsearch.annotations.FieldType.Text,
                analyzer = "vi_html_exact",
                searchAnalyzer = "vi_exact"
            ),
        }
    )
    private String content;

    @Lob
    @Column(name = "excerpt")
    // Sửa tay: nội dung là HTML, bỏ thẻ khi index (html_strip) rồi tìm không dấu; .exact giữ dấu
    @org.springframework.data.elasticsearch.annotations.MultiField(
        mainField = @org.springframework.data.elasticsearch.annotations.Field(
            type = org.springframework.data.elasticsearch.annotations.FieldType.Text,
            analyzer = "vi_html_folding",
            searchAnalyzer = "vi_folding"
        ),
        otherFields = {
            @org.springframework.data.elasticsearch.annotations.InnerField(
                suffix = "exact",
                type = org.springframework.data.elasticsearch.annotations.FieldType.Text,
                analyzer = "vi_html_exact",
                searchAnalyzer = "vi_exact"
            ),
        }
    )
    private String excerpt;

    /**
     * URL ảnh đại diện (featured image)
     */
    @Size(max = 500)
    @Column(name = "thumbnail", length = 500)
    @org.springframework.data.elasticsearch.annotations.MultiField(
        mainField = @org.springframework.data.elasticsearch.annotations.Field(
            type = org.springframework.data.elasticsearch.annotations.FieldType.Text
        ),
        otherFields = {
            @org.springframework.data.elasticsearch.annotations.InnerField(
                suffix = "keyword",
                type = org.springframework.data.elasticsearch.annotations.FieldType.Keyword,
                ignoreAbove = 256
            ),
        }
    )
    private String thumbnail;

    @Size(max = 50)
    @Column(name = "status", length = 50)
    @org.springframework.data.elasticsearch.annotations.MultiField(
        mainField = @org.springframework.data.elasticsearch.annotations.Field(
            type = org.springframework.data.elasticsearch.annotations.FieldType.Text
        ),
        otherFields = {
            @org.springframework.data.elasticsearch.annotations.InnerField(
                suffix = "keyword",
                type = org.springframework.data.elasticsearch.annotations.FieldType.Keyword,
                ignoreAbove = 256
            ),
        }
    )
    private String status;

    @Column(name = "view_count")
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Integer)
    private Integer viewCount;

    @Column(name = "published_at")
    @org.springframework.data.elasticsearch.annotations.Field(
        type = org.springframework.data.elasticsearch.annotations.FieldType.Date,
        format = org.springframework.data.elasticsearch.annotations.DateFormat.date_time
    )
    private Instant publishedAt;

    @Column(name = "created_at")
    @org.springframework.data.elasticsearch.annotations.Field(
        type = org.springframework.data.elasticsearch.annotations.FieldType.Date,
        format = org.springframework.data.elasticsearch.annotations.DateFormat.date_time
    )
    private Instant createdAt;

    @Column(name = "updated_at")
    @org.springframework.data.elasticsearch.annotations.Field(
        type = org.springframework.data.elasticsearch.annotations.FieldType.Date,
        format = org.springframework.data.elasticsearch.annotations.DateFormat.date_time
    )
    private Instant updatedAt;

    /**
     * Tên tác giả trên WordPress, vd: ADMIN HCM
     */
    @Size(max = 255)
    @Column(name = "author_name", length = 255)
    @org.springframework.data.elasticsearch.annotations.MultiField(
        mainField = @org.springframework.data.elasticsearch.annotations.Field(
            type = org.springframework.data.elasticsearch.annotations.FieldType.Text
        ),
        otherFields = {
            @org.springframework.data.elasticsearch.annotations.InnerField(
                suffix = "keyword",
                type = org.springframework.data.elasticsearch.annotations.FieldType.Keyword,
                ignoreAbove = 256
            ),
        }
    )
    private String authorName;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rel_blog_post__category",
        joinColumns = @JoinColumn(name = "blog_post_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @JsonIgnoreProperties(value = { "parent", "blogPosts" }, allowSetters = true)
    // Sửa tay: không ghi quan hệ vào Elasticsearch (tránh lazy-load khi reindex)
    @org.springframework.data.annotation.Transient
    private Set<BlogCategory> categories = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rel_blog_post__tag",
        joinColumns = @JoinColumn(name = "blog_post_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @JsonIgnoreProperties(value = { "blogPosts" }, allowSetters = true)
    // Sửa tay: không ghi quan hệ vào Elasticsearch (tránh lazy-load khi reindex)
    @org.springframework.data.annotation.Transient
    private Set<Tag> tags = new HashSet<>();

    // Sửa tay: các field dưới chỉ có trong Elasticsearch (không phải cột DB), do BlogPostSearchFields điền trước khi index.
    // categoryIds gồm cả id danh mục cha, để lọc theo một nhánh cây danh mục.
    @jakarta.persistence.Transient
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Long)
    private Set<Long> categoryIds = new HashSet<>();

    @jakarta.persistence.Transient
    @org.springframework.data.elasticsearch.annotations.Field(
        type = org.springframework.data.elasticsearch.annotations.FieldType.Text,
        analyzer = "vi_folding"
    )
    private Set<String> categoryNames = new HashSet<>();

    @jakarta.persistence.Transient
    @org.springframework.data.elasticsearch.annotations.Field(type = org.springframework.data.elasticsearch.annotations.FieldType.Long)
    private Set<Long> tagIds = new HashSet<>();

    @jakarta.persistence.Transient
    @org.springframework.data.elasticsearch.annotations.Field(
        type = org.springframework.data.elasticsearch.annotations.FieldType.Text,
        analyzer = "vi_folding"
    )
    private Set<String> tagNames = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public BlogPost id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWpId() {
        return this.wpId;
    }

    public BlogPost wpId(Long wpId) {
        this.setWpId(wpId);
        return this;
    }

    public void setWpId(Long wpId) {
        this.wpId = wpId;
    }

    public String getTitle() {
        return this.title;
    }

    public BlogPost title(String title) {
        this.setTitle(title);
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return this.slug;
    }

    public BlogPost slug(String slug) {
        this.setSlug(slug);
        return this;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getContent() {
        return this.content;
    }

    public BlogPost content(String content) {
        this.setContent(content);
        return this;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getExcerpt() {
        return this.excerpt;
    }

    public BlogPost excerpt(String excerpt) {
        this.setExcerpt(excerpt);
        return this;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public String getThumbnail() {
        return this.thumbnail;
    }

    public BlogPost thumbnail(String thumbnail) {
        this.setThumbnail(thumbnail);
        return this;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getStatus() {
        return this.status;
    }

    public BlogPost status(String status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getViewCount() {
        return this.viewCount;
    }

    public BlogPost viewCount(Integer viewCount) {
        this.setViewCount(viewCount);
        return this;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Instant getPublishedAt() {
        return this.publishedAt;
    }

    public BlogPost publishedAt(Instant publishedAt) {
        this.setPublishedAt(publishedAt);
        return this;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public BlogPost createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public BlogPost updatedAt(Instant updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAuthorName() {
        return this.authorName;
    }

    public BlogPost authorName(String authorName) {
        this.setAuthorName(authorName);
        return this;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Set<BlogCategory> getCategories() {
        return this.categories;
    }

    public void setCategories(Set<BlogCategory> blogCategories) {
        this.categories = blogCategories;
    }

    public BlogPost categories(Set<BlogCategory> blogCategories) {
        this.setCategories(blogCategories);
        return this;
    }

    public BlogPost addCategory(BlogCategory blogCategory) {
        this.categories.add(blogCategory);
        return this;
    }

    public BlogPost removeCategory(BlogCategory blogCategory) {
        this.categories.remove(blogCategory);
        return this;
    }

    public Set<Tag> getTags() {
        return this.tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public BlogPost tags(Set<Tag> tags) {
        this.setTags(tags);
        return this;
    }

    public BlogPost addTag(Tag tag) {
        this.tags.add(tag);
        return this;
    }

    public BlogPost removeTag(Tag tag) {
        this.tags.remove(tag);
        return this;
    }

    public Set<Long> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(Set<Long> categoryIds) {
        this.categoryIds = categoryIds;
    }

    public Set<String> getCategoryNames() {
        return categoryNames;
    }

    public void setCategoryNames(Set<String> categoryNames) {
        this.categoryNames = categoryNames;
    }

    public Set<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(Set<Long> tagIds) {
        this.tagIds = tagIds;
    }

    public Set<String> getTagNames() {
        return tagNames;
    }

    public void setTagNames(Set<String> tagNames) {
        this.tagNames = tagNames;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BlogPost)) {
            return false;
        }
        return getId() != null && getId().equals(((BlogPost) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "BlogPost{" +
            "id=" + getId() +
            ", wpId=" + getWpId() +
            ", title='" + getTitle() + "'" +
            ", slug='" + getSlug() + "'" +
            ", content='" + getContent() + "'" +
            ", excerpt='" + getExcerpt() + "'" +
            ", thumbnail='" + getThumbnail() + "'" +
            ", status='" + getStatus() + "'" +
            ", viewCount=" + getViewCount() +
            ", publishedAt='" + getPublishedAt() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            ", authorName='" + getAuthorName() + "'" +
            "}";
    }
}
