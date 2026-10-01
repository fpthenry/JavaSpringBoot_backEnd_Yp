-- =====================================================================
-- Kiểm tra sau đồng bộ: so số dòng, checksum nội dung và cây cha/con
-- giữa jhipster_vnyp (nguồn) và javaspringbootbackend (đích).
-- Mỗi dòng kết quả phải có ok = 1.
--
-- Chạy (Git Bash):
--   docker exec -i javaspringbootbackend-mysql-1 mysql -uroot < db-sync/verify_sync.sql
-- =====================================================================

USE javaspringbootbackend;

-- 1. Số dòng
SELECT 'listing' tbl, (SELECT COUNT(*) FROM jhipster_vnyp.listing) src, (SELECT COUNT(*) FROM javaspringbootbackend.listing) dst
UNION ALL SELECT 'category', (SELECT COUNT(*) FROM jhipster_vnyp.category), (SELECT COUNT(*) FROM javaspringbootbackend.category)
UNION ALL SELECT 'location', (SELECT COUNT(*) FROM jhipster_vnyp.location), (SELECT COUNT(*) FROM javaspringbootbackend.location)
UNION ALL SELECT 'listing_category', (SELECT COUNT(*) FROM jhipster_vnyp.listing_category), (SELECT COUNT(*) FROM javaspringbootbackend.rel_listing__category)
UNION ALL SELECT 'listing_location', (SELECT COUNT(*) FROM jhipster_vnyp.listing_location), (SELECT COUNT(*) FROM javaspringbootbackend.rel_listing__location)
UNION ALL SELECT 'blog_post', (SELECT COUNT(*) FROM jhipster_vnyp.blog_post), (SELECT COUNT(*) FROM javaspringbootbackend.blog_post)
UNION ALL SELECT 'tag', (SELECT COUNT(*) FROM jhipster_vnyp.tag), (SELECT COUNT(*) FROM javaspringbootbackend.tag);

-- 2. Checksum nội dung listing (mọi cột text/số; datetime so riêng ở bước 3)
SELECT 'listing checksum' chk, s.v = d.v ok, s.v src, d.v dst FROM
 (SELECT SUM(CRC32(CONCAT_WS('|', id, wp_id, api_id, name, name_en, name_alias, slug, SHA2(description, 256), phone, mobile,
        email, website, SHA2(address, 256), location_json, tax_code, representative, capital, founded_year, business_type,
        business_status, industry_code, managed_by, thumbnail, SHA2(images, 256), view_count, is_featured, status, es_indexed))) v
  FROM jhipster_vnyp.listing) s,
 (SELECT SUM(CRC32(CONCAT_WS('|', id, wp_id, api_id, name, name_en, name_alias, slug, SHA2(description, 256), phone, mobile,
        email, website, SHA2(address, 256), location_json, tax_code, representative, capital, founded_year, business_type,
        business_status, industry_code, managed_by, thumbnail, SHA2(images, 256), view_count, is_featured, status, es_indexed))) v
  FROM javaspringbootbackend.listing) d;

-- 3. Datetime listing: số dòng lệch giá trị (phải = 0)
SELECT 'listing datetime lệch' chk, COUNT(*) = 0 ok, COUNT(*) so_dong_lech
FROM jhipster_vnyp.listing s JOIN javaspringbootbackend.listing d ON d.id = s.id
WHERE NOT (s.published_at <=> d.published_at AND s.modified_at <=> d.modified_at
           AND s.created_at <=> d.created_at AND s.updated_at <=> d.updated_at);

-- 4. Checksum nội dung bảng nối
SELECT 'listing_category checksum' chk,
 (SELECT SUM(CRC32(CONCAT(listing_id, ':', category_id))) FROM jhipster_vnyp.listing_category) =
 (SELECT SUM(CRC32(CONCAT(listing_id, ':', category_id))) FROM javaspringbootbackend.rel_listing__category) ok
UNION ALL SELECT 'listing_location checksum',
 (SELECT SUM(CRC32(CONCAT(listing_id, ':', location_id))) FROM jhipster_vnyp.listing_location) =
 (SELECT SUM(CRC32(CONCAT(listing_id, ':', location_id))) FROM javaspringbootbackend.rel_listing__location);

-- 5. Cây cha/con: cha ở đích phải đúng là bản ghi có wp_term_id = parent_id nguồn
SELECT 'category parent lệch' chk, COUNT(*) = 0 ok, COUNT(*) so_dong_lech
FROM jhipster_vnyp.category s
JOIN javaspringbootbackend.category d ON d.id = s.id
LEFT JOIN javaspringbootbackend.category dp ON dp.id = d.parent_id
WHERE NOT (s.parent_id <=> dp.wp_term_id)
UNION ALL
SELECT 'location parent lệch', COUNT(*) = 0, COUNT(*)
FROM jhipster_vnyp.location s
JOIN javaspringbootbackend.location d ON d.id = s.id
LEFT JOIN javaspringbootbackend.location dp ON dp.id = d.parent_id
WHERE NOT (s.parent_id <=> dp.wp_term_id);

-- 6. AUTO_INCREMENT phải lớn hơn id lớn nhất (để bản ghi mới không trùng id)
SET SESSION information_schema_stats_expiry = 0;
SELECT t.table_name, t.auto_increment,
       CASE t.table_name WHEN 'listing' THEN (SELECT MAX(id) FROM javaspringbootbackend.listing)
                         WHEN 'category' THEN (SELECT MAX(id) FROM javaspringbootbackend.category)
                         WHEN 'location' THEN (SELECT MAX(id) FROM javaspringbootbackend.location)
                         WHEN 'blog_post' THEN (SELECT MAX(id) FROM javaspringbootbackend.blog_post) END max_id
FROM information_schema.tables t
WHERE t.table_schema = 'javaspringbootbackend' AND t.table_name IN ('listing', 'category', 'location', 'blog_post');
