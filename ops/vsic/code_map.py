"""
Bảng tra mã VSIC 2018 (và mã 2007 sót trong WordPress) → mã VSIC 2025 CÙNG CẤP, theo bảng chuyển đổi chính thức.
Quy tắc (theo thứ tự):
  1. Cùng dòng trong bảng chuyển đổi, cùng cấp → mã đó.
  2. Mã cấp < 5: lấy đích của các mã cấp 5 con (cùng dòng hoặc theo ghi chú), cắt về cùng cấp; một đích duy nhất → mã đó.
  3. Ghi chú: các mã xuất hiện sau "thành/chuyển/đến/sang/xuống/vào/mã" và có trong VSIC 2025, cắt/ghép về cùng cấp.
  4. Mã không có trong 2018 nhưng có trong 2025 → giữ; mã 2007 → tên trùng trong 2025 (cùng cấp).
Kết quả: code_map.tsv (old_code, new_codes cách nhau dấu phẩy, how).
python -I code_map.py vsic2025.json vsic2018_2025.docx app_cat.tsv code_map.tsv
"""
import html, json, re, sys, unicodedata, zipfile
from collections import Counter, defaultdict

sys.stdout.reconfigure(encoding="utf-8")
f_tree, f_docx, f_cat, f_out = sys.argv[1:5]
tree = json.load(open(f_tree, encoding="utf-8"))
by25 = {r["code"]: r for r in tree}
children25 = defaultdict(list)
for r in tree:
    if r["parent"]:
        children25[r["parent"]].append(r["code"])


def lvl(c):
    return 1 if re.fullmatch(r"[A-Z]", c) else len(c)


def norm_name(s):
    s = unicodedata.normalize("NFC", html.unescape(s or "")).lower()
    for old, new in {"uỷ": "ủy", "uý": "úy", "uỳ": "ùy", "uỹ": "ũy", "uỵ": "ụy", "oá": "óa", "oà": "òa", "oả": "ỏa", "oã": "õa", "oạ": "ọa"}.items():
        s = s.replace(old, new)
    return re.sub(r"\s+", " ", re.sub(r"[.;,]+$", "", s)).strip()


# ── bảng chuyển đổi, điền ô gộp dọc
xml = zipfile.ZipFile(f_docx).read("word/document.xml").decode("utf-8")


def cell_text(c):
    out = []
    for p in re.findall(r"<w:p[ >].*?</w:p>", c, re.S):
        t = html.unescape("".join(re.findall(r"<w:t(?: [^>]*)?>(.*?)</w:t>", p, re.S))).strip()
        if t:
            out.append(t)
    return "\n".join(out)


rows, above = [], {}
for tr in re.findall(r"<w:tr[ >].*?</w:tr>", re.findall(r"<w:tbl>.*?</w:tbl>", xml, re.S)[0], re.S):
    col, row = 0, []
    for tc in re.findall(r"<w:tc>.*?</w:tc>", tr, re.S):
        span = int((re.search(r'<w:gridSpan w:val="(\d+)"', tc) or [0, 1])[1])
        t = cell_text(tc)
        if re.search(r"<w:vMerge/>", tc):
            t = above.get(col, "")
        above[col] = t
        row.append(t)
        col += span
    rows.append(row)
conv = [r for r in rows[3:] if len(r) == 13 and any(r)][:-1]

name18, same_row, notes18 = {}, defaultdict(set), defaultdict(str)
for r in conv:
    L = [(i + 1, r[i]) for i in range(5) if r[i]]
    R = [(i + 1, r[6 + i]) for i in range(5) if r[6 + i]]
    for lv, c in L:
        name18.setdefault(c, r[5])
        for rl, rc in R:
            if rl == lv:
                same_row[c].add(rc)
        if r[12] and r[12] not in notes18[c]:
            notes18[c] += ("\n" if notes18[c] else "") + r[12]

KW = re.compile(r"(thành|chuyển|đến|sang|xuống|vào|mã)", re.I)


def note_targets(c):
    note = notes18.get(c, "")
    m = KW.search(note)
    if not m:
        return []
    found = [x for x in re.findall(r"(?<![\d])(\d{2,5})(?![\d])", note[m.start():]) if x in by25]
    return list(dict.fromkeys(found))


def at_level(code, k):
    """Đưa mã 2025 về cấp k: cắt tiền tố nếu sâu hơn; nếu nông hơn mà chỉ có 1 con ở cấp k thì lấy con đó."""
    if lvl(code) == k:
        return [code]
    if lvl(code) > k:
        p = code[:k]
        return [p] if p in by25 else []
    out = [code]
    while out and lvl(out[0]) < k:
        nxt = [ch for c in out for ch in children25[c]]
        out = nxt
    return out if len(out) == 1 else []


def map_code(c):
    k = lvl(c)
    if c in same_row:
        return sorted(same_row[c]), "Cùng dòng bảng chuyển đổi"
    if c in name18 and k < 5:
        kids = [x for x in name18 if lvl(x) == 5 and x.startswith(c)]
        targets = set()
        for kid in kids:
            t = same_row.get(kid) or set(note_targets(kid))
            for x in t:
                targets.update(at_level(x, k))
        if len(targets) == 1:
            return sorted(targets), "Theo các mã cấp 5 con"
        nt = [y for x in note_targets(c) for y in at_level(x, k)]
        if nt:
            return list(dict.fromkeys(nt)), "Theo ghi chú"
        if targets:
            return sorted(targets), "Theo các mã cấp 5 con (nhiều đích)"
        return [], "Không xác định được"
    if c in name18:
        nt = [y for x in note_targets(c) for y in at_level(x, k)]
        return (list(dict.fromkeys(nt)), "Theo ghi chú") if nt else ([], "Không xác định được")
    if c in by25:
        return [c], "Không có trong 2018, có trong 2025"
    return [], "Mã cũ VSIC 2007"


cats = [dict(zip(*[open(f_cat, encoding="utf-8").readline().rstrip("\n").split("\t")] * 1, l.rstrip("\n").split("\t"))) for l in []]
lines = open(f_cat, encoding="utf-8").read().splitlines()
hdr = lines[0].split("\t")
cats = [dict(zip(hdr, l.split("\t"))) for l in lines[1:]]
out = []
stat = Counter()
for c in cats:
    code = c["code"].strip()
    if not code:
        continue
    new, how = map_code(code)
    if not new:
        same = [r["code"] for r in tree if norm_name(r["name"]) == norm_name(c["name"]) and lvl(r["code"]) == lvl(code)]
        if same:
            new, how = same[:1], how + " → tên trùng cùng cấp"
    stat[(how, "co" if new else "KHONG", "1" if len(new) == 1 else ("n" if new else "0"))] += 1
    out.append((c["id"], code, ",".join(new), how))
with open(f_out, "w", encoding="utf-8") as f:
    for r in out:
        f.write("\t".join(r) + "\n")
for k, v in sorted(stat.items(), key=lambda x: -x[1]):
    print(v, k)
print("Không xác định:", [(code, n) for _, code, n, how in out if not n][:40])
print("Nhiều đích:", [(code, n) for _, code, n, how in out if "," in n][:30])
