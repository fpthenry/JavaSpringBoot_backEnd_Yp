package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CategoryTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static Category getCategorySample1() {
        return new Category().id(1L).code("code1").level(1).name("name1").slug("slug1").listingCount(1);
    }

    public static Category getCategorySample2() {
        return new Category().id(2L).code("code2").level(2).name("name2").slug("slug2").listingCount(2);
    }

    public static Category getCategoryRandomSampleGenerator() {
        return new Category()
            .id(longCount.incrementAndGet())
            .code(UUID.randomUUID().toString())
            .level(intCount.incrementAndGet())
            .name(UUID.randomUUID().toString())
            .slug(UUID.randomUUID().toString())
            .listingCount(intCount.incrementAndGet());
    }
}
