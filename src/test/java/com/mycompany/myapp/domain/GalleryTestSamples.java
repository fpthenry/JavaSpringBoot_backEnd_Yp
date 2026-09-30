package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class GalleryTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static Gallery getGallerySample1() {
        return new Gallery().id(1L).title("title1").slug("slug1").status("status1");
    }

    public static Gallery getGallerySample2() {
        return new Gallery().id(2L).title("title2").slug("slug2").status("status2");
    }

    public static Gallery getGalleryRandomSampleGenerator() {
        return new Gallery()
            .id(longCount.incrementAndGet())
            .title(UUID.randomUUID().toString())
            .slug(UUID.randomUUID().toString())
            .status(UUID.randomUUID().toString());
    }
}
