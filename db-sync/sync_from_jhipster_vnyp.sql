-- =====================================================================
-- Đồng bộ dữ liệu chuẩn từ jhipster_vnyp sang javaspringbootbackend
--
-- - Giữ nguyên id gốc để quan hệ không đổi; AUTO_INCREMENT tự nhảy qua id lớn nhất.
-- - Giữ nguyên giá trị datetime (không đổi múi giờ).
-- - Chạy lại được nhiều lần: bước 0 xóa sạch dữ liệu đích trước khi chép.
-- - Không ghi gì vào jhipster_vnyp.
-- - Elasticsearch KHÔNG tự cập nhật (JHipster index ở tầng service), cần reindex sau.
--
-- Chạy (Git Bash):
--   docker exec -i javaspringbootbackend-mysql-1 mysql -uroot < db-sync/sync_from_jhipster_vnyp.sql
-- =====================================================================

USE javaspringbootbackend;

-- 0. Xóa dữ liệu hiện có (dữ liệu giả của faker) theo thứ tự khóa ngoại
DELETE FROM rel_listing__category;
DELETE FROM rel_listing__location;
DELETE FROM listing;
UPDATE category SET parent_id = NULL;
DELETE FROM category;
UPDATE location SET parent_id = NULL;
DELETE FROM location;
DELETE FROM blog_post;
DELETE FROM tag;

-- 1. category: chép trước với parent_id NULL, sau đó gán cha.
--    Nguồn: category.parent_id trỏ tới wp_term_id của cha, không phải id.
INSERT INTO category (id, wp_term_id, name, slug, description, icon, listing_count, created_at, updated_at, parent_id)
SELECT id, wp_term_id, name, slug, description, icon, listing_count, created_at, updated_at, NULL
FROM jhipster_vnyp.category;

UPDATE category c
JOIN jhipster_vnyp.category s ON s.id = c.id
JOIN jhipster_vnyp.category p ON p.wp_term_id = s.parent_id
SET c.parent_id = p.id;

-- 2. location: tương tự category
INSERT INTO location (id, wp_term_id, name, slug, type, code, latitude, longitude, created_at, parent_id)
SELECT id, wp_term_id, name, slug, type, code, latitude, longitude, created_at, NULL
FROM jhipster_vnyp.location;

UPDATE location l
JOIN jhipster_vnyp.location s ON s.id = l.id
JOIN jhipster_vnyp.location p ON p.wp_term_id = s.parent_id
SET l.parent_id = p.id;

-- 3. listing: tên cột trùng 100% với nguồn
INSERT INTO listing (id, wp_id, api_id, name, name_en, name_alias, slug, description, phone, mobile, email, website,
                     address, location_json, tax_code, representative, capital, founded_year, business_type,
                     business_status, industry_code, managed_by, thumbnail, images, view_count, is_featured, status,
                     published_at, modified_at, created_at, updated_at, es_indexed)
SELECT id, wp_id, api_id, name, name_en, name_alias, slug, description, phone, mobile, email, website,
       address, location_json, tax_code, representative, capital, founded_year, business_type,
       business_status, industry_code, managed_by, thumbnail, images, view_count, is_featured, status,
       published_at, modified_at, created_at, updated_at, es_indexed
FROM jhipster_vnyp.listing;

-- 4. Bảng nối: listing_category -> rel_listing__category, listing_location -> rel_listing__location
INSERT INTO rel_listing__category (listing_id, category_id)
SELECT listing_id, category_id FROM jhipster_vnyp.listing_category;

INSERT INTO rel_listing__location (listing_id, location_id)
SELECT listing_id, location_id FROM jhipster_vnyp.listing_location;

-- 5. blog_post, tag
INSERT INTO blog_post (id, wp_id, title, slug, content, excerpt, thumbnail, status, view_count, published_at, created_at, updated_at)
SELECT id, wp_id, title, slug, content, excerpt, thumbnail, status, view_count, published_at, created_at, updated_at
FROM jhipster_vnyp.blog_post;

INSERT INTO tag (id, wp_term_id, name, slug)
SELECT id, wp_term_id, name, slug FROM jhipster_vnyp.tag;
