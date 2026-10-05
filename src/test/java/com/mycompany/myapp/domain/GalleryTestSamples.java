package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class GalleryTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static Gallery getGallerySample1() {
        return new Gallery().id(1L).name("name1").code("code1").wpId(1L);
    }

    public static Gallery getGallerySample2() {
        return new Gallery().id(2L).name("name2").code("code2").wpId(2L);
    }

    public static Gallery getGalleryRandomSampleGenerator() {
        return new Gallery()
            .id(longCount.incrementAndGet())
            .name(UUID.randomUUID().toString())
            .code(UUID.randomUUID().toString())
            .wpId(longCount.incrementAndGet());
    }
}
