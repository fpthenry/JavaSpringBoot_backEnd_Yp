"""
Cập nhật tên tỉnh mới, xã mới trong bảng chuyển đổi (src/main/resources/config/liquibase/data/location_conversion.csv)
theo danh mục ĐVHC chính thức mới nhất (file .xls "Danh sách tất cả các cấp", xuất ra CSV UTF-8 bằng Excel).

Danh mục có thay đổi sau 01/7/2025 (tỉnh lên thành phố, xã lên phường...). Bảng chuyển đổi là "danh mục gốc" của
API sửa bảng location (POST /api/admin/administrative-units/fix-locations), nên sửa ở đây rồi chạy lại API đó.

Quy ước giữ như db-sync/dvhc/excel-to-csv.mjs: nháy thẳng ('), tiền tố viết hoa (Xã/Phường), gộp khoảng trắng.
Mỗi tỉnh trong file có 2 dòng: dòng đầu (cột "Mã TP" trống) là tên hiện hành, dòng sau là tên năm 2025.

python -I db-sync/dvhc/update-names-from-official.py <danh-muc.csv> src/main/resources/config/liquibase/data/location_conversion.csv
"""
import csv
import io
import re
import sys
import unicodedata

sys.stdout.reconfigure(encoding="utf-8")
f_official, f_conv = sys.argv[1:3]


def clean(s):
    s = unicodedata.normalize("NFC", s or "").replace("’", "'").replace("‘", "'")
    s = re.sub(r"\s+", " ", s).strip()
    return re.sub(r"^(xã|phường|đặc khu|tỉnh|thành phố)\b", lambda m: m.group(1)[0].upper() + m.group(1)[1:], s)


provinces, wards = {}, {}
for row in list(csv.reader(open(f_official, encoding="utf-8-sig")))[1:]:
    code, name, _en, level, pcode = [(row + [""] * 5)[i].strip() for i in range(5)]
    if not code:
        continue
    if level in ("Tỉnh", "Thành phố Trung ương"):
        if not pcode:
            provinces[code] = clean(name)
    else:
        wards[code] = clean(name)
print(f"Danh mục: {len(provinces)} tỉnh, {len(wards)} xã")

text = open(f_conv, encoding="utf-8").read()
reader = csv.reader(io.StringIO(text), delimiter=";")
header = next(reader)
rows = list(reader)
col = {n: i for i, n in enumerate(header)}
changed_p, changed_w = {}, {}
for r in rows:
    pc, wc = r[col["new_province_code"]], r[col["new_ward_code"]]
    if pc in provinces and r[col["new_province_name"]] != provinces[pc]:
        changed_p[pc] = (r[col["new_province_name"]], provinces[pc])
        r[col["new_province_name"]] = provinces[pc]
    if wc in wards and r[col["new_ward_name"]] != wards[wc]:
        changed_w[wc] = (r[col["new_ward_name"]], wards[wc])
        r[col["new_ward_name"]] = wards[wc]
missing = sorted({r[col["new_ward_code"]] for r in rows} - set(wards))
print(f"Đổi tên tỉnh: {len(changed_p)}, đổi tên xã: {len(changed_w)}; mã xã trong bảng chuyển đổi không có trong danh mục: {missing}")
for code, (old, new) in sorted(changed_p.items()) + sorted(changed_w.items()):
    print(f"  {code}: {old} → {new}")

# Ghi lại đúng định dạng cũ: dấu ; , mọi ô trong nháy kép, cột partial_merge (true/false) không nháy
out = io.StringIO()
out.write(";".join(header) + "\n")
for r in rows:
    out.write(";".join(v if i == col["partial_merge"] else '"' + v.replace('"', '""') + '"' for i, v in enumerate(r)) + "\n")
open(f_conv, "w", encoding="utf-8", newline="\n").write(out.getvalue())
