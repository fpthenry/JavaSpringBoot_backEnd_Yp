package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ListingImageTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static ListingImage getListingImageSample1() {
        return new ListingImage().id(1L).altText("altText1").displayOrder(1);
    }

    public static ListingImage getListingImageSample2() {
        return new ListingImage().id(2L).altText("altText2").displayOrder(2);
    }

    public static ListingImage getListingImageRandomSampleGenerator() {
        return new ListingImage()
            .id(longCount.incrementAndGet())
            .altText(UUID.randomUUID().toString())
            .displayOrder(intCount.incrementAndGet());
    }
}
