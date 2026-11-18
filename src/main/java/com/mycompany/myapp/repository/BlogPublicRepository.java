package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.BlogPost;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Truy vấn chỉ đọc cho API công khai bài viết (FE Next.js). Chỉ lấy bài đã xuất bản ({@code status = 'publish'}).
 */
public interface BlogPublicRepository extends org.springframework.data.repository.Repository<BlogPost, Long> {
    String PUBLISHED = "publish";

    /** Cặp [id bài, id danh mục] của mọi bài đã xuất bản, để đếm số bài theo cây danh mục (vài nghìn dòng). */
    @Query("select p.id, c.id from BlogPost p join p.categories c where p.status = '" + PUBLISHED + "'")
    List<Object[]> findPublishedPostCategoryPairs();

    /** Bài đã xuất bản theo slug, mới đăng trước (slug có thể trùng: lấy bài đầu tiên). */
    @Query("select p from BlogPost p where p.slug = :slug and p.status = '" + PUBLISHED + "' order by p.publishedAt desc, p.id desc")
    List<BlogPost> findPublishedBySlug(@Param("slug") String slug, Pageable pageable);

    /**
     * Sửa tay (2026-11-17): id bài liên quan của một bài: nhiều thẻ chung trước, rồi nhiều danh mục chung, rồi mới đăng.
     * Bài không chung gì vẫn có (xếp sau, theo ngày đăng) để luôn đủ số bài. {@code tagIds}/{@code categoryIds} không được
     * rỗng (truyền [-1] khi bài không có thẻ/danh mục). Vài nghìn bài nên đếm trực tiếp là đủ nhanh.
     */
    @Query(
        value = "select p.id from blog_post p where p.status = '" +
            PUBLISHED +
            "' and p.id <> :postId order by " +
            "(select count(*) from rel_blog_post__tag t where t.blog_post_id = p.id and t.tag_id in (:tagIds)) desc, " +
            "(select count(*) from rel_blog_post__category c where c.blog_post_id = p.id and c.category_id in (:categoryIds)) desc, " +
            "p.published_at desc, p.id desc limit :size",
        nativeQuery = true
    )
    List<Number> findRelatedIds(
        @Param("postId") Long postId,
        @Param("tagIds") Collection<Long> tagIds,
        @Param("categoryIds") Collection<Long> categoryIds,
        @Param("size") int size
    );
}
