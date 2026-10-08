// Chuyển bảng chuyển đổi đơn vị hành chính cũ → mới (file Excel, sheet "Tổng hợp_không merge") thành CSV cho Liquibase.
// Không cần thư viện: .xlsx là các file XML nén, giải nén bằng lệnh unzip (có sẵn trong Git Bash).
//
//   node db-sync/dvhc/excel-to-csv.mjs "BangChuyendoiĐVHCmoi_cu_final.xlsx" src/main/resources/config/liquibase/data/location_conversion.csv
//
// Tự sửa lỗi của file Excel:
//   - Mã bị mất số 0 đầu (Excel lưu thành số): xã 5 chữ số, huyện 3, tỉnh 2.
//   - Tên tỉnh cũ không khớp mã nhưng khớp tên của mã khác: sửa mã theo tên (huyện Cồn Cỏ ghi "Quảng Trị (44)", 44 là
//     Quảng Bình cũ, Quảng Trị cũ là 45).
//   - Tỉnh cũ ghi sai: lấy theo tỉnh của đa số dòng cùng mã huyện cũ (ví dụ Phường 4, TP Tân An (794) ghi nhầm Tiền Giang (82), đúng là Long An (80)).
//   - Tên tỉnh cũ ghi không thống nhất ("Quảng Trị" / "Tỉnh Quảng Trị"): lấy tên xuất hiện nhiều nhất của mã tỉnh đó.
//   - Tiền tố loại đơn vị viết sai: "Xa Ea Kly", "phường Vĩnh Hải", "Thi trấn Ngan Dừa", "Thị Trấn Yên Minh"
//     → "Xã", "Phường", "Thị trấn", "Đặc khu".
//   - Dấu nháy cong (’) thành nháy thẳng ('): "Xã Ea M’Droh" → "Xã Ea M'Droh".
//   - Khoảng trắng thừa, chữ Unicode tổ hợp (chuyển về NFC).
// Dòng không có mã xã cũ là hợp lệ: cả huyện đảo thành đặc khu (Bạch Long Vĩ, Cồn Cỏ, Hoàng Sa, Lý Sơn...), vùng bãi bồi
// chưa có mã. Các dòng đó ánh xạ ở cấp huyện cũ.
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';

const [file, out] = process.argv.slice(2);
if (!file || !out) {
  console.error('Cách dùng: node excel-to-csv.mjs <file.xlsx> <output.csv>');
  process.exit(1);
}

const unzip = name => execFileSync('unzip', ['-p', file, name], { maxBuffer: 1 << 30 }).toString('utf8');
const decodeXml = s =>
  s
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&#(\d+);/g, (_, n) => String.fromCodePoint(+n))
    .replace(/&amp;/g, '&');
const shared = [...unzip('xl/sharedStrings.xml').matchAll(/<si>([\s\S]*?)<\/si>/g)].map(m =>
  decodeXml([...m[1].matchAll(/<t[^>]*>([\s\S]*?)<\/t>/g)].map(t => t[1]).join('')),
);
const workbook = unzip('xl/workbook.xml');
const rels = unzip('xl/_rels/workbook.xml.rels');
const sheetEntry = [...workbook.matchAll(/<sheet [^>]*name="([^"]+)"[^>]*r:id="([^"]+)"/g)].find(m =>
  decodeXml(m[1]).includes('không merge'),
);
if (!sheetEntry) throw new Error('Không thấy sheet "Tổng hợp_không merge"');
const target = rels.match(new RegExp(`Id="${sheetEntry[2]}"[^>]*Target="([^"]+)"`))[1];
const xml = unzip('xl/' + target.replace(/^\/?xl\//, ''));
const colIndex = ref => [...ref.match(/^[A-Z]+/)[0]].reduce((n, c) => n * 26 + c.charCodeAt(0) - 64, 0) - 1;
const sheet = [...xml.matchAll(/<row[^>]*>([\s\S]*?)<\/row>/g)].map(row => {
  const cells = [];
  for (const c of row[1].matchAll(/<c ([^>]*?)(?:\/>|>([\s\S]*?)<\/c>)/g)) {
    const ref = c[1].match(/r="([A-Z]+\d+)"/)?.[1];
    const type = c[1].match(/t="([^"]+)"/)?.[1];
    let value = (c[2] ?? '').match(/<v>([\s\S]*?)<\/v>/)?.[1] ?? null;
    if (type === 's' && value !== null) value = shared[+value];
    else if (value !== null) value = decodeXml(value);
    if (ref) cells[colIndex(ref)] = value;
  }
  return cells;
});

const clean = s => (s ?? '').normalize('NFC').replace(/[’‘]/g, "'").replace(/\s+/g, ' ').trim();
const UNIT_PREFIXES = [
  [/^(xa|xã)\s+/i, 'Xã '],
  [/^phường\s+/i, 'Phường '],
  [/^(thị|thi)\s+trấn\s+/i, 'Thị trấn '],
  [/^đặc\s+khu\s+/i, 'Đặc khu '],
];
let prefixFixes = 0;
/** Tên xã: chuẩn hóa tiền tố loại đơn vị. */
const wardName = s => {
  const name = clean(s);
  for (const [pattern, prefix] of UNIT_PREFIXES) {
    if (pattern.test(name)) {
      const fixed = name.replace(pattern, prefix);
      if (fixed !== name) {
        prefixFixes++;
      }
      return fixed;
    }
  }
  return name;
};
/** "Thành phố Hà Nội (01)" → ["Thành phố Hà Nội", "01"] */
const nameAndCode = s => {
  const m = clean(s).match(/^(.*?)\s*\((\d+)\)$/);
  return m ? [m[1], m[2]] : [clean(s), null];
};
const pad = (code, n) => (code ? String(code).trim().padStart(n, '0') : null);

const rows = [];
for (const r of sheet.slice(2)) {
  if (!r[1] && !r[3]) continue; // dòng tiêu đề tỉnh
  const [newProvinceName, newProvinceCode] = nameAndCode(r[0]);
  const [oldDistrictName, oldDistrictCode] = nameAndCode(r[6]);
  const [oldProvinceName, oldProvinceCode] = nameAndCode(r[7]);
  const note = clean(r[5]);
  rows.push({
    new_province_code: pad(newProvinceCode, 2),
    new_province_name: newProvinceName,
    new_ward_code: pad(clean(r[2]), 5),
    new_ward_name: wardName(r[1]),
    old_ward_code: pad(clean(r[4]), 5),
    old_ward_name: wardName(r[3]),
    old_district_code: pad(oldDistrictCode, 3),
    old_district_name: oldDistrictName,
    old_province_code: pad(oldProvinceCode, 2),
    old_province_name: oldProvinceName,
    note,
    // Nhập một phần / tách một phần: xã cũ có thể thuộc nhiều xã mới
    partial_merge: /một phần|phần còn lại/i.test(note),
  });
}

// Sửa mã tỉnh cũ theo tên khi tên và mã mâu thuẫn
const baseName = n => n.replace(/^(tỉnh|thành phố)\s+/i, '').toLowerCase();
const majorityName = new Map();
for (const r of rows) {
  const names = majorityName.get(r.old_province_code) ?? new Map();
  names.set(baseName(r.old_province_name), (names.get(baseName(r.old_province_name)) ?? 0) + 1);
  majorityName.set(r.old_province_code, names);
}
const nameOf = code => [...majorityName.get(code).entries()].sort((a, b) => b[1] - a[1])[0][0];
const codeByName = new Map([...majorityName.keys()].map(code => [nameOf(code), code]));
let codeFixes = 0;
for (const r of rows) {
  const name = baseName(r.old_province_name);
  const byName = codeByName.get(name);
  if (name !== nameOf(r.old_province_code) && byName && byName !== r.old_province_code) {
    console.log(
      `Sửa mã tỉnh cũ theo tên: ${r.old_ward_name || r.old_district_name}: ${r.old_province_name} (${r.old_province_code}) → (${byName})`,
    );
    r.old_province_code = byName;
    codeFixes++;
  }
}

// Sửa tỉnh cũ theo đa số dòng cùng huyện cũ
const byDistrict = new Map();
for (const r of rows) {
  const counts = byDistrict.get(r.old_district_code) ?? new Map();
  const key = `${r.old_province_code}|${r.old_province_name}`;
  counts.set(key, (counts.get(key) ?? 0) + 1);
  byDistrict.set(r.old_district_code, counts);
}
let provinceFixes = 0;
for (const r of rows) {
  const [best] = [...byDistrict.get(r.old_district_code).entries()].sort((a, b) => b[1] - a[1]);
  const [code, name] = best[0].split('|');
  if (code !== r.old_province_code) {
    console.log(
      `Sửa tỉnh cũ: ${r.old_ward_name} (${r.old_district_name} ${r.old_district_code}): ${r.old_province_name} (${r.old_province_code}) → ${name} (${code})`,
    );
    r.old_province_code = code;
    r.old_province_name = name;
    provinceFixes++;
  }
}

// Thống nhất tên tỉnh cũ theo mã
const provinceNames = new Map();
for (const r of rows) {
  const names = provinceNames.get(r.old_province_code) ?? new Map();
  names.set(r.old_province_name, (names.get(r.old_province_name) ?? 0) + 1);
  provinceNames.set(r.old_province_code, names);
}
for (const r of rows) {
  r.old_province_name = [...provinceNames.get(r.old_province_code).entries()].sort((a, b) => b[1] - a[1])[0][0];
}

const invalid = rows.filter(
  r =>
    !/^\d{2}$/.test(r.new_province_code ?? '') ||
    !/^\d{5}$/.test(r.new_ward_code ?? '') ||
    (r.old_ward_code !== null && !/^\d{5}$/.test(r.old_ward_code)) ||
    !/^\d{3}$/.test(r.old_district_code ?? '') ||
    !/^\d{2}$/.test(r.old_province_code ?? ''),
);
if (invalid.length) {
  console.error('Dòng thiếu mã:', invalid.slice(0, 5));
  process.exit(1);
}

const columns = Object.keys(rows[0]);
// Ô trống ghi NULL để Liquibase nạp thành NULL (không phải chuỗi rỗng)
const csvValue = v => (typeof v === 'boolean' ? String(v) : v === null || v === '' ? 'NULL' : `"${String(v).replace(/"/g, '""')}"`);
const csv = [columns.join(';'), ...rows.map(r => columns.map(c => csvValue(r[c])).join(';'))].join('\n') + '\n';
fs.writeFileSync(out, csv);
const count = key => new Set(rows.map(r => r[key])).size;
console.log(
  `Ghi ${rows.length} dòng vào ${out}: ${count('new_province_code')} tỉnh mới, ${count('new_ward_code')} xã mới, ` +
    `${count('old_province_code')} tỉnh cũ, ${count('old_district_code')} huyện cũ, ${count('old_ward_code')} xã cũ; ` +
    `${rows.filter(r => r.partial_merge).length} dòng nhập một phần; ${rows.filter(r => !r.old_ward_code).length} dòng cấp huyện (không có xã cũ); ` +
    `sửa ${codeFixes + provinceFixes} tỉnh cũ ghi sai, ${prefixFixes} tiền tố tên xã.`,
);
