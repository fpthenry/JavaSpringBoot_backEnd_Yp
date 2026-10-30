"""
Sinh SQL chuyển bảng category sang VSIC 2025 (bước 3), chạy thẳng trong MySQL (xem migrate_vsic2025.sql sinh ra).
  - Nạp 1.606 ngành VSIC 2025 (mã, cấp, tên, mô tả "Chi tiết", ngành cha), slug = tên + mã như categorySlug() ở giao diện.
  - Bảng slug cũ → ngành mới (cho chuyển hướng 301 ở bước 4).
python -I migrate_sql.py vsic2025.json final_map.json migrate_vsic2025.sql
"""
import html, json, re, sys, unicodedata

sys.stdout.reconfigure(encoding="utf-8")
f_tree, f_map, f_out = sys.argv[1:4]
BACKUP = "backup_vsic_20261029"
tree = json.load(open(f_tree, encoding="utf-8"))
codes = {r["code"] for r in tree}
final = json.load(open(f_map, encoding="utf-8"))


def slugify(s):
    """Như categorySlug() (category-code.ts): &amp; → &, & → " va ", bỏ dấu, đ → d, nối bằng gạch ngang."""
    s = html.unescape(s).replace("&", " va ").replace("đ", "d").replace("Đ", "D")
    s = "".join(c for c in unicodedata.normalize("NFD", s) if unicodedata.category(c) != "Mn").lower()
    return re.sub(r"[^a-z0-9]+", "-", s).strip("-")


def q(v):
    if v is None:
        return "NULL"
    return "'" + str(v).replace("\\", "\\\\").replace("'", "''") + "'"


def redirect_code(new):
    """Một ngành cũ ghép nhiều mã: lấy mã cha chung sâu nhất có trong VSIC, không có thì mã đầu."""
    if len(new) <= 1:
        return new[0] if new else None
    prefix = new[0]
    for c in new[1:]:
        while not c.startswith(prefix):
            prefix = prefix[:-1]
    while prefix and (prefix not in codes or len(prefix) == 1):
        prefix = prefix[:-1]
    return prefix if len(prefix) >= 2 else new[0]


out = [f"-- Sinh bởi ops/vsic/migrate_sql.py: {len(tree)} ngành VSIC 2025, {len(final)} ngành cũ"]
out.append(f"DROP TABLE IF EXISTS {BACKUP}.vsic_new;")
out.append(
    f"CREATE TABLE {BACKUP}.vsic_new (code VARCHAR(10) PRIMARY KEY, level INT, name VARCHAR(255), slug VARCHAR(255), "
    "descr LONGTEXT, parent VARCHAR(10), ord INT) COLLATE utf8mb4_0900_ai_ci;"
)
for i, r in enumerate(tree):
    assert len(r["name"]) <= 255, r["code"]
    slug = slugify(r["name"]) + "-" + r["code"].lower()
    out.append(
        f"INSERT INTO {BACKUP}.vsic_new VALUES ({q(r['code'])},{r['level']},{q(r['name'])},{q(slug)},"
        f"{q(r['desc'] or None)},{q(r['parent'])},{i});"
    )
out.append(f"DROP TABLE IF EXISTS {BACKUP}.category_redirect;")
out.append(
    f"CREATE TABLE {BACKUP}.category_redirect (old_id BIGINT PRIMARY KEY, old_slug VARCHAR(255), old_name VARCHAR(255), "
    "new_codes VARCHAR(200), new_code VARCHAR(10), new_category_id BIGINT, new_slug VARCHAR(255), KEY(old_slug)) "
    "COLLATE utf8mb4_0900_ai_ci;"
)
for r in final:
    out.append(
        f"INSERT INTO {BACKUP}.category_redirect (old_id, old_slug, old_name, new_codes, new_code) VALUES "
        f"({r['id']},{q(r['slug'])},{q(r['name'][:255])},{q(','.join(r['new']) or None)},{q(redirect_code(r['new']))});"
    )
open(f_out, "w", encoding="utf-8", newline="\n").write("\n".join(out) + "\n")
print("Ngành VSIC:", len(tree), "| ngành cũ:", len(final), "| có đích 301:", sum(1 for r in final if r["new"]))
print("Slug trùng:", len(tree) - len({slugify(r["name"]) + "-" + r["code"].lower() for r in tree}))
print("Ví dụ:", [slugify(r["name"]) + "-" + r["code"].lower() for r in tree[:3]])
