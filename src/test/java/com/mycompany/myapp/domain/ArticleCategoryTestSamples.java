package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ArticleCategoryTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static ArticleCategory getArticleCategorySample1() {
        return new ArticleCategory().id(1L).name("name1").slug("slug1").count(1);
    }

    public static ArticleCategory getArticleCategorySample2() {
        return new ArticleCategory().id(2L).name("name2").slug("slug2").count(2);
    }

    public static ArticleCategory getArticleCategoryRandomSampleGenerator() {
        return new ArticleCategory()
            .id(longCount.incrementAndGet())
            .name(UUID.randomUUID().toString())
            .slug(UUID.randomUUID().toString())
            .count(intCount.incrementAndGet());
    }
}
