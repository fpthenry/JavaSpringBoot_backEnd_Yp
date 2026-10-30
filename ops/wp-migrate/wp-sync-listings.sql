-- ═══════════════════════════════════════════════════════════════════════════════════════════════════════════
--  Đồng bộ DOANH NGHIỆP từ WordPress cũ vào backend. Nguồn: schema ypvn_25_db (nạp từ bản dump WordPress, chỉ đọc).
--  Đích: javaspringbootbackend.listing + rel_listing__category + rel_listing__location. Bảng phụ, bản sao lưu: wp_extract.
--
--  Chạy lại được (mỗi lần chỉ xử lý phần còn chênh):
--    1. Thêm doanh nghiệp WordPress đang xuất bản mà app chưa có (theo wp_id). source = 'wordpress', source_ref = wp_id.
--    2. Cập nhật doanh nghiệp WordPress sửa sau app (post_modified > listing.modified_at): ghi đè các cột lấy từ WordPress,
--       ngành theo WordPress, địa phương gắn lại từ WordPress (job link-listings bổ sung đơn vị mới/cũ sau).
--    3. Logo, thư viện ảnh khác WordPress → lấy theo WordPress (link https://yp.vn/wp-content/uploads/...).
--    4. Doanh nghiệp app đang hiện nhưng WordPress đã bỏ xuất bản/xóa → status = 'draft' (ẩn, không xóa).
--  Trước khi ghi: sao lưu dòng listing và liên kết sẽ bị sửa vào wp_extract.bak_listing / bak_rel_category / bak_rel_location.
--
--  Chạy:  docker exec -i javaspringbootbackend-mysql-1 mysql -uroot --default-character-set=utf8mb4 < ops/wp-migrate/wp-sync-listings.sql
--  Sau đó (backend đang chạy, tài khoản admin): link-listings → new-addresses?overwrite=false → reindex listing.
--  Xem docs/TONG-HOP-DU-AN.md mục 18.8.
-- ═══════════════════════════════════════════════════════════════════════════════════════════════════════════
SET NAMES utf8mb4;
SET SESSION sql_mode = '';
SET SESSION group_concat_max_len = 1000000;
CREATE DATABASE IF NOT EXISTS wp_extract CHARACTER SET utf8mb4;
USE wp_extract;

SET @uploads = 'https://yp.vn/wp-content/uploads/';
SET @now = NOW(6);

-- ── Danh sách cần xử lý ─────────────────────────────────────────────────────────────────────────────────────
DROP TABLE IF EXISTS s_target;
CREATE TABLE s_target (wp_id BIGINT PRIMARY KEY, app_id BIGINT NULL, kind VARCHAR(10));
INSERT INTO s_target
SELECT p.ID, NULL, 'new' FROM ypvn_25_db.wp_posts p
WHERE p.post_type = 'listing' AND p.post_status = 'publish'
  AND NOT EXISTS (SELECT 1 FROM javaspringbootbackend.listing l WHERE l.wp_id = p.ID);
INSERT INTO s_target
SELECT p.ID, l.id, 'update' FROM javaspringbootbackend.listing l JOIN ypvn_25_db.wp_posts p ON p.ID = l.wp_id
WHERE p.post_type = 'listing' AND p.post_status = 'publish' AND p.post_modified > l.modified_at;

-- ── Meta WordPress → cột app (một dòng mỗi doanh nghiệp) ────────────────────────────────────────────────────
DROP TABLE IF EXISTS s_meta;
CREATE TABLE s_meta (wp_id BIGINT PRIMARY KEY) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci
SELECT t.wp_id,
  NULLIF(MAX(CASE WHEN m.meta_key = 'api_id' THEN m.meta_value END), '') api_id,
  NULLIF(MAX(CASE WHEN m.meta_key = 'webbupointfinder_item_name_en' THEN m.meta_value END), '') name_en,
  NULLIF(MAX(CASE WHEN m.meta_key = 'webbupointfinder_item_name_alias' THEN m.meta_value END), '') name_alias,
  NULLIF(MAX(CASE WHEN m.meta_key = 'webbupointfinder_item_telephone' THEN m.meta_value END), '') phone,
  NULLIF(MAX(CASE WHEN m.meta_key = 'webbupointfinder_item_mobile' THEN m.meta_value END), '') mobile,
  NULLIF(MAX(CASE WHEN m.meta_key = 'webbupointfinder_item_email' THEN m.meta_value END), '') email,
  NULLIF(MAX(CASE WHEN m.meta_key = 'webbupointfinder_item_website' THEN m.meta_value END), '') website,
  NULLIF(MAX(CASE WHEN m.meta_key = 'webbupointfinder_items_address' THEN m.meta_value END), '') address,
  NULLIF(MAX(CASE WHEN m.meta_key = 'webbupointfinder_items_location' THEN m.meta_value END), '') coord_raw,
  NULLIF(MAX(CASE WHEN m.meta_key = 'new_location' THEN m.meta_value END), '') new_location,
  NULLIF(MAX(CASE WHEN m.meta_key = 'webbupointfinder_item_taxcode' THEN m.meta_value END), '') tax_code,
  NULLIF(MAX(CASE WHEN m.meta_key = 'webbupointfinder_item_representative' THEN m.meta_value END), '') representative,
  NULLIF(MAX(CASE WHEN m.meta_key = 'webbupointfinder_item_capital' THEN m.meta_value END), '') capital,
  NULLIF(MAX(CASE WHEN m.meta_key = 'webbupointfinder_item_begin' THEN m.meta_value END), '') founded_year,
  NULLIF(MAX(CASE WHEN m.meta_key = 'webbupointfinder_item_type' THEN m.meta_value END), '') business_type,
  NULLIF(MAX(CASE WHEN m.meta_key = 'webbupointfinder_item_status' THEN m.meta_value END), '') business_status,
  NULLIF(MAX(CASE WHEN m.meta_key = 'webbupointfinder_item_managed' THEN m.meta_value END), '') managed_by,
  MAX(CASE WHEN m.meta_key = 'webbupointfinder_item_featuredmarker' THEN m.meta_value END) = '1' is_featured
FROM s_target t
LEFT JOIN ypvn_25_db.wp_postmeta m ON m.post_id = t.wp_id AND m.meta_key IN (
  'api_id', 'webbupointfinder_item_name_en', 'webbupointfinder_item_name_alias', 'webbupointfinder_item_telephone',
  'webbupointfinder_item_mobile', 'webbupointfinder_item_email', 'webbupointfinder_item_website',
  'webbupointfinder_items_address', 'webbupointfinder_items_location', 'new_location', 'webbupointfinder_item_taxcode',
  'webbupointfinder_item_representative', 'webbupointfinder_item_capital', 'webbupointfinder_item_begin',
  'webbupointfinder_item_type', 'webbupointfinder_item_status', 'webbupointfinder_item_managed',
  'webbupointfinder_item_featuredmarker')
GROUP BY t.wp_id;
ALTER TABLE s_meta CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- ── location_json khi WordPress không có new_location: tách "…, <Phường/Xã>, <TP/Tỉnh>, Việt Nam" (địa chỉ mới) ──
-- Tọa độ giữ nguyên chữ số, chỉ đổi "vĩ,kinh" → "vĩ:kinh"; ngoài Việt Nam thì bỏ.
DROP TABLE IF EXISTS s_addr;
CREATE TABLE s_addr (wp_id BIGINT PRIMARY KEY) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci
SELECT wp_id, a, street, ward_raw,
  CASE WHEN prov_raw LIKE 'TP %' THEN CONCAT('Thành phố ', TRIM(SUBSTRING(prov_raw, 4)))
       WHEN prov_raw LIKE 'TP.%' THEN CONCAT('Thành phố ', TRIM(SUBSTRING(prov_raw, 4)))
       ELSE prov_raw END prov_name,
  coord
FROM (
  SELECT wp_id, a,
    TRIM(SUBSTRING_INDEX(a, ',', -1)) prov_raw,
    IF(a LIKE '%,%,%', TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(a, ',', -2), ',', 1)), NULL) ward_raw,
    IF(a LIKE '%,%,%', TRIM(LEFT(a, CHAR_LENGTH(a) - CHAR_LENGTH(SUBSTRING_INDEX(a, ',', -2)) - 1)), NULL) street,
    coord
  FROM (
    SELECT wp_id,
      REGEXP_REPLACE(TRIM(TRAILING '.' FROM TRIM(address)), ',\\s*Vi[eệ]t Nam$', '') a,
      CASE WHEN REPLACE(coord_raw, ' ', '') REGEXP '^[0-9]+\\.[0-9]+,[0-9]+\\.[0-9]+$'
            AND CAST(SUBSTRING_INDEX(REPLACE(coord_raw, ' ', ''), ',', 1) AS DECIMAL(20, 12)) BETWEEN 8 AND 24
            AND CAST(SUBSTRING_INDEX(REPLACE(coord_raw, ' ', ''), ',', -1) AS DECIMAL(20, 12)) BETWEEN 102 AND 110
           THEN REPLACE(REPLACE(coord_raw, ' ', ''), ',', ':') END coord
    FROM s_meta WHERE new_location IS NULL AND address IS NOT NULL
  ) x
) y;
ALTER TABLE s_addr ADD prov_id BIGINT, ADD prov_full VARCHAR(255), ADD prov_code VARCHAR(50),
  ADD ward_name VARCHAR(255), ADD ward_code VARCHAR(50);
-- Tỉnh mới (slug kết thúc -2025), khớp đúng một
UPDATE s_addr s JOIN (
  SELECT s.wp_id, MIN(p.id) id, MIN(p.name) name, MIN(p.code) code, COUNT(*) n
  FROM s_addr s JOIN javaspringbootbackend.location p
    ON p.type = 'province' AND p.slug LIKE '%-2025' AND p.name = s.prov_name
  GROUP BY s.wp_id HAVING n = 1
) m ON m.wp_id = s.wp_id
SET s.prov_id = m.id, s.prov_full = m.name, s.prov_code = m.code;
-- Xã mới trong tỉnh đó (parent_id = id tỉnh mới), khớp đúng một
UPDATE s_addr s JOIN (
  SELECT s.wp_id, MIN(w.name) name, MIN(w.code) code, COUNT(*) n
  FROM s_addr s JOIN javaspringbootbackend.location w
    ON w.type = 'ward' AND w.slug LIKE '%-2025' AND w.parent_id = s.prov_id AND w.name = s.ward_raw
  GROUP BY s.wp_id HAVING n = 1
) m ON m.wp_id = s.wp_id
SET s.ward_name = m.name, s.ward_code = m.code;
ALTER TABLE s_meta ADD location_json LONGTEXT;
-- Cùng cấu trúc new_location của WordPress: {province, ward, street, ward_type, province_code, ward_code, coordinate}
UPDATE s_meta s JOIN s_addr a ON a.wp_id = s.wp_id
SET s.location_json = CONCAT('{', CONCAT_WS(',',
    IF(a.prov_code IS NULL, NULL, CONCAT('"province":', JSON_QUOTE(a.prov_full))),
    IF(a.ward_code IS NULL, NULL, CONCAT('"ward":', JSON_QUOTE(a.ward_name))),
    IF(a.ward_code IS NULL OR a.street IS NULL, NULL, CONCAT('"street":', JSON_QUOTE(a.street))),
    IF(a.ward_code IS NULL, NULL, CONCAT('"ward_type":', JSON_QUOTE(CASE
      WHEN a.ward_name LIKE 'Phường %' THEN 'Phường' WHEN a.ward_name LIKE 'Xã %' THEN 'Xã'
      WHEN a.ward_name LIKE 'Đặc khu %' THEN 'Đặc khu' END))),
    IF(a.prov_code IS NULL, NULL, CONCAT('"province_code":', JSON_QUOTE(a.prov_code))),
    IF(a.ward_code IS NULL, NULL, CONCAT('"ward_code":', JSON_QUOTE(a.ward_code))),
    IF(a.coord IS NULL, NULL, CONCAT('"coordinate":', JSON_QUOTE(a.coord)))
  ), '}')
WHERE a.prov_code IS NOT NULL OR a.coord IS NOT NULL;
UPDATE s_meta SET location_json = new_location WHERE new_location IS NOT NULL;

-- ── Ảnh theo WordPress (mọi doanh nghiệp đang xuất bản): logo = _thumbnail_id, thư viện = webbupointfinder_item_images ──
DROP TABLE IF EXISTS s_att;
CREATE TABLE s_att (att_id BIGINT PRIMARY KEY, file VARCHAR(1000), caption TEXT) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci
SELECT m.post_id att_id, MIN(m.meta_value) file, MIN(a.post_excerpt) caption
FROM ypvn_25_db.wp_postmeta m JOIN ypvn_25_db.wp_posts a ON a.ID = m.post_id
WHERE m.meta_key = '_wp_attached_file' AND m.meta_value <> '' GROUP BY m.post_id;
ALTER TABLE s_att CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
DROP TABLE IF EXISTS s_logo;
CREATE TABLE s_logo (wp_id BIGINT PRIMARY KEY, url VARCHAR(1000)) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci
SELECT m.post_id wp_id, CONCAT(@uploads, MIN(f.file)) url
FROM ypvn_25_db.wp_postmeta m
JOIN ypvn_25_db.wp_posts p ON p.ID = m.post_id AND p.post_type = 'listing' AND p.post_status = 'publish'
JOIN s_att f ON f.att_id = CAST(m.meta_value AS UNSIGNED)
WHERE m.meta_key = '_thumbnail_id' AND m.meta_value REGEXP '^[0-9]+$' AND m.meta_value <> '0'
GROUP BY m.post_id;
-- Định dạng như dữ liệu đang có: [{"image_url":"…","caption":"…"}], giữ thứ tự WordPress
DROP TABLE IF EXISTS s_gallery;
CREATE TABLE s_gallery (wp_id BIGINT PRIMARY KEY, images LONGTEXT, urls LONGTEXT) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci
SELECT m.post_id wp_id,
  CONCAT('[', GROUP_CONCAT(CONCAT('{"image_url":', JSON_QUOTE(CONCAT(@uploads, f.file)), ',"caption":',
    JSON_QUOTE(IFNULL(f.caption, '')), '}') ORDER BY m.meta_id SEPARATOR ','), ']') images,
  GROUP_CONCAT(CONCAT(@uploads, f.file) ORDER BY m.meta_id SEPARATOR '\n') urls
FROM ypvn_25_db.wp_postmeta m
JOIN ypvn_25_db.wp_posts p ON p.ID = m.post_id AND p.post_type = 'listing' AND p.post_status = 'publish'
JOIN s_att f ON f.att_id = CAST(m.meta_value AS UNSIGNED)
WHERE m.meta_key = 'webbupointfinder_item_images' AND m.meta_value REGEXP '^[0-9]+$'
GROUP BY m.post_id;
-- Doanh nghiệp đã có trong app mà logo/thư viện khác WordPress (so theo từng link ảnh)
DROP TABLE IF EXISTS s_img_fix;
CREATE TABLE s_img_fix (app_id BIGINT PRIMARY KEY, fix_logo TINYINT, fix_gallery TINYINT)
SELECT l.id app_id,
  (g.url IS NOT NULL AND NOT (l.thumbnail <=> g.url)) fix_logo,
  (s.images IS NOT NULL AND (l.images IS NULL OR l.images NOT LIKE '[%' OR EXISTS (
     SELECT 1 FROM JSON_TABLE(CONCAT('["', REPLACE(REPLACE(s.urls, '"', '\\"'), '\n', '","'), '"]'),
       '$[*]' COLUMNS (u VARCHAR(1000) PATH '$')) j
     WHERE l.images NOT LIKE CONCAT('%"', j.u, '"%')))) fix_gallery
FROM javaspringbootbackend.listing l
LEFT JOIN s_logo g ON g.wp_id = l.wp_id
LEFT JOIN s_gallery s ON s.wp_id = l.wp_id
WHERE (g.wp_id IS NOT NULL OR s.wp_id IS NOT NULL)
  AND l.id NOT IN (SELECT app_id FROM s_target WHERE app_id IS NOT NULL)
HAVING fix_logo OR fix_gallery;

-- ── Doanh nghiệp app đang hiện nhưng WordPress đã bỏ xuất bản/xóa ─────────────────────────────────────────────
DROP TABLE IF EXISTS s_unpublish;
CREATE TABLE s_unpublish (app_id BIGINT PRIMARY KEY, wp_status VARCHAR(20))
SELECT l.id app_id, IFNULL(p.post_status, '(đã xóa)') wp_status
FROM javaspringbootbackend.listing l LEFT JOIN ypvn_25_db.wp_posts p ON p.ID = l.wp_id AND p.post_type = 'listing'
WHERE l.wp_id IS NOT NULL AND l.status = 'publish' AND (p.ID IS NULL OR p.post_status <> 'publish');

-- ── Sao lưu trước khi ghi (cộng dồn qua các lần chạy, cột backup_at) ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bak_listing SELECT CAST(NULL AS DATETIME(6)) backup_at, l.* FROM javaspringbootbackend.listing l WHERE 1 = 0;
DROP TABLE IF EXISTS s_touched;
CREATE TABLE s_touched (app_id BIGINT PRIMARY KEY)
SELECT app_id FROM s_target WHERE app_id IS NOT NULL
UNION SELECT app_id FROM s_img_fix UNION SELECT app_id FROM s_unpublish;
INSERT INTO bak_listing SELECT @now, l.* FROM javaspringbootbackend.listing l JOIN s_touched t ON t.app_id = l.id;
CREATE TABLE IF NOT EXISTS bak_rel_category (backup_at DATETIME(6), category_id BIGINT, listing_id BIGINT);
CREATE TABLE IF NOT EXISTS bak_rel_location (backup_at DATETIME(6), location_id BIGINT, listing_id BIGINT);
INSERT INTO bak_rel_category SELECT @now, r.category_id, r.listing_id FROM javaspringbootbackend.rel_listing__category r
  JOIN s_target t ON t.app_id = r.listing_id;
INSERT INTO bak_rel_location SELECT @now, r.location_id, r.listing_id FROM javaspringbootbackend.rel_listing__location r
  JOIN s_target t ON t.app_id = r.listing_id;

DROP TABLE IF EXISTS s_cat;
CREATE TABLE s_cat (wp_id BIGINT, category_id BIGINT, PRIMARY KEY (wp_id, category_id))
SELECT DISTINCT t.wp_id, c.id category_id FROM s_target t
JOIN ypvn_25_db.wp_term_relationships tr ON tr.object_id = t.wp_id
JOIN ypvn_25_db.wp_term_taxonomy tt ON tt.term_taxonomy_id = tr.term_taxonomy_id AND tt.taxonomy = 'pointfinderltypes'
JOIN javaspringbootbackend.category c ON c.wp_term_id = tt.term_id;

-- ── Ghi vào app (một transaction) ────────────────────────────────────────────────────────────────────────────
START TRANSACTION;

-- 1. Thêm mới
INSERT INTO javaspringbootbackend.listing (wp_id, api_id, name, name_en, name_alias, slug, description, phone, mobile,
  email, website, address, location_json, tax_code, representative, capital, founded_year, business_type,
  business_status, managed_by, thumbnail, images, view_count, is_featured, status, published_at, modified_at,
  created_at, updated_at, es_indexed, source, source_ref)
SELECT p.ID, m.api_id, LEFT(p.post_title, 500), LEFT(m.name_en, 500), LEFT(m.name_alias, 500), LEFT(p.post_name, 500),
  p.post_content, LEFT(m.phone, 100), LEFT(m.mobile, 100), LEFT(m.email, 255), LEFT(m.website, 500), m.address,
  m.location_json, LEFT(m.tax_code, 100), LEFT(m.representative, 255), LEFT(m.capital, 100), LEFT(m.founded_year, 50),
  LEFT(m.business_type, 100), LEFT(m.business_status, 50), LEFT(m.managed_by, 255), g.url, s.images,
  0, IFNULL(m.is_featured, 0), 'publish', p.post_date, p.post_modified, @now, @now, 0, 'wordpress', CAST(p.ID AS CHAR)
FROM s_target t JOIN ypvn_25_db.wp_posts p ON p.ID = t.wp_id JOIN s_meta m ON m.wp_id = t.wp_id
LEFT JOIN s_logo g ON g.wp_id = t.wp_id LEFT JOIN s_gallery s ON s.wp_id = t.wp_id
WHERE t.kind = 'new';
UPDATE s_target t JOIN javaspringbootbackend.listing l ON l.wp_id = t.wp_id SET t.app_id = l.id WHERE t.kind = 'new';

-- 2. Cập nhật theo WordPress. Địa chỉ đổi → address_new = NULL để job new-addresses tạo lại.
UPDATE javaspringbootbackend.listing l
JOIN s_target t ON t.app_id = l.id AND t.kind = 'update'
JOIN ypvn_25_db.wp_posts p ON p.ID = t.wp_id JOIN s_meta m ON m.wp_id = t.wp_id
LEFT JOIN s_logo g ON g.wp_id = t.wp_id LEFT JOIN s_gallery s ON s.wp_id = t.wp_id
SET l.address_new = IF(l.address <=> m.address, l.address_new, NULL),
  l.api_id = IFNULL(m.api_id, l.api_id), l.name = LEFT(p.post_title, 500), l.name_en = LEFT(m.name_en, 500),
  l.name_alias = LEFT(m.name_alias, 500), l.slug = LEFT(p.post_name, 500), l.description = p.post_content,
  l.phone = LEFT(m.phone, 100), l.mobile = LEFT(m.mobile, 100), l.email = LEFT(m.email, 255),
  l.website = LEFT(m.website, 500), l.address = m.address, l.location_json = IFNULL(m.new_location, l.location_json),
  l.tax_code = LEFT(m.tax_code, 100), l.representative = LEFT(m.representative, 255), l.capital = LEFT(m.capital, 100),
  l.founded_year = LEFT(m.founded_year, 50), l.business_type = LEFT(m.business_type, 100),
  l.business_status = LEFT(m.business_status, 50), l.managed_by = LEFT(m.managed_by, 255),
  l.thumbnail = IFNULL(g.url, l.thumbnail), l.images = IFNULL(s.images, l.images),
  l.is_featured = IFNULL(m.is_featured, l.is_featured), l.status = 'publish', l.published_at = p.post_date,
  l.modified_at = p.post_modified, l.updated_at = @now, l.es_indexed = 0;

-- Ngành: theo WordPress (chỉ thay khi WordPress có ít nhất một ngành có trong app)
DELETE r FROM javaspringbootbackend.rel_listing__category r JOIN s_target t ON t.app_id = r.listing_id AND t.kind = 'update'
WHERE EXISTS (SELECT 1 FROM s_cat c WHERE c.wp_id = t.wp_id)
  AND NOT EXISTS (SELECT 1 FROM s_cat c WHERE c.wp_id = t.wp_id AND c.category_id = r.category_id);
INSERT IGNORE INTO javaspringbootbackend.rel_listing__category (category_id, listing_id)
SELECT c.category_id, t.app_id FROM s_cat c JOIN s_target t ON t.wp_id = c.wp_id;

-- Địa phương: gắn lại từ WordPress; đơn vị mới/cũ suy ra thì job link-listings thêm sau
DELETE r FROM javaspringbootbackend.rel_listing__location r JOIN s_target t ON t.app_id = r.listing_id AND t.kind = 'update';
INSERT IGNORE INTO javaspringbootbackend.rel_listing__location (location_id, listing_id)
SELECT DISTINCT lo.id, t.app_id FROM s_target t
JOIN ypvn_25_db.wp_term_relationships tr ON tr.object_id = t.wp_id
JOIN ypvn_25_db.wp_term_taxonomy tt ON tt.term_taxonomy_id = tr.term_taxonomy_id AND tt.taxonomy = 'pointfinderlocations'
JOIN javaspringbootbackend.location lo ON lo.wp_term_id = tt.term_id;

-- 3. Logo, thư viện ảnh của doanh nghiệp không nằm trong (1), (2)
UPDATE javaspringbootbackend.listing l JOIN s_img_fix f ON f.app_id = l.id
LEFT JOIN s_logo g ON g.wp_id = l.wp_id LEFT JOIN s_gallery s ON s.wp_id = l.wp_id
SET l.thumbnail = IF(f.fix_logo, g.url, l.thumbnail), l.images = IF(f.fix_gallery, s.images, l.images),
  l.updated_at = @now, l.es_indexed = 0;

-- 4. Ẩn doanh nghiệp WordPress đã bỏ
UPDATE javaspringbootbackend.listing l JOIN s_unpublish u ON u.app_id = l.id
SET l.status = 'draft', l.updated_at = @now, l.es_indexed = 0;

COMMIT;

-- ── Báo cáo ──────────────────────────────────────────────────────────────────────────────────────────────────
SELECT 'Thêm mới' muc, COUNT(*) so_dn FROM s_target WHERE kind = 'new'
UNION ALL SELECT 'Cập nhật theo WordPress', COUNT(*) FROM s_target WHERE kind = 'update'
UNION ALL SELECT 'Sửa logo', COUNT(*) FROM s_img_fix WHERE fix_logo
UNION ALL SELECT 'Sửa thư viện ảnh', COUNT(*) FROM s_img_fix WHERE fix_gallery
UNION ALL SELECT 'Ẩn (WordPress đã bỏ)', COUNT(*) FROM s_unpublish;
SELECT 'Mới: địa phương' muc, COUNT(*) so_dn,
  SUM(m.location_json LIKE '%"ward_code"%') co_xa_moi, SUM(m.location_json LIKE '%"province_code"%') co_tinh_moi,
  SUM(m.location_json LIKE '%"coordinate"%') co_toa_do,
  SUM(EXISTS (SELECT 1 FROM s_cat c WHERE c.wp_id = t.wp_id)) co_nganh
FROM s_target t JOIN s_meta m ON m.wp_id = t.wp_id WHERE t.kind = 'new';
SELECT 'Mới: chưa tách được xã' muc, a.wp_id, a.ward_raw, a.prov_name FROM s_addr a JOIN s_target t ON t.wp_id = a.wp_id
WHERE t.kind = 'new' AND a.ward_code IS NULL;
SELECT 'Ẩn' muc, u.app_id, u.wp_status, LEFT(l.name, 80) ten FROM s_unpublish u JOIN javaspringbootbackend.listing l ON l.id = u.app_id;
