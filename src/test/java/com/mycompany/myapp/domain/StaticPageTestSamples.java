package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class StaticPageTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static StaticPage getStaticPageSample1() {
        return new StaticPage().id(1L).title("title1").slug("slug1").status("status1").template("template1").menuOrder(1);
    }

    public static StaticPage getStaticPageSample2() {
        return new StaticPage().id(2L).title("title2").slug("slug2").status("status2").template("template2").menuOrder(2);
    }

    public static StaticPage getStaticPageRandomSampleGenerator() {
        return new StaticPage()
            .id(longCount.incrementAndGet())
            .title(UUID.randomUUID().toString())
            .slug(UUID.randomUUID().toString())
            .status(UUID.randomUUID().toString())
            .template(UUID.randomUUID().toString())
            .menuOrder(intCount.incrementAndGet());
    }
}
