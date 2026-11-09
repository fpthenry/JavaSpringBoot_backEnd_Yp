package com.mycompany.myapp.service.listingimport;

import com.mycompany.myapp.domain.Category;
import com.mycompany.myapp.domain.Listing;
import com.mycompany.myapp.domain.Location;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Những gì một dòng nhập đã đổi ở doanh nghiệp: từng trường cũ → mới, ngành và địa phương đã gắn thêm, hai mốc thời gian trước khi nhập.
 * Lưu dạng JSON ở {@code listing_import_row.changes}, dùng để xem lại và hoàn tác cả đợt.
 *
 * @param fields trường đã đổi
 * @param categories id ngành đã gắn thêm
 * @param locations id địa phương đã gắn thêm
 * @param modifiedAt {@code modified_at} trước khi nhập
 * @param updatedAt {@code updated_at} trước khi nhập
 */
public record ListingChanges(List<FieldChange> fields, List<Long> categories, List<Long> locations, Instant modifiedAt, Instant updatedAt) {
    /** Tên trường nội bộ, không hiện trên màn hình kết quả: đổi ở đây đi kèm tỉnh, xã, tọa độ. */
    public static final String LOCATION_JSON = "locationJson";

    /** @param updated true nếu ghi đè giá trị đang có, false nếu điền ô trống */
    public record FieldChange(String field, String oldValue, String newValue, boolean updated) {}

    private record Tracked(String name, Function<Listing, String> get, BiConsumer<Listing, String> set) {}

    private static final List<Tracked> TRACKED = List.of(
        new Tracked("name", Listing::getName, Listing::setName),
        new Tracked("nameEn", Listing::getNameEn, Listing::setNameEn),
        new Tracked("nameAlias", Listing::getNameAlias, Listing::setNameAlias),
        new Tracked("taxCode", Listing::getTaxCode, Listing::setTaxCode),
        new Tracked("phone", Listing::getPhone, Listing::setPhone),
        new Tracked("mobile", Listing::getMobile, Listing::setMobile),
        new Tracked("email", Listing::getEmail, Listing::setEmail),
        new Tracked("website", Listing::getWebsite, Listing::setWebsite),
        new Tracked("address", Listing::getAddress, Listing::setAddress),
        new Tracked("addressNew", Listing::getAddressNew, Listing::setAddressNew),
        new Tracked("managedBy", Listing::getManagedBy, Listing::setManagedBy),
        new Tracked("logo", Listing::getThumbnail, Listing::setThumbnail),
        new Tracked("images", Listing::getImages, Listing::setImages),
        new Tracked("representative", Listing::getRepresentative, Listing::setRepresentative),
        new Tracked("capital", Listing::getCapital, Listing::setCapital),
        new Tracked("foundedYear", Listing::getFoundedYear, Listing::setFoundedYear),
        new Tracked("businessType", Listing::getBusinessType, Listing::setBusinessType),
        new Tracked("businessStatus", Listing::getBusinessStatus, Listing::setBusinessStatus),
        new Tracked("industryCode", Listing::getIndustryCode, Listing::setIndustryCode),
        new Tracked("description", Listing::getDescription, Listing::setDescription),
        new Tracked(LOCATION_JSON, Listing::getLocationJson, Listing::setLocationJson)
    );

    /** Trạng thái doanh nghiệp ngay trước khi nhập. */
    public record Snapshot(Map<String, String> values, Set<Long> categories, Set<Long> locations, Instant modifiedAt, Instant updatedAt) {
        /** So với doanh nghiệp sau khi ghi. */
        public ListingChanges diff(Listing after) {
            List<FieldChange> fields = new ArrayList<>();
            for (Tracked t : TRACKED) {
                String before = values.get(t.name());
                String now = t.get().apply(after);
                if (!Objects.equals(before, now)) {
                    fields.add(new FieldChange(t.name(), before, now, before != null && !before.isBlank()));
                }
            }
            List<Long> newCategories = categoryIds(after)
                .stream()
                .filter(id -> !categories.contains(id))
                .sorted()
                .toList();
            List<Long> newLocations = locationIds(after)
                .stream()
                .filter(id -> !locations.contains(id))
                .sorted()
                .toList();
            return new ListingChanges(fields, newCategories, newLocations, modifiedAt, updatedAt);
        }
    }

    public static Snapshot snapshot(Listing l) {
        Map<String, String> values = new LinkedHashMap<>();
        TRACKED.forEach(t -> values.put(t.name(), t.get().apply(l)));
        return new Snapshot(values, categoryIds(l), locationIds(l), l.getModifiedAt(), l.getUpdatedAt());
    }

    /** Tên các trường bị ghi đè (không tính trường nội bộ). */
    public List<String> updatedFields() {
        return fields
            .stream()
            .filter(FieldChange::updated)
            .map(FieldChange::field)
            .filter(f -> !LOCATION_JSON.equals(f))
            .toList();
    }

    /**
     * Trả doanh nghiệp về trước khi nhập. Trường đã đổi tiếp sau đợt này thì giữ nguyên, không đè lên bản mới hơn.
     *
     * @return số trường giữ nguyên
     */
    public int undo(Listing l) {
        int skipped = 0;
        for (FieldChange change : fields) {
            Tracked t = TRACKED.stream()
                .filter(x -> x.name().equals(change.field()))
                .findFirst()
                .orElseThrow();
            if (Objects.equals(t.get().apply(l), change.newValue())) {
                t.set().accept(l, change.oldValue());
            } else {
                skipped++;
            }
        }
        l.getCategories().removeIf(c -> categories.contains(c.getId()));
        l.getLocations().removeIf(x -> locations.contains(x.getId()));
        l.setModifiedAt(modifiedAt);
        l.setUpdatedAt(updatedAt);
        return skipped;
    }

    private static Set<Long> categoryIds(Listing l) {
        return l.getCategories().stream().map(Category::getId).collect(Collectors.toCollection(HashSet::new));
    }

    private static Set<Long> locationIds(Listing l) {
        return l.getLocations().stream().map(Location::getId).collect(Collectors.toCollection(HashSet::new));
    }
}
