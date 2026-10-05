package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class GalleryImageTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static GalleryImage getGalleryImageSample1() {
        return new GalleryImage().id(1L).title("title1").imageUrl("imageUrl1").linkUrl("linkUrl1").altText("altText1").displayOrder(1);
    }

    public static GalleryImage getGalleryImageSample2() {
        return new GalleryImage().id(2L).title("title2").imageUrl("imageUrl2").linkUrl("linkUrl2").altText("altText2").displayOrder(2);
    }

    public static GalleryImage getGalleryImageRandomSampleGenerator() {
        return new GalleryImage()
            .id(longCount.incrementAndGet())
            .title(UUID.randomUUID().toString())
            .imageUrl(UUID.randomUUID().toString())
            .linkUrl(UUID.randomUUID().toString())
            .altText(UUID.randomUUID().toString())
            .displayOrder(intCount.incrementAndGet());
    }
}
