package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.BlogPost;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

/**
 * Utility repository to load bag relationships based on https://vladmihalcea.com/hibernate-multiplebagfetchexception/
 */
public class BlogPostRepositoryWithBagRelationshipsImpl implements BlogPostRepositoryWithBagRelationships {

    private static final String ID_PARAMETER = "id";
    private static final String BLOGPOSTS_PARAMETER = "blogPosts";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<BlogPost> fetchBagRelationships(Optional<BlogPost> blogPost) {
        return blogPost.map(this::fetchCategories).map(this::fetchTags);
    }

    @Override
    public Page<BlogPost> fetchBagRelationships(Page<BlogPost> blogPosts) {
        return new PageImpl<>(fetchBagRelationships(blogPosts.getContent()), blogPosts.getPageable(), blogPosts.getTotalElements());
    }

    @Override
    public List<BlogPost> fetchBagRelationships(List<BlogPost> blogPosts) {
        return Optional.of(blogPosts).map(this::fetchCategories).map(this::fetchTags).orElse(List.of());
    }

    BlogPost fetchCategories(BlogPost result) {
        return entityManager
            .createQuery(
                "select blogPost from BlogPost blogPost left join fetch blogPost.categories where blogPost.id = :id",
                BlogPost.class
            )
            .setParameter(ID_PARAMETER, result.getId())
            .getSingleResult();
    }

    List<BlogPost> fetchCategories(List<BlogPost> blogPosts) {
        HashMap<Object, Integer> order = new HashMap<>();
        IntStream.range(0, blogPosts.size()).forEach(index -> order.put(blogPosts.get(index).getId(), index));
        List<BlogPost> result = entityManager
            .createQuery(
                "select blogPost from BlogPost blogPost left join fetch blogPost.categories where blogPost in :blogPosts",
                BlogPost.class
            )
            .setParameter(BLOGPOSTS_PARAMETER, blogPosts)
            .getResultList();
        result.sort((o1, o2) -> Integer.compare(order.get(o1.getId()), order.get(o2.getId())));
        return result;
    }

    BlogPost fetchTags(BlogPost result) {
        return entityManager
            .createQuery("select blogPost from BlogPost blogPost left join fetch blogPost.tags where blogPost.id = :id", BlogPost.class)
            .setParameter(ID_PARAMETER, result.getId())
            .getSingleResult();
    }

    List<BlogPost> fetchTags(List<BlogPost> blogPosts) {
        HashMap<Object, Integer> order = new HashMap<>();
        IntStream.range(0, blogPosts.size()).forEach(index -> order.put(blogPosts.get(index).getId(), index));
        List<BlogPost> result = entityManager
            .createQuery(
                "select blogPost from BlogPost blogPost left join fetch blogPost.tags where blogPost in :blogPosts",
                BlogPost.class
            )
            .setParameter(BLOGPOSTS_PARAMETER, blogPosts)
            .getResultList();
        result.sort((o1, o2) -> Integer.compare(order.get(o1.getId()), order.get(o2.getId())));
        return result;
    }
}
