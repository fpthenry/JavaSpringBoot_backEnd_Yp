"""
Bảng ghép cuối: mỗi ngành hiện tại (category.id) → mã VSIC 2025.
  - Ngành có mã: code_map.tsv (theo bảng chuyển đổi chính thức) — trừ các mã không xác định được.
  - Còn lại (ngành riêng + mã không xác định): manual_map.txt (ghép theo nghĩa, cần duyệt).
python -I final_map.py vsic2025.json app_cat.tsv code_map.tsv manual_map.txt final_map.json
"""
import html, json, re, sys, unicodedata

sys.stdout.reconfigure(encoding="utf-8")
f_tree, f_cat, f_auto, f_manual, f_out = sys.argv[1:6]
v = {r["code"]: r for r in json.load(open(f_tree, encoding="utf-8"))}


def slugify(s):
    s = html.unescape(s).replace("đ", "d").replace("Đ", "D")
    s = "".join(c for c in unicodedata.normalize("NFD", s) if unicodedata.category(c) != "Mn").lower()
    return re.sub(r"[^a-z0-9]+", "-", s.replace("&", " va ")).strip("-")


lines = open(f_cat, encoding="utf-8").read().splitlines()
hdr = lines[0].split("\t")
cats = {r["id"]: r for r in (dict(zip(hdr, l.split("\t"))) for l in lines[1:])}
auto = {}
for l in open(f_auto, encoding="utf-8"):
    i, old, new, how = l.rstrip("\n").split("\t")
    if new:
        auto[i] = (new.split(","), how)
manual = {}
for l in open(f_manual, encoding="utf-8"):
    l = l.strip()
    if l and not l.startswith("#"):
        i, codes = l.split("|")
        manual[i] = codes

out = []
for i, c in cats.items():
    if i in auto:
        codes, how = auto[i]
    elif i in manual:
        codes = [] if manual[i] == "-" else manual[i].split(",")
        how = "Bỏ liên kết (không phải ngành nghề)" if not codes else "Ghép theo nghĩa tên ngành (cần duyệt)"
    else:
        raise SystemExit(f"Thiếu ngành {i} {c['name']}")
    fixed = []
    for x in codes:
        if x not in v:
            # bảng chuyển đổi (.docx) ghi mã khác danh mục trên trang (vd 0722 ↔ 0729 cùng tên): lấy theo danh mục, cùng cấp, cùng tên
            same = [y for y, r in v.items() if len(y) == len(x) and y[:3] == x[:3] and r["name"] == c["name"]]
            if len(same) != 1:
                raise SystemExit(f"Mã không có trong VSIC 2025: {x} (ngành {i})")
            how += f" — bảng chuyển đổi ghi {x}, danh mục trên trang là {same[0]}"
            x = same[0]
        fixed.append(x)
    codes = fixed
    out.append({
        "id": int(i), "name": html.unescape(c["name"]), "slug": c["slug"], "old_code": c["code"], "so_dn": int(c["so_dn"]),
        "new": codes, "new_names": [v[x]["name"] for x in codes],
        "new_slugs": [slugify(v[x]["name"]) + "-" + x.lower() for x in codes], "how": how,
    })
json.dump(out, open(f_out, "w", encoding="utf-8"), ensure_ascii=False)
from collections import Counter
print("Theo cách xác định:", Counter(r["how"] for r in out))
print("Số ngành ra nhiều mã:", sum(1 for r in out if len(r["new"]) > 1))
