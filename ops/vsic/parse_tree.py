"""
Đọc cây "HỆ THỐNG NGÀNH KINH TẾ VIỆT NAM 2025" trong trang NganhNghe.aspx (dangkykinhdoanh.gov.vn) → vsic2025.json
Trang: mỗi mã là <span class="badge">MÃ</span> <span class="lbl">TÊN</span>, ngay sau đó (nếu có) là
<div class="meta">Chi tiết: <span class="txt">MÔ TẢ</span></div>, rồi mới tới các mã con. HTML lồng thẻ không chuẩn nên đọc
tuần tự: mô tả thuộc mã đứng ngay trước; mã cha suy từ chính mã (01110 → 0111 → 011 → 01 → chữ cái gần nhất phía trước).
Cách dùng: python -I parse_tree.py page.html vsic2025.json
"""
import html, json, re, sys
from collections import Counter

sys.stdout.reconfigure(encoding="utf-8")
src, out = sys.argv[1], sys.argv[2]
s = open(src, encoding="utf-8", errors="replace").read()
a = s.find('id="treeRoot"')
s = s[a:s.find("<script", a)]


def text(fragment: str) -> str:
    t = re.sub(r"<br\s*/?>", "\n", fragment)
    t = html.unescape(re.sub(r"<[^>]+>", "", t))
    t = re.sub(r"[ \t\r\xa0]+", " ", t)
    return "\n".join(x.strip() for x in t.split("\n")).strip()


tokens = re.finditer(
    r'<span class="badge">(?P<code>.*?)</span>\s*<span class="lbl">(?P<name>.*?)</span>|<span class="txt">(?P<txt>.*?)</span>\s*</div>',
    s,
    re.S,
)
rows, last, section = [], None, None
for m in tokens:
    if m.group("code") is not None:
        code, name = text(m.group("code")), text(m.group("name"))
        if re.fullmatch(r"[A-Z]", code):
            level, parent, section = 1, None, code
        elif re.fullmatch(r"\d{2,5}", code):
            level = len(code)
            parent = section if level == 2 else code[:-1]
        else:
            raise SystemExit(f"Mã lạ: {code!r}")
        last = {"level": level, "code": code, "name": name, "parent": parent, "section": section, "desc": ""}
        rows.append(last)
    else:
        last["desc"] = (last["desc"] + "\n" + text(m.group("txt"))).strip()

codes = Counter(r["code"] for r in rows)
known = set(codes)
print("Tổng:", len(rows), "| theo cấp:", dict(sorted(Counter(r["level"] for r in rows).items())))
print("Có mô tả:", sum(1 for r in rows if r["desc"]))
print("Trùng mã:", [c for c, n in codes.items() if n > 1])
print("Mã cha không tồn tại:", [r["code"] for r in rows if r["parent"] and r["parent"] not in known])
print("Không có con ở cấp < 5:", [r["code"] for r in rows if r["level"] < 5 and not any(x["parent"] == r["code"] for x in rows)][:20])
json.dump(rows, open(out, "w", encoding="utf-8"), ensure_ascii=False, indent=0)
for r in rows[:6] + rows[-2:]:
    print(r["level"], r["code"], r["parent"], r["name"][:60], "|", r["desc"][:50].replace("\n", " "))
