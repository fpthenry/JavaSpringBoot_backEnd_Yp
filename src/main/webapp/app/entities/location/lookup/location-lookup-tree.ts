import { LocationNodeItem } from '../service/location-index.service';

/** Một nút trong cây tra cứu đơn vị hành chính. */
export interface LocationNode {
  item: LocationNodeItem;
  parent: LocationNode | null;
  children: LocationNode[];
  /** Tên bỏ dấu, chữ thường, tách từ — để tìm không dấu theo đầu từ. */
  words: string[];
  /** Loại theo tiền tố tên: Tỉnh, Thành phố, Quận, Huyện, Thị xã, Phường, Xã, Thị trấn, Đặc khu. */
  kind: string | null;
}

export interface LocationTree {
  roots: LocationNode[];
  byId: Map<number, LocationNode>;
}

export type LocationSort = 'code' | 'name';

/** all: mọi đơn vị; listings: chỉ nơi có doanh nghiệp; unmapped: không có trong bảng chuyển đổi 2025. */
export type LocationFilter = 'all' | 'listings' | 'unmapped';

/** Kết quả tìm/lọc: nút khớp, nút còn hiện (khớp + cha + con cháu), nút mở sẵn. */
export interface LocationSearch {
  matches: Set<number>;
  keep: Set<number>;
  expand: Set<number>;
}

const KIND = /^(thành phố|tỉnh|quận|huyện|thị xã|phường|xã|thị trấn|đặc khu)\s+/i;

/** "Phường Ba Đình" → "Phường"; "Thành phố Thủ Đức" → "Thành phố". */
export function kindOf(name: string): string | null {
  const m = KIND.exec(name.trim());
  return m ? m[1].charAt(0).toLocaleUpperCase('vi') + m[1].slice(1).toLocaleLowerCase('vi') : null;
}

/** "Phường Ba Đình" → "Ba Đình". */
export function properName(name: string): string {
  return name.trim().replace(KIND, '');
}

/** "Thủ  Đức" → "thu duc". */
export function foldText(text: string | null | undefined): string {
  return (text ?? '').normalize('NFD').replace(/\p{M}/gu, '').replace(/[đĐ]/g, 'd').toLowerCase().replace(/\s+/g, ' ').trim();
}

function foldWords(text: string): string[] {
  return foldText(text)
    .split(/[\s\-–,()'’]+/)
    .filter(Boolean);
}

/**
 * Dựng cây từ danh sách phẳng; con sắp theo mã ĐVHC (thứ tự chính thức) hoặc theo tên riêng tiếng Việt (bỏ "Phường",
 * "Xã"...). Cha không có trong danh sách thì là gốc; cây lỗi có vòng lặp cũng không treo.
 */
export function buildLocationTree(items: LocationNodeItem[], sort: LocationSort): LocationTree {
  const byId = new Map<number, LocationNode>();
  for (const item of items) {
    byId.set(item.id, { item, parent: null, children: [], words: foldWords(item.name), kind: kindOf(item.name) });
  }
  const roots: LocationNode[] = [];
  for (const node of byId.values()) {
    const parent = node.item.parentId === null ? undefined : byId.get(node.item.parentId);
    if (parent && !isAncestorOrSelf(node, parent)) {
      node.parent = parent;
      parent.children.push(node);
    } else {
      roots.push(node);
    }
  }
  const compare =
    sort === 'code'
      ? (a: LocationNode, b: LocationNode) => a.item.code.localeCompare(b.item.code) || a.item.id - b.item.id
      : (a: LocationNode, b: LocationNode) =>
          properName(a.item.name).localeCompare(properName(b.item.name), 'vi') || a.item.name.localeCompare(b.item.name, 'vi');
  roots.sort(compare);
  for (const node of byId.values()) {
    node.children.sort(compare);
  }
  return { roots, byId };
}

/**
 * Tìm và lọc. Gõ toàn chữ số (01, 001, 00167) → theo đầu mã ĐVHC; còn lại mỗi từ là đầu một từ trong tên, không dấu
 * ("cau giay", "thu duc"). Lọc "có doanh nghiệp" chỉ ẩn nút 0 doanh nghiệp (số đã gồm đơn vị con); lọc "chưa chuyển đổi"
 * coi các nút không có trong bảng chuyển đổi là khớp. Không tìm, không lọc → null (hiện cả cây).
 */
export function searchLocations(tree: LocationTree, query: string | null | undefined, filter: LocationFilter): LocationSearch | null {
  const text = (query ?? '').trim();
  if (!text && filter === 'all') {
    return null;
  }
  const codePrefix = /^\d{1,5}$/.test(text) ? text : null;
  const words = codePrefix || !text ? [] : foldWords(text);
  const passes = (node: LocationNode): boolean =>
    (filter !== 'listings' || node.item.listingCount > 0) && (filter !== 'unmapped' || !node.item.mapped);
  const isMatch = (node: LocationNode): boolean => {
    if (!passes(node)) {
      return false;
    }
    if (codePrefix !== null) {
      return node.item.code.startsWith(codePrefix);
    }
    if (words.length > 0) {
      return words.every(word => node.words.some(w => w.startsWith(word)));
    }
    // Chỉ lọc: "chưa chuyển đổi" là khớp; "có doanh nghiệp" không đánh dấu khớp, chỉ ẩn nút rỗng
    return filter === 'unmapped';
  };

  const matches = new Set<number>();
  const keep = new Set<number>();
  const expand = new Set<number>();
  for (const node of tree.byId.values()) {
    if (isMatch(node)) {
      matches.add(node.item.id);
    }
  }
  if (!text && filter === 'listings') {
    for (const node of tree.byId.values()) {
      if (passes(node)) {
        keep.add(node.item.id);
      }
    }
    return { matches, keep, expand };
  }
  for (const id of matches) {
    const node = tree.byId.get(id)!;
    addSubtree(node, keep, passes);
    // Gõ mã: con cháu của nút khớp cũng khớp (001 → 00100…), chỉ mở tới nút khớp trên cùng
    const topmost = codePrefix === null || !node.parent || !matches.has(node.parent.item.id);
    for (let parent = node.parent; parent; parent = parent.parent) {
      keep.add(parent.item.id);
      if (topmost) {
        expand.add(parent.item.id);
      }
    }
  }
  return { matches, keep, expand };
}

/** Id mọi nút có con còn hiện (để "Mở hết"). */
export function expandableIds(tree: LocationTree, search: LocationSearch | null): Set<number> {
  const ids = new Set<number>();
  for (const node of tree.byId.values()) {
    if (node.children.length > 0 && (!search || search.keep.has(node.item.id))) {
      ids.add(node.item.id);
    }
  }
  return ids;
}

/** Các nút cha từ gốc xuống (không gồm chính nó). */
export function ancestorsOf(node: LocationNode): LocationNode[] {
  const result: LocationNode[] = [];
  for (let parent = node.parent; parent; parent = parent.parent) {
    result.unshift(parent);
  }
  return result;
}

/** Thứ tự loại đơn vị khi hiện số liệu: cấp tỉnh, cấp huyện, cấp xã. */
const KIND_ORDER = ['Thành phố', 'Tỉnh', 'Quận', 'Huyện', 'Thị xã', 'Phường', 'Xã', 'Thị trấn', 'Đặc khu'];

/** Đếm theo loại (theo thứ tự cấp: { Thành phố: 9, Tỉnh: 25, Phường: 709, ... }) và số nút không có trong bảng chuyển đổi. */
export function summarize(tree: LocationTree): { kinds: Map<string, number>; unmapped: number; withListings: number } {
  const kinds = new Map<string, number>(KIND_ORDER.map(kind => [kind, 0]));
  let unmapped = 0;
  let withListings = 0;
  for (const node of tree.byId.values()) {
    const kind = node.kind ?? 'Khác';
    kinds.set(kind, (kinds.get(kind) ?? 0) + 1);
    if (!node.item.mapped) {
      unmapped++;
    }
    if (node.item.listingCount > 0) {
      withListings++;
    }
  }
  return { kinds: new Map([...kinds].filter(([, count]) => count > 0)), unmapped, withListings };
}

function addSubtree(node: LocationNode, ids: Set<number>, passes: (node: LocationNode) => boolean): void {
  const stack = [node];
  while (stack.length > 0) {
    const current = stack.pop()!;
    if (!ids.has(current.item.id) && (current === node || passes(current))) {
      ids.add(current.item.id);
      stack.push(...current.children);
    }
  }
}

function isAncestorOrSelf(node: LocationNode, candidate: LocationNode): boolean {
  for (let current: LocationNode | null = candidate; current; current = current.parent) {
    if (current === node) {
      return true;
    }
  }
  return false;
}
