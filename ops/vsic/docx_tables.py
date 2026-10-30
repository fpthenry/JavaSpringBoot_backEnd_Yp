"""Đọc mọi bảng trong file .docx (chỉ thư viện chuẩn) → JSON [[ [ô, ô, ...], ... ] mỗi bảng]. python -I docx_tables.py X.docx out.json"""
import json, re, sys, zipfile, html
sys.stdout.reconfigure(encoding="utf-8")
x = zipfile.ZipFile(sys.argv[1]).read("word/document.xml").decode("utf-8")
def cell_text(c):
    paras = re.findall(r"<w:p[ >].*?</w:p>", c, re.S)
    out = []
    for p in paras:
        t = "".join(re.findall(r"<w:t(?: [^>]*)?>(.*?)</w:t>", p, re.S))
        out.append(html.unescape(t).strip())
    return "\n".join(o for o in out if o)
tables = []
for tbl in re.findall(r"<w:tbl>.*?</w:tbl>", x, re.S):
    rows = []
    for tr in re.findall(r"<w:tr[ >].*?</w:tr>", tbl, re.S):
        cells = re.findall(r"<w:tc>.*?</w:tc>", tr, re.S)
        rows.append([cell_text(c) for c in cells])
    tables.append(rows)
json.dump(tables, open(sys.argv[2], "w", encoding="utf-8"), ensure_ascii=False)
print("So bang:", len(tables), "| so dong:", [len(t) for t in tables][:20])
for t in tables[:2]:
    for r in t[:12]:
        print(r)
    print("...")
    for r in t[-3:]:
        print(r)
# văn bản ngoài bảng ở đầu file
body = re.sub(r"<w:tbl>.*?</w:tbl>", "", x, flags=re.S)
print("TEXT:", " ".join(html.unescape(t) for t in re.findall(r"<w:t(?: [^>]*)?>(.*?)</w:t>", body))[:800])
