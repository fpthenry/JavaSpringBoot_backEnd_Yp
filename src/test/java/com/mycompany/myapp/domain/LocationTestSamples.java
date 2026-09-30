package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class LocationTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static Location getLocationSample1() {
        return new Location()
            .id(1L)
            .name("name1")
            .slug("slug1")
            .type("type1")
            .provinceCode("provinceCode1")
            .districtCode("districtCode1")
            .wardCode("wardCode1")
            .count(1)
            .displayOrder(1);
    }

    public static Location getLocationSample2() {
        return new Location()
            .id(2L)
            .name("name2")
            .slug("slug2")
            .type("type2")
            .provinceCode("provinceCode2")
            .districtCode("districtCode2")
            .wardCode("wardCode2")
            .count(2)
            .displayOrder(2);
    }

    public static Location getLocationRandomSampleGenerator() {
        return new Location()
            .id(longCount.incrementAndGet())
            .name(UUID.randomUUID().toString())
            .slug(UUID.randomUUID().toString())
            .type(UUID.randomUUID().toString())
            .provinceCode(UUID.randomUUID().toString())
            .districtCode(UUID.randomUUID().toString())
            .wardCode(UUID.randomUUID().toString())
            .count(intCount.incrementAndGet())
            .displayOrder(intCount.incrementAndGet());
    }
}
