"""
Tạo file Excel mã ngành VSIC 2025 + bảng chuyển đổi 2018→2025 + đối chiếu ngành đang có trong hệ thống.
Chỉ dùng thư viện chuẩn (tự ghi .xlsx). Cách dùng:
  python -I build_xlsx.py vsic2025.json vsic2018_2025.docx app_cat.tsv OUT.xlsx
"""
import html, json, re, sys, unicodedata, zipfile
from collections import Counter, defaultdict
from xml.sax.saxutils import escape

sys.stdout.reconfigure(encoding="utf-8")
f_tree, f_docx, f_cat, f_out = sys.argv[1:5]
TODAY = "29/10/2026"
SRC = "https://dangkykinhdoanh.gov.vn/vn/Pages/NganhNghe.aspx"

# ───────────── 1. VSIC 2025 ─────────────
tree = json.load(open(f_tree, encoding="utf-8"))
by25 = {r["code"]: r for r in tree}


def slugify(s: str) -> str:
    s = html.unescape(s).replace("đ", "d").replace("Đ", "D")
    s = unicodedata.normalize("NFD", s)
    s = "".join(c for c in s if unicodedata.category(c) != "Mn").lower()
    s = re.sub(r"&", " va ", s)
    return re.sub(r"[^a-z0-9]+", "-", s).strip("-")


def norm_name(s: str) -> str:
    """So tên: bỏ khác biệt hoa/thường, khoảng trắng, &amp;, kiểu bỏ dấu cũ/mới (thuỷ/thủy, hoá/hóa)."""
    s = unicodedata.normalize("NFC", html.unescape(s or "")).lower()
    for old, new in {"uỷ": "ủy", "uý": "úy", "uỳ": "ùy", "uỹ": "ũy", "uỵ": "ụy",
                     "oá": "óa", "oà": "òa", "oả": "ỏa", "oã": "õa", "oạ": "ọa",
                     "oé": "óe", "oè": "òe", "oẻ": "ỏe", "oẽ": "õe", "oẹ": "ọe"}.items():
        s = s.replace(old, new)
    return re.sub(r"\s+", " ", re.sub(r"[.;,]+$", "", s)).strip()


# ───────────── 2. Bảng chuyển đổi (docx), có xử lý ô gộp dọc ─────────────
xml = zipfile.ZipFile(f_docx).read("word/document.xml").decode("utf-8")


def cell_text(c):
    out = []
    for p in re.findall(r"<w:p[ >].*?</w:p>", c, re.S):
        t = "".join(re.findall(r"<w:t(?: [^>]*)?>(.*?)</w:t>", p, re.S))
        t = html.unescape(t).strip()
        if t:
            out.append(t)
    return "\n".join(out)


rows, above = [], {}
for tr in re.findall(r"<w:tr[ >].*?</w:tr>", re.findall(r"<w:tbl>.*?</w:tbl>", xml, re.S)[0], re.S):
    col, row = 0, []
    for tc in re.findall(r"<w:tc>.*?</w:tc>", tr, re.S):
        span = int((re.search(r'<w:gridSpan w:val="(\d+)"', tc) or [0, 1])[1])
        t = cell_text(tc)
        if re.search(r"<w:vMerge/>", tc) or re.search(r'<w:vMerge w:val="continue"', tc):
            t = above.get(col, "")
        above[col] = t
        row.append(t)
        col += span
    rows.append(row)
conv = [r for r in rows[3:] if len(r) == 13 and any(r)]
total_row = conv.pop()  # dòng tổng: 21 88 242 464 734 | 22 87 259 495 743
print("Dòng tổng:", total_row)


def side(r, off):
    codes = [(i + 1, r[off + i]) for i in range(5) if r[off + i]]
    return codes, r[off + 5]


official18 = {}
for r in conv:
    codes, name = side(r, 0)
    for lv, c in codes:
        official18.setdefault(c, name)

# Tra mã 2018 → 2025: cùng dòng (ưu tiên cùng cấp), không có thì lấy mã trong ghi chú "chuyển đến ngành ..."
map18 = defaultdict(list)
how18 = {}
note18 = {}
for r in conv:
    L, nameL = side(r, 0)
    R, nameR = side(r, 6)
    note = r[12]
    for lv, c in L:
        if note:
            note18.setdefault(c, note)
        if R:
            same = [rc for rl, rc in R if rl == lv]
            target = same[0] if same else R[-1][1]
            if target not in map18[c]:
                map18[c].append(target)
            how18.setdefault(c, "Cùng dòng trong bảng chuyển đổi")
        else:
            m = re.search(r"chuyển (?:đến|sang|vào)[^0-9]*((?:\d{2,5}(?:\s*,\s*|\s+và\s+)?)+)", note or "")
            if m:
                for t in re.findall(r"\d{2,5}", m.group(1)):
                    if t not in map18[c]:
                        map18[c].append(t)
                how18[c] = "Theo ghi chú (chuyển ngành)"
            else:
                how18.setdefault(c, "Không có mã tương ứng" if not note else "Xem ghi chú")

# ───────────── 3. Ngành trong hệ thống hiện tại ─────────────
lines = open(f_cat, encoding="utf-8").read().splitlines()
hdr = lines[0].split("\t")
cats = [dict(zip(hdr, l.split("\t"))) for l in lines[1:]]
by_wp = {c["wp_term_id"]: c for c in cats}


def depth(c):
    d, seen = 1, set()
    while c["parent_wp"] not in ("NULL", "") and c["parent_wp"] in by_wp and c["parent_wp"] not in seen:
        seen.add(c["parent_wp"]); c = by_wp[c["parent_wp"]]; d += 1
    return d


status_count = Counter()
name_hint = Counter()
cat_rows = []
for c in cats:
    code = c["code"].strip()
    name = html.unescape(c["name"])
    new_codes, st, new_name = [], "", ""
    if not code:
        st = "Ngành riêng của YP (không theo VSIC)"
    else:
        mapped = map18.get(code)
        if code in official18 and mapped:
            new_codes = mapped
            if mapped == [code]:
                n25 = by25.get(code, {}).get("name", "")
                st = "Giữ mã, giữ tên" if norm_name(n25) == norm_name(name) else "Giữ mã, tên khác VSIC 2025"
            elif len(mapped) == 1:
                st = "Đổi mã"
            else:
                st = "Tách thành nhiều mã"
        elif code in official18:
            st = "Bỏ trong VSIC 2025 (xem ghi chú)"
        elif code in by25:
            new_codes = [code]
            n25 = by25[code]["name"]
            st = "Không có trong VSIC 2018, có trong 2025" + ("" if norm_name(n25) == norm_name(name) else " (khác tên)")
        else:
            st = "Mã cũ (VSIC 2007), không còn dùng"
        if not new_codes:
            # gợi ý theo tên trùng khớp trong VSIC 2025 (ưu tiên cùng cấp) — cần người duyệt
            same = [r["code"] for r in tree if norm_name(r["name"]) == norm_name(name)]
            same.sort(key=lambda x: (len(x) != len(code), len(x)))
            if same:
                new_codes = same[:1]
                how18[code + "#name"] = "Gợi ý theo tên trùng (cần duyệt)"
        new_name = " | ".join(by25[x]["name"] for x in new_codes if x in by25)
    status_count[st] += 1
    cat_rows.append([
        int(c["id"]), int(c["wp_term_id"]), depth(c), name, c["slug"], code, int(c["so_dn"]), st,
        ", ".join(new_codes), new_name,
        ", ".join(slugify(by25[x]["name"]) + "-" + x.lower() for x in new_codes if x in by25),
        (how18.get(code + "#name") or how18.get(code, "")) if code else "", note18.get(code, "") if code else "",
    ])
    if code and how18.get(code + "#name"):
        name_hint[st] += 1
print("Đối chiếu ngành hệ thống:", dict(status_count), "| gợi ý theo tên:", dict(name_hint))

# ───────────── 4. Ghi xlsx ─────────────
def col_letter(i):
    s = ""
    i += 1
    while i:
        i, r = divmod(i - 1, 26)
        s = chr(65 + r) + s
    return s


def sheet_xml(header, data, widths, wrap_cols=()):
    out = ['<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',
           '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">']
    if header:
        out.append('<sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>')
    out.append("<cols>" + "".join(f'<col min="{i+1}" max="{i+1}" width="{w}" customWidth="1"/>' for i, w in enumerate(widths)) + "</cols>")
    out.append("<sheetData>")
    all_rows = ([header] if header else []) + data
    for ri, row in enumerate(all_rows, 1):
        cells = []
        for ci, v in enumerate(row):
            ref = f"{col_letter(ci)}{ri}"
            style = 1 if (header and ri == 1) else (2 if ci in wrap_cols else 0)
            if v is None or v == "":
                continue
            if isinstance(v, (int, float)) and not isinstance(v, bool):
                cells.append(f'<c r="{ref}" s="{style}"><v>{v}</v></c>')
            else:
                t = escape(str(v)).replace("\n", "&#10;")
                cells.append(f'<c r="{ref}" s="{style}" t="inlineStr"><is><t xml:space="preserve">{t}</t></is></c>')
        out.append(f'<row r="{ri}">' + "".join(cells) + "</row>")
    out.append("</sheetData>")
    if header:
        out.append(f'<autoFilter ref="A1:{col_letter(len(header)-1)}{len(all_rows)}"/>')
    out.append("</worksheet>")
    return "".join(out)


sheets = []

# Sheet: Hướng dẫn
lv = Counter(r["level"] for r in tree)
guide = [
    ["HỆ THỐNG NGÀNH KINH TẾ VIỆT NAM 2025 (VSIC 2025)"],
    ["Ban hành kèm Quyết định số 36/2025/QĐ-TTg ngày 29/9/2025 của Thủ tướng Chính phủ (thay VSIC 2018 – QĐ 27/2018/QĐ-TTg)."],
    [f"Nguồn: {SRC} (lấy ngày {TODAY}); bảng chuyển đổi: file \"Bảng so sánh VSIC 2018 - 2025\" trên cùng trang."],
    [""],
    [f"Số ngành VSIC 2025: cấp 1: {lv[1]}, cấp 2: {lv[2]}, cấp 3: {lv[3]}, cấp 4: {lv[4]}, cấp 5: {lv[5]} (tổng {len(tree)}). Khớp dòng tổng của bảng chuyển đổi."],
    [""],
    ["Các sheet:"],
    ["• VSIC 2025: toàn bộ mã ngành, tên, mã cha, ngành cấp 1, mô tả chi tiết (nội dung bao gồm/loại trừ), slug gợi ý = tên ngành + mã."],
    ["• Chuyển đổi 2018-2025: bảng so sánh gốc của cơ quan đăng ký kinh doanh (mỗi dòng: mã/tên VSIC 2018 | mã/tên VSIC 2025 | ghi chú)."],
    ["• Tra mã 2018→2025: mỗi mã VSIC 2018 → mã VSIC 2025 tương ứng (lấy theo cùng dòng, hoặc theo ghi chú khi ngành bị chuyển)."],
    ["• Ngành hệ thống hiện tại: 2.394 ngành nghề đang có trong database (lấy từ WordPress), mã đang gắn (VSIC 2018), trạng thái so với VSIC 2025, mã/tên/slug mới gợi ý."],
    [""],
    ["Trạng thái ngành hệ thống:"],
] + [[f"• {k}: {v}" + (f" (trong đó {name_hint[k]} có mã gợi ý theo tên trùng, cần duyệt)" if name_hint[k] else "")] for k, v in status_count.most_common()] + [
    [""],
    ["Lưu ý: ngành cấp 1 đổi chữ cái từ J trở đi (VSIC 2025 tách J cũ thành J và K, U cũ thành V), nên slug ngành cấp 1 kết thúc bằng chữ cái sẽ đổi."],
    ["File này chỉ để đối chiếu; chưa thay đổi dữ liệu trong hệ thống."],
]
sheets.append(("Hướng dẫn", sheet_xml(None, guide, [160])))

# Sheet: VSIC 2025
sec_name = {r["code"]: r["name"] for r in tree if r["level"] == 1}
data = []
for r in tree:
    parent = by25.get(r["parent"]) if r["parent"] else None
    data.append([r["level"], r["code"], r["name"], r["parent"] or "", parent["name"] if parent else "",
                 r["section"], sec_name.get(r["section"], ""), slugify(r["name"]) + "-" + r["code"].lower(), r["desc"]])
sheets.append(("VSIC 2025", sheet_xml(
    ["Cấp", "Mã ngành", "Tên ngành", "Mã cha", "Tên ngành cha", "Ngành cấp 1", "Tên ngành cấp 1", "Slug gợi ý (tên + mã)", "Mô tả chi tiết"],
    data, [6, 10, 60, 9, 45, 8, 40, 60, 90], wrap_cols=(8,))))

# Sheet: Chuyển đổi gốc
data = []
for r in conv:
    L, _ = side(r, 0)
    R, _ = side(r, 6)
    data.append([L[-1][1] if L else "", L[-1][0] if L else "", *r[0:6], R[-1][1] if R else "", R[-1][0] if R else "", *r[6:13]])
sheets.append(("Chuyển đổi 2018-2025", sheet_xml(
    ["Mã 2018 (chi tiết nhất)", "Cấp 2018", "2018 Cấp 1", "2018 Cấp 2", "2018 Cấp 3", "2018 Cấp 4", "2018 Cấp 5", "Tên ngành VSIC 2018",
     "Mã 2025 (chi tiết nhất)", "Cấp 2025", "2025 Cấp 1", "2025 Cấp 2", "2025 Cấp 3", "2025 Cấp 4", "2025 Cấp 5", "Tên ngành VSIC 2025", "Ghi chú sửa đổi"],
    data, [12, 8, 7, 7, 7, 8, 8, 55, 12, 8, 7, 7, 7, 8, 8, 55, 70], wrap_cols=(16,))))

# Sheet: Tra mã 2018 → 2025
data = []
for c, n in official18.items():
    targets = map18.get(c, [])
    data.append([c, len(c) if c.isdigit() else 1, n, ", ".join(targets),
                 " | ".join(by25[t]["name"] for t in targets if t in by25),
                 "Giữ nguyên" if targets == [c] else ("Đổi/tách mã" if targets else "Không có mã tương ứng"),
                 how18.get(c, ""), note18.get(c, "")])
sheets.append(("Tra mã 2018→2025", sheet_xml(
    ["Mã VSIC 2018", "Cấp", "Tên ngành 2018", "Mã VSIC 2025", "Tên ngành 2025", "Kết quả", "Cách xác định", "Ghi chú sửa đổi"],
    data, [12, 6, 55, 16, 55, 18, 28, 70], wrap_cols=(7,))))

# Sheet: Ngành hệ thống hiện tại
sheets.append(("Ngành hệ thống hiện tại", sheet_xml(
    ["id (backend)", "wp_term_id", "Cấp cây", "Tên ngành hiện tại", "Slug hiện tại", "Mã hiện tại (VSIC 2018 từ WordPress)", "Số doanh nghiệp",
     "Trạng thái so với VSIC 2025", "Mã VSIC 2025 gợi ý", "Tên VSIC 2025", "Slug gợi ý (tên + mã)", "Cách xác định", "Ghi chú sửa đổi"],
    cat_rows, [10, 10, 7, 50, 55, 14, 12, 34, 16, 55, 60, 26, 60], wrap_cols=(12,))))

# Sheet: Bảng ghép cuối (để duyệt trước khi chuyển dữ liệu) — có khi truyền thêm final_map.json
if len(sys.argv) > 6:
    fm = json.load(open(sys.argv[6], encoding="utf-8"))
    order = {"Ghép theo nghĩa tên ngành (cần duyệt)": 0, "Bỏ liên kết (không phải ngành nghề)": 1}
    fm.sort(key=lambda r: (order.get(r["how"], 2), -r["so_dn"]))
    data = [[r["id"], r["name"], r["slug"], r["old_code"], r["so_dn"], ", ".join(r["new"]), " | ".join(r["new_names"]),
             ", ".join(r["new_slugs"]), r["how"]] for r in fm]
    sheets.insert(1, ("Bảng ghép (duyệt)", sheet_xml(
        ["id (backend)", "Tên ngành hiện tại", "Slug hiện tại (sẽ 301 sang slug mới)", "Mã hiện tại", "Số doanh nghiệp",
         "Mã VSIC 2025 sẽ gắn", "Tên VSIC 2025", "Slug mới (tên + mã)", "Cách xác định"],
        data, [10, 50, 50, 10, 12, 14, 55, 60, 45])))
    guide.insert(11, ["• Bảng ghép (duyệt): mỗi ngành hiện tại sẽ chuyển sang mã VSIC 2025 nào. Các dòng \"Ghép theo nghĩa\" và \"Bỏ liên kết\" xếp đầu, cần duyệt; sửa cột \"Mã VSIC 2025 sẽ gắn\" nếu chưa đúng rồi gửi lại."])
    sheets[0] = ("Hướng dẫn", sheet_xml(None, guide, [160]))

styles = ('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
          '<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">'
          '<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>'
          '<fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill>'
          '<fill><patternFill patternType="solid"><fgColor rgb="FFDDEBF7"/><bgColor indexed="64"/></patternFill></fill></fills>'
          '<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>'
          '<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>'
          '<cellXfs count="3"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>'
          '<xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"><alignment wrapText="1" vertical="center"/></xf>'
          '<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0" applyAlignment="1"><alignment wrapText="1" vertical="top"/></xf></cellXfs>'
          '</styleSheet>')
with zipfile.ZipFile(f_out, "w", zipfile.ZIP_DEFLATED) as z:
    z.writestr("[Content_Types].xml",
               '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
               '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/>'
               '<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>'
               '<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>'
               + "".join(f'<Override PartName="/xl/worksheets/sheet{i}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>' for i in range(1, len(sheets) + 1))
               + "</Types>")
    z.writestr("_rels/.rels", '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
               '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>')
    z.writestr("xl/workbook.xml", '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" '
               'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>'
               + "".join(f'<sheet name="{escape(n)}" sheetId="{i}" r:id="rId{i}"/>' for i, (n, _) in enumerate(sheets, 1))
               + "</sheets></workbook>")
    z.writestr("xl/_rels/workbook.xml.rels", '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
               + "".join(f'<Relationship Id="rId{i}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet{i}.xml"/>' for i in range(1, len(sheets) + 1))
               + f'<Relationship Id="rId{len(sheets)+1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>')
    z.writestr("xl/styles.xml", styles)
    for i, (_, x) in enumerate(sheets, 1):
        z.writestr(f"xl/worksheets/sheet{i}.xml", x)
print("Đã ghi", f_out, "| sheet:", [n for n, _ in sheets])
print("Tra 2018→2025:", Counter(("giu" if v == [k] else "doi") for k, v in map18.items()), "| không có đích:", sum(1 for c in official18 if not map18.get(c)))

if len(sys.argv) > 5:  # tùy chọn: xuất kết quả đối chiếu ngành hệ thống ra JSON
    json.dump([{"id": r[0], "code": r[5], "status": r[7], "new": r[8], "how": r[11]} for r in cat_rows],
              open(sys.argv[5], "w", encoding="utf-8"), ensure_ascii=False)
