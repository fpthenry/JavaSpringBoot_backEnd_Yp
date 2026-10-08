package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.BlogPost;
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
}
