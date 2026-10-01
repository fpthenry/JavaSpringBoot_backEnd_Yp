package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class LocationTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static Location getLocationSample1() {
        return new Location().id(1L).wpTermId(1L).name("name1").slug("slug1").type("type1").code("code1");
    }

    public static Location getLocationSample2() {
        return new Location().id(2L).wpTermId(2L).name("name2").slug("slug2").type("type2").code("code2");
    }

    public static Location getLocationRandomSampleGenerator() {
        return new Location()
            .id(longCount.incrementAndGet())
            .wpTermId(longCount.incrementAndGet())
            .name(UUID.randomUUID().toString())
            .slug(UUID.randomUUID().toString())
            .type(UUID.randomUUID().toString())
            .code(UUID.randomUUID().toString());
    }
}
