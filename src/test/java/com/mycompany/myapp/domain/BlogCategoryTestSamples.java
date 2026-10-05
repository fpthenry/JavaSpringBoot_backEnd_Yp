package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class BlogCategoryTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static BlogCategory getBlogCategorySample1() {
        return new BlogCategory().id(1L).wpTermId(1L).name("name1").slug("slug1").postCount(1);
    }

    public static BlogCategory getBlogCategorySample2() {
        return new BlogCategory().id(2L).wpTermId(2L).name("name2").slug("slug2").postCount(2);
    }

    public static BlogCategory getBlogCategoryRandomSampleGenerator() {
        return new BlogCategory()
            .id(longCount.incrementAndGet())
            .wpTermId(longCount.incrementAndGet())
            .name(UUID.randomUUID().toString())
            .slug(UUID.randomUUID().toString())
            .postCount(intCount.incrementAndGet());
    }
}
