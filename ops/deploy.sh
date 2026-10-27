#!/usr/bin/env bash
# Cập nhật bản mới trên server: kéo code từ GitHub, build NGAY TRÊN SERVER, thay bản đang chạy.
#
#   sudo bash /opt/yp/src/backend/ops/deploy.sh backend      # API + trang quản trị (yellowbe.yp.vn)
#   sudo bash /opt/yp/src/backend/ops/deploy.sh fe           # website Next.js (yellowfe.yp.vn)
#   sudo bash /opt/yp/src/backend/ops/deploy.sh all          # backend trước, rồi FE
#   sudo bash /opt/yp/src/backend/ops/deploy.sh fe-rollback  # FE quay về bản build trước
#
# Backend: build jar trong /opt/yp/src/backend (bản cũ vẫn chạy trong lúc build), giữ app.jar.bak, restart, chờ health UP.
# FE: mỗi lần build vào thư mục MỚI /opt/yp/fe-releases/<thời điểm>, build xong mới trỏ /opt/yp/fe sang rồi restart —
#     build lỗi thì website vẫn chạy bản cũ. Giữ 3 bản gần nhất.
# Hướng dẫn đầy đủ: docs/TONG-HOP-DU-AN.md mục 18.
set -euo pipefail

BE_SRC=/opt/yp/src/backend
BE_JAR=/opt/yp/backend/app.jar
FE_SRC=/opt/yp/src/fe
FE_RELEASES=/opt/yp/fe-releases
FE_CURRENT=/opt/yp/fe
KEEP=3

as_yp() { sudo -u yp -H "$@"; }
log() { printf '\n==> %s\n' "$*"; }

deploy_backend() {
  cd "$BE_SRC"
  log "Backend: kéo code"
  as_yp git pull --ff-only
  log "Backend: build $(as_yp git log -1 --format='%h %s') — vài phút"
  as_yp bash ./mvnw -B -q -Pprod clean package -DskipTests
  local built
  built=$(ls target/java-spring-boot-back-end-*.jar | head -1)
  unzip -l "$built" | grep -q "static/index.html" || { echo "Jar không có trang quản trị (static/index.html) — dừng, bản cũ vẫn chạy"; exit 1; }
  [ -f "$BE_JAR" ] && cp -p "$BE_JAR" "$BE_JAR.bak"
  install -o yp -g yp -m 644 "$built" "$BE_JAR"
  log "Backend: khởi động lại (Liquibase tự áp thay đổi CSDL)"
  systemctl restart yp-backend
  for _ in $(seq 1 60); do
    if curl -fs http://127.0.0.1:8080/management/health | grep -q '"UP"'; then
      echo "Backend UP"
      return
    fi
    sleep 5
  done
  echo "Backend chưa UP sau 5 phút. Xem: journalctl -u yp-backend -n 200"
  echo "Quay lại bản cũ: cp -p $BE_JAR.bak $BE_JAR && systemctl restart yp-backend"
  exit 1
}

wait_fe() {
  for _ in $(seq 1 30); do
    if curl -fsI http://127.0.0.1:3000 >/dev/null; then
      echo "FE chạy"
      return
    fi
    sleep 2
  done
  echo "FE chưa chạy. Xem: journalctl -u yp-fe -n 200 — quay lại: $0 fe-rollback"
  exit 1
}

deploy_fe() {
  cd "$FE_SRC"
  [ -f .env.production ] || { echo "Thiếu $FE_SRC/.env.production"; exit 1; }
  [ ! -e .env.local ] || { echo "Có $FE_SRC/.env.local — xóa đi (Next ưu tiên nó hơn .env.production)"; exit 1; }
  [ -L "$FE_CURRENT" ] || [ ! -e "$FE_CURRENT" ] || { echo "$FE_CURRENT là thư mục thật, phải là symlink — đổi tên nó đi rồi chạy lại"; exit 1; }
  log "FE: kéo code"
  as_yp git pull --ff-only
  local rel
  rel="$FE_RELEASES/$(date +%Y%m%d-%H%M%S)"
  as_yp mkdir -p "$rel"
  as_yp sh -c "git archive HEAD | tar -x -C '$rel' && git log -1 --format='%h %s' > '$rel/REVISION'"
  as_yp install -m 600 .env.production "$rel/.env.production"
  log "FE: build $(cat "$rel/REVISION") vào $rel — website vẫn chạy bản cũ"
  if ! (cd "$rel" && as_yp npm ci --no-audit --no-fund && as_yp npm run build); then
    echo "Build lỗi — website vẫn chạy bản cũ. Xóa bản dở: rm -rf $rel"
    exit 1
  fi
  ln -sfn "$rel" "$FE_CURRENT"
  log "FE: khởi động lại"
  systemctl restart yp-fe
  wait_fe
  # Dọn bản cũ, giữ $KEEP bản mới nhất (không bao giờ xóa bản đang chạy)
  local cur
  cur=$(readlink -f "$FE_CURRENT")
  ls -1dt "$FE_RELEASES"/*/ | sed 's:/$::' | tail -n +$((KEEP + 1)) | while read -r old; do
    [ "$old" = "$cur" ] || rm -rf "$old"
  done
}

rollback_fe() {
  local cur prev
  cur=$(readlink -f "$FE_CURRENT")
  prev=$(ls -1dt "$FE_RELEASES"/*/ | sed 's:/$::' | grep -vx "$cur" | head -1 || true)
  [ -n "$prev" ] || { echo "Không có bản FE nào trước $cur"; exit 1; }
  ln -sfn "$prev" "$FE_CURRENT"
  systemctl restart yp-fe
  echo "FE quay về $prev ($(cat "$prev/REVISION" 2>/dev/null || echo '?'))"
  wait_fe
}

[ "$(id -u)" = 0 ] || { echo "Chạy bằng sudo"; exit 1; }
case "${1:-}" in
  backend) deploy_backend ;;
  fe) deploy_fe ;;
  all) deploy_backend && deploy_fe ;;
  fe-rollback) rollback_fe ;;
  *) echo "Dùng: sudo bash $0 backend | fe | all | fe-rollback"; exit 1 ;;
esac
