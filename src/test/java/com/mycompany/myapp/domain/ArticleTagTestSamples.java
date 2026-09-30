package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ArticleTagTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static ArticleTag getArticleTagSample1() {
        return new ArticleTag().id(1L).name("name1").slug("slug1").count(1);
    }

    public static ArticleTag getArticleTagSample2() {
        return new ArticleTag().id(2L).name("name2").slug("slug2").count(2);
    }

    public static ArticleTag getArticleTagRandomSampleGenerator() {
        return new ArticleTag()
            .id(longCount.incrementAndGet())
            .name(UUID.randomUUID().toString())
            .slug(UUID.randomUUID().toString())
            .count(intCount.incrementAndGet());
    }
}
