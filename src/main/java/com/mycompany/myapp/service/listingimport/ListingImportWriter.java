package com.mycompany.myapp.service.listingimport;

import com.mycompany.myapp.domain.Category;
import com.mycompany.myapp.domain.Listing;
import com.mycompany.myapp.domain.Location;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

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
    private final ObjectMapper objectMapper;
    /** JSON gọn một dòng như dữ liệu đang có (ObjectMapper của profile dev bật INDENT_OUTPUT). */
    private final ObjectWriter compactJson;

    public ListingImportWriter(EntityManager em, DataSource dataSource, ObjectMapper objectMapper) {
        this.em = em;
        this.jdbc = new JdbcTemplate(dataSource);
        this.objectMapper = objectMapper;
        this.compactJson = objectMapper.writer().without(SerializationFeature.INDENT_OUTPUT);
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
     * Ngành nghề / tỉnh chỉ gắn khi doanh nghiệp chưa có ngành / địa phương nào; phường/xã chỉ gắn khi chưa có phường/xã.
     * Logo, ảnh, tọa độ: chỉ khi doanh nghiệp chưa có (không ghi đè, không nối thêm ảnh).
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
        fill(filled, "addressNew", l::getAddressNew, l::setAddressNew, r.addressNew(), 500);
        fill(filled, "managedBy", l::getManagedBy, l::setManagedBy, r.managedBy(), 255);
        String logo = ListingImportNormalizer.httpUrl(r.logo());
        if (r.logo() != null && logo == null && notes != null) {
            notes.add("Logo không phải URL http(s), bỏ qua");
        }
        fill(filled, "logo", l::getThumbnail, l::setThumbnail, logo != null && logo.length() <= 500 ? logo : null, 500);
        if (isBlank(l.getImages())) {
            List<String> urls = ListingImportNormalizer.imageUrls(r.images());
            if (!urls.isEmpty()) {
                l.setImages(imagesJson(urls)); // định dạng gallery WordPress: [{"image_url": …, "caption": ""}]
                filled.add("images");
            }
        }
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
        // ---- địa phương: tỉnh (khi chưa có địa phương nào), phường/xã (khi chưa có phường/xã)
        Set<Long> locationIds = new HashSet<>();
        Long existingProvince = null;
        boolean hasWard = false;
        for (Location loc : l.getLocations()) {
            locationIds.add(loc.getId());
            if ("province".equals(loc.getType()) && (existingProvince == null || isNewUnit(terms, loc.getId()))) {
                existingProvince = loc.getId();
            }
            hasWard |= "ward".equals(loc.getType());
        }
        Long province =
            r.province() != null
                ? terms.province(r.province())
                : terms.provinceFromAddress(r.address() != null ? r.address() : r.addressNew());
        if (r.province() != null && province == null && notes != null) {
            notes.add("Không nhận ra tỉnh: " + r.province());
        }
        if (locationIds.isEmpty() && province != null) {
            attach(l, locationIds, province);
            filled.add("locations");
        }
        ImportTermResolver.WardMatch ward = terms.ward(r.ward(), province != null ? province : existingProvince, r.district());
        if (ward != null && ward.note() != null && notes != null) {
            notes.add(ward.note());
        }
        if (ward != null && ward.ward() != null && !hasWard) {
            attach(l, locationIds, ward.ward().id());
            if (ward.newWard() != null) {
                attach(l, locationIds, ward.newWard().id()); // xã cũ → gắn thêm xã mới tương ứng (như dữ liệu đang có)
                attach(l, locationIds, ward.newWard().province());
            }
            filled.add("ward");
        }

        // ---- location_json: tỉnh, phường/xã, mã, tọa độ — chỉ điền khóa đang trống
        ListingImportNormalizer.Coordinate coordinate = ListingImportNormalizer.coordinate(r.coordinate(), r.latitude(), r.longitude());
        if (coordinate.problem() != null && notes != null) {
            notes.add(coordinate.problem());
        }
        Map<String, String> json = new LinkedHashMap<>();
        ImportTermResolver.Unit wardUnit = ward != null && ward.ward() != null ? ward.display() : null;
        ImportTermResolver.Unit provinceUnit = terms.unit(wardUnit != null ? Long.valueOf(wardUnit.province()) : province);
        if (provinceUnit != null) {
            json.put("province", provinceUnit.name());
            json.put("province_code", provinceUnit.code());
        }
        if (wardUnit != null) {
            json.put("ward", wardUnit.name());
            json.put("ward_type", wardType(wardUnit.name()));
            json.put("ward_code", wardUnit.code());
        }
        if (coordinate.value() != null) {
            json.put("coordinate", coordinate.value()); // nguyên văn "vĩ độ:kinh độ", không làm tròn
        }
        if (mergeLocationJson(l, json)) {
            filled.add("coordinate");
        }
        return filled;
    }

    private void attach(Listing l, Set<Long> locationIds, Long id) {
        if (id != null && locationIds.add(id)) {
            l.addLocation(em.getReference(Location.class, id));
        }
    }

    private static boolean isNewUnit(ImportTermResolver terms, Long id) {
        ImportTermResolver.Unit u = terms.unit(id);
        return u != null && u.isNew();
    }

    /** "Phường Ba Đình" → "Phường"; "Thị trấn Đông Anh" → "Thị trấn" (khóa ward_type của location_json). */
    static String wardType(String name) {
        for (String t : List.of("Phường", "Xã", "Thị trấn", "Đặc khu")) {
            if (name != null && name.startsWith(t + " ")) {
                return t;
            }
        }
        return null;
    }

    /**
     * Ghi các khóa vào listing.location_json (cùng cấu trúc 1,86 triệu doanh nghiệp đang có: province, ward, street,
     * ward_type, province_code, ward_code, coordinate). Khóa đã có giá trị giữ nguyên; JSON cũ hỏng thì không đụng tới.
     *
     * @return true nếu đã ghi khóa coordinate (để báo "đã bổ sung tọa độ")
     */
    private boolean mergeLocationJson(Listing l, Map<String, String> values) {
        if (values.isEmpty()) {
            return false;
        }
        ObjectNode node = null;
        if (!isBlank(l.getLocationJson())) {
            try {
                JsonNode parsed = objectMapper.readTree(l.getLocationJson());
                if (!(parsed instanceof ObjectNode o)) {
                    return false;
                }
                node = o;
            } catch (RuntimeException e) {
                return false;
            }
        }
        if (node == null) {
            node = objectMapper.createObjectNode();
            for (String k : List.of("province", "ward", "street", "ward_type", "province_code", "ward_code", "coordinate")) {
                node.putNull(k);
            }
        }
        boolean changed = false;
        boolean coordinate = false;
        for (Map.Entry<String, String> e : values.entrySet()) {
            JsonNode current = node.get(e.getKey());
            boolean empty = current == null || current.isNull() || (current.isString() && current.stringValue().isBlank());
            if (empty && e.getValue() != null) {
                node.put(e.getKey(), e.getValue());
                changed = true;
                coordinate |= "coordinate".equals(e.getKey());
            }
        }
        if (changed) {
            l.setLocationJson(compactJson.writeValueAsString(node));
        }
        return coordinate;
    }

    private String imagesJson(List<String> urls) {
        ArrayNode arr = objectMapper.createArrayNode();
        for (String u : urls) {
            arr.addObject().put("image_url", u).put("caption", "");
        }
        return compactJson.writeValueAsString(arr);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank() || "[]".equals(s.trim());
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
