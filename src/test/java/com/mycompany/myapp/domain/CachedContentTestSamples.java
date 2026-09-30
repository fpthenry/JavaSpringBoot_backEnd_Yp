package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class CachedContentTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static CachedContent getCachedContentSample1() {
        return new CachedContent().id(1L).termSlug("termSlug1");
    }

    public static CachedContent getCachedContentSample2() {
        return new CachedContent().id(2L).termSlug("termSlug2");
    }

    public static CachedContent getCachedContentRandomSampleGenerator() {
        return new CachedContent().id(longCount.incrementAndGet()).termSlug(UUID.randomUUID().toString());
    }
}
