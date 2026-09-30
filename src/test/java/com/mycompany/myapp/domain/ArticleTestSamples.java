package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class ArticleTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static Article getArticleSample1() {
        return new Article().id(1L).title("title1").slug("slug1").status("status1");
    }

    public static Article getArticleSample2() {
        return new Article().id(2L).title("title2").slug("slug2").status("status2");
    }

    public static Article getArticleRandomSampleGenerator() {
        return new Article()
            .id(longCount.incrementAndGet())
            .title(UUID.randomUUID().toString())
            .slug(UUID.randomUUID().toString())
            .status(UUID.randomUUID().toString());
    }
}
