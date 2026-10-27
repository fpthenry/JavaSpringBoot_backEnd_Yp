#!/usr/bin/env bash
# Sao lưu CSDL hằng ngày, giữ 7 bản gần nhất trong /opt/yp/backups.
# Đặt lịch (root): crontab -e →  30 2 * * * /opt/yp/ops/backup-mysql.sh >> /var/log/yp-backup.log 2>&1
# Khôi phục: gunzip -c /opt/yp/backups/yp-NGAY.sql.gz | docker exec -i yp-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" javaspringbootbackend'
set -euo pipefail

BACKUP_DIR=/opt/yp/backups
KEEP_DAYS=7
mkdir -p "$BACKUP_DIR"
FILE="$BACKUP_DIR/yp-$(date +%F_%H%M).sql.gz"

docker exec yp-mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --quick --no-tablespaces javaspringbootbackend' \
  | gzip > "$FILE.tmp"
mv "$FILE.tmp" "$FILE"
find "$BACKUP_DIR" -name 'yp-*.sql.gz' -mtime +"$KEEP_DAYS" -delete
echo "$(date '+%F %T') OK $FILE ($(du -h "$FILE" | cut -f1))"
