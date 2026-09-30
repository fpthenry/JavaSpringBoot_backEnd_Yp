package com.mycompany.myapp.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class ListingTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static Listing getListingSample1() {
        return new Listing()
            .id(1L)
            .title("title1")
            .slug("slug1")
            .status("status1")
            .email("email1")
            .telephone("telephone1")
            .mobile("mobile1")
            .website("website1")
            .fax("fax1")
            .taxCode("taxCode1")
            .nameAlias("nameAlias1")
            .nameEn("nameEn1")
            .representative("representative1")
            .mainIndustry("mainIndustry1")
            .managedBy("managedBy1")
            .businessType("businessType1")
            .statusYp("statusYp1")
            .apiId(1L);
    }

    public static Listing getListingSample2() {
        return new Listing()
            .id(2L)
            .title("title2")
            .slug("slug2")
            .status("status2")
            .email("email2")
            .telephone("telephone2")
            .mobile("mobile2")
            .website("website2")
            .fax("fax2")
            .taxCode("taxCode2")
            .nameAlias("nameAlias2")
            .nameEn("nameEn2")
            .representative("representative2")
            .mainIndustry("mainIndustry2")
            .managedBy("managedBy2")
            .businessType("businessType2")
            .statusYp("statusYp2")
            .apiId(2L);
    }

    public static Listing getListingRandomSampleGenerator() {
        return new Listing()
            .id(longCount.incrementAndGet())
            .title(UUID.randomUUID().toString())
            .slug(UUID.randomUUID().toString())
            .status(UUID.randomUUID().toString())
            .email(UUID.randomUUID().toString())
            .telephone(UUID.randomUUID().toString())
            .mobile(UUID.randomUUID().toString())
            .website(UUID.randomUUID().toString())
            .fax(UUID.randomUUID().toString())
            .taxCode(UUID.randomUUID().toString())
            .nameAlias(UUID.randomUUID().toString())
            .nameEn(UUID.randomUUID().toString())
            .representative(UUID.randomUUID().toString())
            .mainIndustry(UUID.randomUUID().toString())
            .managedBy(UUID.randomUUID().toString())
            .businessType(UUID.randomUUID().toString())
            .statusYp(UUID.randomUUID().toString())
            .apiId(longCount.incrementAndGet());
    }
}
