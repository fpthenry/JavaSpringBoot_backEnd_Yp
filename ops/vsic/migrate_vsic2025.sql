-- Chuyển ngành nghề sang VSIC 2025 (bước 3). Chạy MỘT lần trên MySQL dev, sau vsic_data.sql (ops/vsic/migrate_sql.py sinh):
--   docker exec -i javaspringbootbackend-mysql-1 mysql -uroot < vsic_data.sql   (sau khi tạo schema sao lưu ở dưới)
-- Chạy lại lần hai sẽ dừng ngay ở bước 0 (bảng sao lưu đã có), không ghi gì.
-- Sao lưu giữ trong schema backup_vsic_20261029: category (2.394 ngành cũ), rel_listing__category (bảng nối cũ, nguyên vẹn),
-- category_redirect (slug cũ → ngành mới, cho 301). Khôi phục: xem docs/TONG-HOP-DU-AN.md mục 18.9.

-- 0. Sao lưu ngành cũ (CREATE TABLE không IF NOT EXISTS: chạy lần hai thì lỗi và dừng ở đây)
CREATE TABLE backup_vsic_20261029.category AS SELECT * FROM javaspringbootbackend.category;
SELECT 'sao luu nganh cu' buoc, COUNT(*) n FROM backup_vsic_20261029.category;

USE javaspringbootbackend;

-- 1. Nạp ngành VSIC 2025, rồi gắn ngành cha theo mã
INSERT INTO category (code, level, name, slug, description, listing_count, created_at, updated_at)
SELECT code, level, name, slug, descr, 0, NOW(6), NOW(6) FROM backup_vsic_20261029.vsic_new ORDER BY ord;
UPDATE category c
  JOIN backup_vsic_20261029.vsic_new v ON v.code = c.code
  JOIN category p ON p.code = v.parent
SET c.parent_id = p.id;
SELECT 'nganh VSIC' buoc, COUNT(*) n, SUM(parent_id IS NULL) goc FROM category WHERE code IS NOT NULL;

-- 2. Bảng nối mới theo bảng ghép (đọc bảng cũ theo khóa chính để ghi gần như tuần tự) (wp_extract.final_map: ngành cũ → một hoặc nhiều mã); ngành cũ không ghép được thì bỏ liên kết
CREATE TABLE rel_listing__category_vsic LIKE rel_listing__category;
INSERT IGNORE INTO rel_listing__category_vsic (category_id, listing_id)
SELECT STRAIGHT_JOIN c.id, r.listing_id
FROM rel_listing__category r FORCE INDEX (PRIMARY)
  JOIN wp_extract.final_map f ON f.category_id = r.category_id
  JOIN category c ON c.code = f.new_code;
SELECT 'lien ket moi' buoc, COUNT(*) n FROM rel_listing__category_vsic;

-- 3. Tráo bảng: bảng nối cũ chuyển nguyên sang schema sao lưu (bỏ khóa ngoại để xóa được ngành cũ)
ALTER TABLE rel_listing__category
  DROP FOREIGN KEY fk_rel_listing__category__category_id,
  DROP FOREIGN KEY fk_rel_listing__category__listing_id;
RENAME TABLE rel_listing__category TO backup_vsic_20261029.rel_listing__category,
             rel_listing__category_vsic TO rel_listing__category;
SET SESSION foreign_key_checks = 0;
ALTER TABLE rel_listing__category
  ADD CONSTRAINT fk_rel_listing__category__category_id FOREIGN KEY (category_id) REFERENCES category (id),
  ADD CONSTRAINT fk_rel_listing__category__listing_id FOREIGN KEY (listing_id) REFERENCES listing (id);
SET SESSION foreign_key_checks = 1;

-- 4. Slug cũ → ngành mới (bước 4 dùng cho chuyển hướng 301)
UPDATE backup_vsic_20261029.category_redirect r
  JOIN category c ON c.code = r.new_code
SET r.new_category_id = c.id, r.new_slug = c.slug;

-- 5. Xóa ngành cũ (đúng các id đã sao lưu)
UPDATE category c JOIN backup_vsic_20261029.category b ON b.id = c.id SET c.parent_id = NULL;
DELETE c FROM category c JOIN backup_vsic_20261029.category b ON b.id = c.id;

SELECT 'ket qua' buoc, COUNT(*) nganh, SUM(code IS NULL) chua_co_ma FROM category;
SELECT 'doanh nghiep co nganh' buoc, COUNT(DISTINCT listing_id) n FROM rel_listing__category;
SELECT 'redirect' buoc, COUNT(*) n, SUM(new_category_id IS NULL) ve_trang_nganh_nghe FROM backup_vsic_20261029.category_redirect;
SELECT 'XONG' buoc;
