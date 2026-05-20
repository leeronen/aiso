/** 与后端 WorkflowGraphSupport 对齐的画布 JSON 结构 */

export type WorkflowGraphNode = {
  id: string;
  type: 'start' | 'agent' | 'end';
  position: { x: number; y: number };
  data?: {
    agentId?: number;
    nodeKey?: string;
    label?: string;
  };
};

export type WorkflowGraphEdge = {
  id: string;
  source: string;
  target: string;
};

export type WorkflowGraph = {
  nodes: WorkflowGraphNode[];
  edges: WorkflowGraphEdge[];
};

export function createDefaultGraph(): WorkflowGraph {
  return {
    nodes: [
      { id: 'start', type: 'start', position: { x: 60, y: 180 }, data: {} },
      {
        id: 'node_1',
        type: 'agent',
        position: { x: 280, y: 160 },
        data: { nodeKey: 'node_1', label: 'Agent 1' },
      },
      { id: 'end', type: 'end', position: { x: 520, y: 180 }, data: {} },
    ],
    edges: [
      { id: 'e-start-node_1', source: 'start', target: 'node_1' },
      { id: 'e-node_1-end', source: 'node_1', target: 'end' },
    ],
  };
}

export function parseGraphJson(raw?: string | null): WorkflowGraph | null {
  if (!raw?.trim()) return null;
  try {
    const g = JSON.parse(raw) as WorkflowGraph;
    if (!Array.isArray(g.nodes) || !Array.isArray(g.edges)) return null;
    return g;
  } catch {
    return null;
  }
}

/** 用于比较是否「实质变更」，避免 position 浮点抖动触发 Form 回写死循环 */
export function graphSignature(graph: WorkflowGraph): string {
  const nodes = [...graph.nodes]
    .sort((a, b) => a.id.localeCompare(b.id))
    .map((n) => ({
      id: n.id,
      type: n.type,
      position: {
        x: Math.round(n.position?.x ?? 0),
        y: Math.round(n.position?.y ?? 0),
      },
      data: n.data ?? {},
    }));
  const edges = [...graph.edges]
    .sort((a, b) => a.id.localeCompare(b.id))
    .map((e) => ({ id: e.id, source: e.source, target: e.target }));
  return JSON.stringify({ nodes, edges });
}

export function graphToJson(graph: WorkflowGraph): string {
  return JSON.stringify({
    nodes: graph.nodes.map((n) => ({
      id: n.id,
      type: n.type,
      position: {
        x: Math.round(n.position?.x ?? 0),
        y: Math.round(n.position?.y ?? 0),
      },
      data: n.data ?? {},
    })),
    edges: graph.edges.map((e) => ({
      id: e.id,
      source: e.source,
      target: e.target,
    })),
  });
}

export function nextAgentNodeKey(nodes: WorkflowGraphNode[]): string {
  const used = new Set(
    nodes.filter((n) => n.type === 'agent').map((n) => n.data?.nodeKey || n.id),
  );
  let i = 1;
  while (used.has(`node_${i}`)) i++;
  return `node_${i}`;
}
