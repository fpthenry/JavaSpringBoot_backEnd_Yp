package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.BlogPost;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface BlogPostRepositoryWithBagRelationships {
    Optional<BlogPost> fetchBagRelationships(Optional<BlogPost> blogPost);

    List<BlogPost> fetchBagRelationships(List<BlogPost> blogPosts);

    Page<BlogPost> fetchBagRelationships(Page<BlogPost> blogPosts);
}
