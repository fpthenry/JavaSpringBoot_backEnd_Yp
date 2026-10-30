#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════════════════════════════
#  Xuất dữ liệu WordPress cũ (máy chủ 192.168.2.200) để đối soát và bổ sung vào backend mới.
#  CHỈ ĐỌC: chỉ chạy SELECT trên CSDL và đếm/đo thư mục ảnh, không sửa gì trên máy chủ cũ.
#
#  Chạy trên máy chủ cũ (quyền root):   bash wp-export.sh [TEN_CSDL] [NGAY_MOC]
#    TEN_CSDL  mặc định ypvn_25_db (DB_NAME trong /var/www/yp.vn/wp-config.php)
#    NGAY_MOC  mặc định 2026-03-01: doanh nghiệp/bài viết tạo hoặc sửa từ ngày này được xuất ĐẦY ĐỦ
#              (nội dung, toàn bộ meta, ngành nghề, địa phương). Dữ liệu app hiện có tới 06/04/2026.
#  Kết quả: /root/yp-export-NGAYGIO.tar.gz (vài chục MB) → tải về máy dev (Git Bash):
#    scp root@192.168.2.200:/root/yp-export-*.tar.gz ~/Downloads/
#  Hướng dẫn: docs/TONG-HOP-DU-AN.md mục "Chuyển ảnh và dữ liệu còn thiếu từ WordPress cũ".
# ═══════════════════════════════════════════════════════════════════════════════════════════
set -euo pipefail

DB="${1:-ypvn_25_db}"
SINCE="${2:-2026-03-01}"
OUT="/root/yp-export-$(date +%Y%m%d-%H%M)"
mkdir -p "$OUT"
q() { mariadb -B --default-character-set=utf8mb4 "$DB" -e "$1"; }

echo "== 1/4 Thông tin máy chủ, CSDL, cấu hình WordPress"
{
  date; hostname; df -h /var/www /root 2>/dev/null
  for f in /var/www/*/wp-config.php; do echo "$f: $(grep -E "DB_NAME|DB_HOST" "$f" | tr -s ' ' | tr '\n' ' ')"; done
  q "SELECT post_type, post_status, COUNT(*) n, MAX(post_date) moi_nhat, MAX(post_modified) sua_gan_nhat
     FROM wp_posts WHERE post_type IN ('listing','post','attachment','page') GROUP BY post_type, post_status"
} > "$OUT/info.txt" 2>&1
cat "$OUT/info.txt"

echo "== 2/4 Kiểm kê kho ảnh (có thể mất vài phút)"
{
  for d in /var/www/*/wp-content/uploads /var/www/yp.vn/media; do
    [ -d "$d" ] || continue
    echo "### $d  (symlink: $(readlink -f "$d"))"
    du -sh "$d" 2>/dev/null
    echo "so_file: $(find "$d" -type f | wc -l)"
    du -sh "$d"/* 2>/dev/null | sort -k2
  done
} > "$OUT/uploads-inventory.txt" 2>&1
grep -E "^###|so_file|^[0-9.]+[KMGT]\s+/var/www/[^/]+/wp-content/uploads$" "$OUT/uploads-inventory.txt" || true
# Danh sách file (đường dẫn tương đối, kích thước) của kho chính — để kiểm tra sau khi chép
UP=/var/www/yp.vn/wp-content/uploads
[ -d "$UP" ] && (cd "$UP" && find . -type f -printf '%P\t%s\n') | gzip > "$OUT/uploads-files.tsv.gz"

echo "== 3/4 Tóm tắt toàn bộ doanh nghiệp, bài viết, ảnh"
q "SELECT ID, post_type, post_status, post_date, post_modified, post_parent, post_mime_type, post_name, LEFT(post_title, 500)
   FROM wp_posts WHERE post_type IN ('listing','attachment','post')" | gzip > "$OUT/posts.tsv.gz"
q "SELECT post_id, meta_key, meta_value FROM wp_postmeta
   WHERE meta_key IN ('_thumbnail_id','webbupointfinder_item_images','_wp_attached_file')" | gzip > "$OUT/meta.tsv.gz"

echo "== 4/4 Dữ liệu đầy đủ của doanh nghiệp/bài viết tạo hoặc sửa từ $SINCE"
RECENT="SELECT ID FROM wp_posts WHERE post_type IN ('listing','post') AND (post_modified >= '$SINCE' OR post_date >= '$SINCE')"
q "SELECT * FROM wp_posts WHERE ID IN ($RECENT)" | gzip > "$OUT/recent-posts.tsv.gz"
q "SELECT post_id, meta_key, meta_value FROM wp_postmeta WHERE post_id IN ($RECENT)" | gzip > "$OUT/recent-meta.tsv.gz"
q "SELECT tr.object_id, tt.taxonomy, t.term_id, t.slug, t.name, tt.parent
   FROM wp_term_relationships tr JOIN wp_term_taxonomy tt ON tt.term_taxonomy_id = tr.term_taxonomy_id
   JOIN wp_terms t ON t.term_id = tt.term_id WHERE tr.object_id IN ($RECENT)" | gzip > "$OUT/recent-terms.tsv.gz"
q "SELECT COUNT(*) so_dn_bai_viet_moi FROM wp_posts WHERE ID IN ($RECENT)" | tee -a "$OUT/info.txt"

tar -czf "$OUT.tar.gz" -C /root "$(basename "$OUT")"
echo
echo "XONG: $OUT.tar.gz ($(du -h "$OUT.tar.gz" | cut -f1))"
echo "Tải về máy dev (Git Bash):  scp root@$(hostname -I | awk '{print $1}'):$OUT.tar.gz ~/Downloads/"
