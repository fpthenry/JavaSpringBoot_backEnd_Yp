package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class BlogPostTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static BlogPost getBlogPostSample1() {
        return new BlogPost()
            .id(1L)
            .wpId(1L)
            .title("title1")
            .slug("slug1")
            .thumbnail("thumbnail1")
            .status("status1")
            .viewCount(1)
            .authorName("authorName1");
    }

    public static BlogPost getBlogPostSample2() {
        return new BlogPost()
            .id(2L)
            .wpId(2L)
            .title("title2")
            .slug("slug2")
            .thumbnail("thumbnail2")
            .status("status2")
            .viewCount(2)
            .authorName("authorName2");
    }

    public static BlogPost getBlogPostRandomSampleGenerator() {
        return new BlogPost()
            .id(longCount.incrementAndGet())
            .wpId(longCount.incrementAndGet())
            .title(UUID.randomUUID().toString())
            .slug(UUID.randomUUID().toString())
            .thumbnail(UUID.randomUUID().toString())
            .status(UUID.randomUUID().toString())
            .viewCount(intCount.incrementAndGet())
            .authorName(UUID.randomUUID().toString());
    }
}
