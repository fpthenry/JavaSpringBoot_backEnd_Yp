-- Nạp bảng category_slug_redirect (changelog 20261030090000) từ bảng ghép slug cũ → ngành mới của bước 3
-- (backup_vsic_20261029.category_redirect). Bỏ các slug cũ trùng y slug ngành mới (link cũ vẫn đúng, không cần chuyển).
-- Chạy lại được: REPLACE ghi đè dòng đã có.
--   docker exec -i javaspringbootbackend-mysql-1 mysql -uroot --default-character-set=utf8mb4 < fill_slug_redirect.sql
USE javaspringbootbackend;

REPLACE INTO category_slug_redirect (old_slug, old_name, category_id)
SELECT r.old_slug, LEFT(r.old_name, 255), r.new_category_id
FROM backup_vsic_20261029.category_redirect r
WHERE r.old_slug IS NOT NULL AND r.old_slug <> ''
  AND NOT EXISTS (SELECT 1 FROM category c WHERE c.slug = r.old_slug);

SELECT COUNT(*) slug_cu, SUM(category_id IS NULL) ve_muc_luc FROM category_slug_redirect;
