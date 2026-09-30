package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class RedirectRuleTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static RedirectRule getRedirectRuleSample1() {
        return new RedirectRule()
            .id(1L)
            .sourceId(1L)
            .sourceSlug("sourceSlug1")
            .destinationId(1L)
            .destinationSlug("destinationSlug1")
            .objectType("objectType1");
    }

    public static RedirectRule getRedirectRuleSample2() {
        return new RedirectRule()
            .id(2L)
            .sourceId(2L)
            .sourceSlug("sourceSlug2")
            .destinationId(2L)
            .destinationSlug("destinationSlug2")
            .objectType("objectType2");
    }

    public static RedirectRule getRedirectRuleRandomSampleGenerator() {
        return new RedirectRule()
            .id(longCount.incrementAndGet())
            .sourceId(longCount.incrementAndGet())
            .sourceSlug(UUID.randomUUID().toString())
            .destinationId(longCount.incrementAndGet())
            .destinationSlug(UUID.randomUUID().toString())
            .objectType(UUID.randomUUID().toString());
    }
}
