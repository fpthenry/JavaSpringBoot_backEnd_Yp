package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ListingTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static Listing getListingSample1() {
        return new Listing()
            .id(1L)
            .wpId(1L)
            .apiId("apiId1")
            .name("name1")
            .nameEn("nameEn1")
            .nameAlias("nameAlias1")
            .slug("slug1")
            .phone("phone1")
            .mobile("mobile1")
            .email("email1")
            .website("website1")
            .taxCode("taxCode1")
            .representative("representative1")
            .capital("capital1")
            .foundedYear("foundedYear1")
            .businessType("businessType1")
            .businessStatus("businessStatus1")
            .industryCode("industryCode1")
            .managedBy("managedBy1")
            .thumbnail("thumbnail1")
            .viewCount(1)
            .status("status1");
    }

    public static Listing getListingSample2() {
        return new Listing()
            .id(2L)
            .wpId(2L)
            .apiId("apiId2")
            .name("name2")
            .nameEn("nameEn2")
            .nameAlias("nameAlias2")
            .slug("slug2")
            .phone("phone2")
            .mobile("mobile2")
            .email("email2")
            .website("website2")
            .taxCode("taxCode2")
            .representative("representative2")
            .capital("capital2")
            .foundedYear("foundedYear2")
            .businessType("businessType2")
            .businessStatus("businessStatus2")
            .industryCode("industryCode2")
            .managedBy("managedBy2")
            .thumbnail("thumbnail2")
            .viewCount(2)
            .status("status2");
    }

    public static Listing getListingRandomSampleGenerator() {
        return new Listing()
            .id(longCount.incrementAndGet())
            .wpId(longCount.incrementAndGet())
            .apiId(UUID.randomUUID().toString())
            .name(UUID.randomUUID().toString())
            .nameEn(UUID.randomUUID().toString())
            .nameAlias(UUID.randomUUID().toString())
            .slug(UUID.randomUUID().toString())
            .phone(UUID.randomUUID().toString())
            .mobile(UUID.randomUUID().toString())
            .email(UUID.randomUUID().toString())
            .website(UUID.randomUUID().toString())
            .taxCode(UUID.randomUUID().toString())
            .representative(UUID.randomUUID().toString())
            .capital(UUID.randomUUID().toString())
            .foundedYear(UUID.randomUUID().toString())
            .businessType(UUID.randomUUID().toString())
            .businessStatus(UUID.randomUUID().toString())
            .industryCode(UUID.randomUUID().toString())
            .managedBy(UUID.randomUUID().toString())
            .thumbnail(UUID.randomUUID().toString())
            .viewCount(intCount.incrementAndGet())
            .status(UUID.randomUUID().toString());
    }
}
