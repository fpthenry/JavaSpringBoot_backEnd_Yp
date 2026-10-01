package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class TagTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static Tag getTagSample1() {
        return new Tag().id(1L).wpTermId(1L).name("name1").slug("slug1");
    }

    public static Tag getTagSample2() {
        return new Tag().id(2L).wpTermId(2L).name("name2").slug("slug2");
    }

    public static Tag getTagRandomSampleGenerator() {
        return new Tag()
            .id(longCount.incrementAndGet())
            .wpTermId(longCount.incrementAndGet())
            .name(UUID.randomUUID().toString())
            .slug(UUID.randomUUID().toString());
    }
}
