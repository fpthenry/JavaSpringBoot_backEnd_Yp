package com.mycompany.myapp.service.listingimport;

import com.mycompany.myapp.domain.Category;
import com.mycompany.myapp.domain.Listing;
import com.mycompany.myapp.domain.Location;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ghi dòng nhập vào listing: tạo doanh nghiệp mới (nháp) hoặc bổ sung ô còn trống của doanh nghiệp đã có
 * (không ghi đè dữ liệu đang có). Gọi trong transaction của lô; đánh index Elasticsearch do người gọi làm sau commit.
 */
@Component
public class ListingImportWriter {

    /** Doanh nghiệp nhập mới chờ duyệt, chưa hiện trên website (API công khai chỉ lấy "publish"). */
    public static final String DRAFT = "draft";

    private final EntityManager em;
    private final JdbcTemplate jdbc;

    public ListingImportWriter(EntityManager em, DataSource dataSource) {
        this.em = em;
        this.jdbc = new JdbcTemplate(dataSource);
    }

    /** Doanh nghiệp mới, trạng thái nháp, đánh dấu nguồn. */
    public Listing create(
        ListingImportMatcher.Normalized n,
        String source,
        Long batchId,
        ImportTermResolver terms,
        Set<String> slugsInBatch,
        List<String> notes
    ) {
        ImportRecord r = n.record();
        Instant now = Instant.now();
        Listing l = new Listing();
        l.setName(ListingImportNormalizer.cut(n.name(), 500));
        l.setTaxCode(n.taxCode());
        l.setSlug(uniqueSlug(n, slugsInBatch));
        l.setStatus(DRAFT);
        l.setIsFeatured(false);
        l.setViewCount(0);
        l.setCreatedAt(now);
        l.setUpdatedAt(now);
        l.setModifiedAt(now);
        l.setEsIndexed(true);
        l.setSource(source);
        l.setSourceRef(n.sourceRef());
        l.setImportBatchId(batchId);
        fillEmpty(l, r, terms, notes); // các field còn lại: cùng quy tắc với bổ sung (doanh nghiệp mới thì mọi ô đều trống)
        em.persist(l);
        return l;
    }

    /**
     * Bổ sung các ô đang trống của {@code l} bằng dữ liệu dòng nhập; ô đã có giữ nguyên.
     * Ngành nghề / tỉnh chỉ gắn khi doanh nghiệp chưa có ngành / địa phương nào.
     *
     * @return tên các field đã điền
     */
    public List<String> fillEmpty(Listing l, ImportRecord r, ImportTermResolver terms, List<String> notes) {
        List<String> filled = new ArrayList<>();
        fill(filled, "taxCode", l::getTaxCode, l::setTaxCode, ListingImportNormalizer.taxCode(r.taxCode()), 100);
        fill(filled, "nameEn", l::getNameEn, l::setNameEn, r.nameEn(), 500);
        fill(filled, "nameAlias", l::getNameAlias, l::setNameAlias, r.nameAlias(), 500);
        fill(filled, "phone", l::getPhone, l::setPhone, r.phone(), 100);
        fill(filled, "mobile", l::getMobile, l::setMobile, r.mobile(), 100);
        fill(filled, "email", l::getEmail, l::setEmail, r.email(), 255);
        fill(filled, "website", l::getWebsite, l::setWebsite, r.website(), 500);
        fill(filled, "address", l::getAddress, l::setAddress, r.address(), 65_000);
        fill(filled, "representative", l::getRepresentative, l::setRepresentative, r.representative(), 255);
        fill(filled, "capital", l::getCapital, l::setCapital, r.capital(), 100);
        fill(filled, "foundedYear", l::getFoundedYear, l::setFoundedYear, r.foundedYear(), 50);
        fill(filled, "businessType", l::getBusinessType, l::setBusinessType, r.businessType(), 100);
        fill(filled, "businessStatus", l::getBusinessStatus, l::setBusinessStatus, r.businessStatus(), 50);
        fill(filled, "industryCode", l::getIndustryCode, l::setIndustryCode, r.industryCode(), 100);
        fill(filled, "description", l::getDescription, l::setDescription, r.description(), 65_000);

        if (l.getCategories().isEmpty()) {
            List<String> unknown = new ArrayList<>();
            for (Long id : terms.categories(r.categories(), r.industryCode(), unknown)) {
                l.addCategory(em.getReference(Category.class, id));
            }
            if (!l.getCategories().isEmpty()) {
                filled.add("categories");
            }
            if (!unknown.isEmpty() && notes != null) {
                notes.add("Không nhận ra ngành: " + String.join(", ", unknown.subList(0, Math.min(5, unknown.size()))));
            }
        }
        if (l.getLocations().isEmpty()) {
            Long province = r.province() != null ? terms.province(r.province()) : terms.provinceFromAddress(r.address());
            if (province != null) {
                l.addLocation(em.getReference(Location.class, province));
                filled.add("locations");
            } else if (r.province() != null && notes != null) {
                notes.add("Không nhận ra tỉnh: " + r.province());
            }
        }
        return filled;
    }

    private static void fill(List<String> filled, String field, Supplier<String> get, Consumer<String> set, String value, int max) {
        String current = get.get();
        String v = ListingImportNormalizer.cut(value, max);
        if ((current == null || current.isBlank()) && v != null) {
            set.accept(v);
            filled.add(field);
        }
    }

    /** Slug từ tên; trùng (trong DB hoặc trong đợt) thì thêm MST / id nguồn / số dòng. */
    private String uniqueSlug(ListingImportMatcher.Normalized n, Set<String> slugsInBatch) {
        String base = ListingImportNormalizer.slug(n.name());
        List<String> tries = new ArrayList<>();
        tries.add(base);
        if (n.taxCode() != null) {
            tries.add(base + "-" + n.taxCode());
        }
        if (n.sourceRef() != null) {
            tries.add(base + "-" + ListingImportNormalizer.slug(n.sourceRef()));
        }
        for (String s : tries) {
            if (!slugsInBatch.contains(s) && !slugExists(s)) {
                slugsInBatch.add(s);
                return s;
            }
        }
        for (int i = 2; ; i++) {
            String s = base + "-" + i;
            if (!slugsInBatch.contains(s) && !slugExists(s)) {
                slugsInBatch.add(s);
                return s;
            }
        }
    }

    private boolean slugExists(String slug) {
        Integer n = jdbc.queryForObject("select count(*) from listing where slug = ?", Integer.class, slug);
        return n != null && n > 0;
    }
}
