-- Cập nhật listing.address_new sau khi đổi tên đơn vị hành chính theo danh mục 30/10/2026
-- (fix-locations đã đổi tên trong bảng location; bản sao trước khi đổi: backup_dvhc_20261030.location):
--   3 tỉnh lên thành phố: Quảng Ninh (NQ 36/2026/QH16), Bắc Ninh (39/2026/QH16), Đồng Nai (30/2026/QH16)
--   xã lên phường, đổi kiểu đặt dấu (Hòa → Hoà...): chỉ đổi địa chỉ của doanh nghiệp đang gắn đúng xã đó
-- Sao lưu (id, address_new) các dòng sẽ đổi vào backup_dvhc_20261030.listing_address_new. Chạy lại được (lần 2 không còn gì).
USE javaspringbootbackend;

CREATE TABLE IF NOT EXISTS backup_dvhc_20261030.listing_address_new (id BIGINT PRIMARY KEY, address_new VARCHAR(500));

-- Xã đổi tên: tên cũ (bản sao) → tên mới, với doanh nghiệp đang gắn xã đó
DROP TEMPORARY TABLE IF EXISTS ward_rename;
CREATE TEMPORARY TABLE ward_rename (id BIGINT PRIMARY KEY, old_name VARCHAR(255), new_name VARCHAR(255))
SELECT w.id, o.name old_name, w.name new_name
FROM location w JOIN backup_dvhc_20261030.location o ON o.id = w.id
WHERE w.type = 'ward' AND BINARY o.name <> BINARY w.name;

INSERT IGNORE INTO backup_dvhc_20261030.listing_address_new (id, address_new)
SELECT DISTINCT l.id, l.address_new
FROM listing l JOIN rel_listing__location r ON r.listing_id = l.id JOIN ward_rename w ON w.id = r.location_id
WHERE l.address_new LIKE CONCAT('%', w.old_name, ',%');

INSERT IGNORE INTO backup_dvhc_20261030.listing_address_new (id, address_new)
SELECT id, address_new FROM listing
WHERE address_new LIKE '%, Tỉnh Bắc Ninh' OR address_new LIKE '%, Tỉnh Đồng Nai' OR address_new LIKE '%, Tỉnh Quảng Ninh';

UPDATE listing l JOIN rel_listing__location r ON r.listing_id = l.id JOIN ward_rename w ON w.id = r.location_id
SET l.address_new = SUBSTRING(REPLACE(CONCAT(', ', l.address_new), CONCAT(', ', w.old_name, ', '), CONCAT(', ', w.new_name, ', ')), 3)
WHERE l.address_new LIKE CONCAT('%', w.old_name, ',%');
SELECT 'doi ten xa' buoc, ROW_COUNT() n;

UPDATE listing SET address_new = CONCAT(LEFT(address_new, CHAR_LENGTH(address_new) - CHAR_LENGTH('Tỉnh Bắc Ninh')), 'Thành phố Bắc Ninh')
WHERE address_new LIKE '%, Tỉnh Bắc Ninh';
SELECT 'Bac Ninh' buoc, ROW_COUNT() n;
UPDATE listing SET address_new = CONCAT(LEFT(address_new, CHAR_LENGTH(address_new) - CHAR_LENGTH('Tỉnh Đồng Nai')), 'Thành phố Đồng Nai')
WHERE address_new LIKE '%, Tỉnh Đồng Nai';
SELECT 'Dong Nai' buoc, ROW_COUNT() n;
UPDATE listing SET address_new = CONCAT(LEFT(address_new, CHAR_LENGTH(address_new) - CHAR_LENGTH('Tỉnh Quảng Ninh')), 'Thành phố Quảng Ninh')
WHERE address_new LIKE '%, Tỉnh Quảng Ninh';
SELECT 'Quang Ninh' buoc, ROW_COUNT() n;

SELECT 'sao luu' buoc, COUNT(*) n FROM backup_dvhc_20261030.listing_address_new;
