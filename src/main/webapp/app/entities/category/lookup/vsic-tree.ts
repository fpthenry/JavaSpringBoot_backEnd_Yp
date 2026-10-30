import { CategoryIndexItem } from '../service/category-index.service';

/** Một nút trong cây tra cứu mã ngành. */
export interface VsicNode {
  item: CategoryIndexItem;
  parent: VsicNode | null;
  children: VsicNode[];
  /** Tên bỏ dấu, chữ thường, để tìm không dấu. */
  folded: string;
}

export interface VsicTree {
  roots: VsicNode[];
  byId: Map<number, VsicNode>;
  /** Số ngành chưa có mã VSIC (dữ liệu cũ trước khi chuyển). */
  withoutCode: number;
}

/** Kết quả tìm: các nút khớp, các nút còn hiện (khớp + cha + con cháu) và các nút mở sẵn. */
export interface VsicSearch {
  matches: Set<number>;
  keep: Set<number>;
  expand: Set<number>;
}

/** "Bán buôn  THỊT" → "ban buon thit". */
export function foldText(text: string | null | undefined): string {
  return (text ?? '').normalize('NFD').replace(/\p{M}/gu, '').replace(/[đĐ]/g, 'd').toLowerCase().replace(/\s+/g, ' ').trim();
}

/**
 * Dựng cây từ danh sách phẳng (giữ thứ tự máy chủ trả: theo mã ngành). Ngành có cha không nằm trong danh sách thì
 * coi là gốc; cây lỗi có vòng lặp cũng không treo (nút trong vòng lặp thành gốc).
 */
export function buildVsicTree(items: CategoryIndexItem[]): VsicTree {
  const byId = new Map<number, VsicNode>();
  for (const item of items) {
    byId.set(item.id, { item, parent: null, children: [], folded: foldText(item.name) });
  }
  const roots: VsicNode[] = [];
  for (const node of byId.values()) {
    const parent = node.item.parentId === null ? undefined : byId.get(node.item.parentId);
    if (parent && !isAncestorOrSelf(node, parent)) {
      node.parent = parent;
      parent.children.push(node);
    } else {
      roots.push(node);
    }
  }
  return { roots, byId, withoutCode: items.filter(item => !item.code).length };
}

/**
 * Tìm theo mã hoặc tên. Gõ một chữ cái (G) hoặc chữ số (46, 4632) thì tìm theo đầu mã ngành; còn lại tìm trong tên,
 * không dấu, đủ mọi từ. Trống thì null (hiện cả cây).
 * <p>
 * Mở sẵn các ngành cha của nút khớp. Tìm theo mã thì con cháu của nút khớp cũng khớp, nên chỉ mở tới nút khớp trên
 * cùng (gõ 46 thấy G › 46, chưa bung 463, 4632...); khớp đúng nguyên mã thì mở luôn nút đó.
 */
export function searchVsic(tree: VsicTree, query: string | null | undefined): VsicSearch | null {
  const text = (query ?? '').trim();
  if (!text) {
    return null;
  }
  const codePrefix = /^([A-Za-z]|\d{1,5})$/.test(text) ? text.toUpperCase() : null;
  const words = foldText(text).split(' ');
  const isMatch = (node: VsicNode): boolean =>
    codePrefix !== null ? (node.item.code ?? '').startsWith(codePrefix) : words.every(word => node.folded.includes(word));

  const matches = new Set<number>();
  const keep = new Set<number>();
  const expand = new Set<number>();
  for (const node of tree.byId.values()) {
    if (!isMatch(node)) {
      continue;
    }
    matches.add(node.item.id);
    addSubtree(node, keep);
    if (codePrefix !== null && node.item.code === codePrefix) {
      expand.add(node.item.id);
    }
  }
  for (const id of matches) {
    const node = tree.byId.get(id)!;
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

/** Id mọi nút có con (để "Mở hết"); chỉ trong các nút còn hiện khi đang tìm. */
export function expandableIds(tree: VsicTree, search: VsicSearch | null): Set<number> {
  const ids = new Set<number>();
  for (const node of tree.byId.values()) {
    if (node.children.length > 0 && (!search || search.keep.has(node.item.id))) {
      ids.add(node.item.id);
    }
  }
  return ids;
}

function addSubtree(node: VsicNode, ids: Set<number>): void {
  const stack = [node];
  while (stack.length > 0) {
    const current = stack.pop()!;
    if (!ids.has(current.item.id)) {
      ids.add(current.item.id);
      stack.push(...current.children);
    }
  }
}

function isAncestorOrSelf(node: VsicNode, candidate: VsicNode): boolean {
  for (let current: VsicNode | null = candidate; current; current = current.parent) {
    if (current === node) {
      return true;
    }
  }
  return false;
}
