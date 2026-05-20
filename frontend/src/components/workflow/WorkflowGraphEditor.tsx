import {
  Background,
  Controls,
  Handle,
  MiniMap,
  Position,
  ReactFlow,
  addEdge,
  useEdgesState,
  useNodesState,
  type Connection,
  type Edge,
  type Node,
  type NodeProps,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { Button, Input, Select, Space, Typography } from 'antd';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  createDefaultGraph,
  graphSignature,
  graphToJson,
  nextAgentNodeKey,
  parseGraphJson,
  type WorkflowGraph,
} from '../../utils/workflowGraphUtils';
import './WorkflowGraphEditor.css';

type AgentOption = { value: number; label: string };

type AgentNodeData = {
  agentId?: number;
  nodeKey: string;
  label?: string;
};

function StartNode() {
  return (
    <div className="wf-node wf-node-start">
      <div className="wf-node-title">开始</div>
      <div className="wf-node-sub">工作流入口</div>
      <Handle type="source" position={Position.Right} />
    </div>
  );
}

function EndNode() {
  return (
    <div className="wf-node wf-node-end">
      <Handle type="target" position={Position.Left} />
      <div className="wf-node-title">结束</div>
      <div className="wf-node-sub">工作流出口</div>
    </div>
  );
}

function AgentNode({ data, selected }: NodeProps) {
  const d = (data ?? {}) as AgentNodeData;
  return (
    <div className={`wf-node wf-node-agent${selected ? ' wf-node-selected' : ''}`}>
      <Handle type="target" position={Position.Left} />
      <div className="wf-node-title">{d.label || d.nodeKey || 'Agent'}</div>
      <div className="wf-node-sub">{d.agentId ? `Agent #${d.agentId}` : '未选择 Agent'}</div>
      <Handle type="source" position={Position.Right} />
    </div>
  );
}

const nodeTypes = { start: StartNode, agent: AgentNode, end: EndNode };

function graphToFlow(g: WorkflowGraph): { nodes: Node[]; edges: Edge[] } {
  return {
    nodes: g.nodes.map((n) => ({
      id: n.id,
      type: n.type,
      position: n.position,
      data: n.data ?? {},
      deletable: n.type === 'agent',
    })),
    edges: g.edges.map((e) => ({
      id: e.id,
      source: e.source,
      target: e.target,
      animated: true,
    })),
  };
}

function flowToGraph(nodes: Node[], edges: Edge[]): WorkflowGraph {
  return {
    nodes: nodes.map((n) => ({
      id: n.id,
      type: (n.type as 'start' | 'agent' | 'end') || 'agent',
      position: n.position,
      data: (n.data as Record<string, unknown>) ?? {},
    })),
    edges: edges.map((e) => ({ id: e.id, source: e.source, target: e.target })),
  };
}

export function WorkflowGraphEditor({
  value,
  onChange,
  agentOptions,
  agentLoading,
  onAgentSearch,
  readOnly = false,
}: {
  value?: string;
  onChange?: (json: string) => void;
  agentOptions: AgentOption[];
  agentLoading?: boolean;
  onAgentSearch?: (kw: string) => void;
  readOnly?: boolean;
}) {
  const lastEmittedJson = useRef<string | undefined>(undefined);
  const lastSignature = useRef('');
  const emitTimer = useRef<ReturnType<typeof setTimeout>>();

  const initial = useMemo(() => {
    const g = parseGraphJson(value) ?? createDefaultGraph();
    lastSignature.current = graphSignature(g);
    return graphToFlow(g);
  }, []);

  const [nodes, setNodes, onNodesChange] = useNodesState(initial.nodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initial.edges);
  /** 侧栏编辑用独立选中态，避免点击侧栏时 React Flow 清空选区导致表单项卸载失焦 */
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const pushToForm = useCallback(
    (ns: Node[], es: Edge[]) => {
      if (readOnly || !onChange) return;
      const graph = flowToGraph(ns, es);
      const sig = graphSignature(graph);
      if (sig === lastSignature.current) return;
      const json = graphToJson(graph);
      lastSignature.current = sig;
      lastEmittedJson.current = json;
      onChange(json);
    },
    [onChange, readOnly],
  );

  useEffect(() => {
    if (value == null || value === lastEmittedJson.current) return;
    const parsed = parseGraphJson(value);
    if (!parsed) return;
    const sig = graphSignature(parsed);
    if (sig === lastSignature.current) {
      lastEmittedJson.current = value;
      return;
    }
    const { nodes: ns, edges: es } = graphToFlow(parsed);
    setNodes(ns);
    setEdges(es);
    lastEmittedJson.current = value;
    lastSignature.current = sig;
  }, [value, setNodes, setEdges]);

  useEffect(() => {
    if (readOnly) return;
    clearTimeout(emitTimer.current);
    emitTimer.current = setTimeout(() => pushToForm(nodes, edges), 350);
    return () => clearTimeout(emitTimer.current);
  }, [nodes, edges, pushToForm, readOnly]);

  const selectedAgent = useMemo(
    () => nodes.find((n) => n.id === selectedId && n.type === 'agent') ?? null,
    [nodes, selectedId],
  );

  useEffect(() => {
    if (readOnly || selectedId) return;
    const first = nodes.find((n) => n.type === 'agent');
    if (first) setSelectedId(first.id);
  }, [nodes, readOnly, selectedId]);

  const selectAgentNode = useCallback((node: Node) => {
    if (node.type === 'agent') setSelectedId(node.id);
  }, []);

  const isValidConnection = useCallback(
    (conn: Connection | Edge) => {
      const src = nodes.find((n) => n.id === conn.source);
      const tgt = nodes.find((n) => n.id === conn.target);
      if (!src || !tgt || src.id === tgt.id) return false;
      if (src.type === 'end' || tgt.type === 'start') return false;
      return true;
    },
    [nodes],
  );

  const onConnect = useCallback(
    (params: Connection) => {
      setEdges((eds) =>
        addEdge(
          { ...params, id: `e-${params.source}-${params.target}`, animated: true },
          eds,
        ),
      );
    },
    [setEdges],
  );

  const addAgent = () => {
    const graphNodes = flowToGraph(nodes, edges).nodes;
    const key = nextAgentNodeKey(graphNodes);
    const count = nodes.filter((n) => n.type === 'agent').length;
    setNodes((ns) => [
      ...ns,
      {
        id: key,
        type: 'agent',
        position: { x: 260 + count * 200, y: 140 + (count % 2) * 80 },
        data: { nodeKey: key, label: `Agent ${count + 1}` },
        deletable: true,
      },
    ]);
    setSelectedId(key);
  };

  const removeSelected = () => {
    if (!selectedId) return;
    const n = nodes.find((x) => x.id === selectedId);
    if (!n || n.type !== 'agent') return;
    setNodes((ns) => ns.filter((x) => x.id !== selectedId));
    setEdges((es) => es.filter((e) => e.source !== selectedId && e.target !== selectedId));
    setSelectedId(null);
  };

  const patchSelectedAgent = (patch: Partial<AgentNodeData>) => {
    if (!selectedId) return;
    setNodes((ns) =>
      ns.map((n) =>
        n.id === selectedId ? { ...n, data: { ...(n.data as AgentNodeData), ...patch } } : n,
      ),
    );
  };

  const d = (selectedAgent?.data ?? {}) as AgentNodeData;

  return (
    <div>
      {!readOnly ? (
        <Typography.Paragraph type="secondary" style={{ marginBottom: 8 }}>
          拖拽节点、从圆点拖线连接 Agent；同层分支将并行执行，汇合后继续下游（按画布拓扑）。
        </Typography.Paragraph>
      ) : null}
      <div className="workflow-graph-wrap">
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
          {!readOnly ? (
            <div className="workflow-graph-toolbar">
            <Button size="small" icon={<PlusOutlined />} onClick={addAgent}>
              添加 Agent
            </Button>
            <Button
              size="small"
              danger
              icon={<DeleteOutlined />}
              disabled={!selectedAgent}
              onClick={removeSelected}
            >
              删除选中
              </Button>
            </div>
          ) : null}
          <div className="workflow-graph-canvas">
            <ReactFlow
              nodes={nodes}
              edges={edges}
              onNodesChange={readOnly ? undefined : onNodesChange}
              onEdgesChange={readOnly ? undefined : onEdgesChange}
              onConnect={readOnly ? undefined : onConnect}
              onNodeClick={(_e, node) => selectAgentNode(node)}
              nodeTypes={nodeTypes}
              isValidConnection={isValidConnection}
              nodesDraggable={!readOnly}
              nodesConnectable={!readOnly}
              elementsSelectable={!readOnly}
              onInit={(inst) => inst.fitView({ padding: 0.2 })}
              onSelectionChange={({ nodes: sel }) => {
                const n = sel.find((x) => x.type === 'agent') ?? sel[0];
                if (n?.type === 'agent') setSelectedId(n.id);
              }}
              deleteKeyCode={readOnly ? null : ['Backspace', 'Delete']}
            >
              <Background gap={16} />
              <Controls />
              <MiniMap zoomable pannable />
            </ReactFlow>
          </div>
        </div>
        <div
          className="workflow-graph-side"
          onMouseDown={(e) => e.stopPropagation()}
          onPointerDown={(e) => e.stopPropagation()}
        >
          <Typography.Text strong>{readOnly ? '节点信息' : '节点属性'}</Typography.Text>
          {selectedAgent ? (
            <Space direction="vertical" style={{ width: '100%', marginTop: 12 }} size="middle">
              <div>
                <Typography.Text type="secondary">节点 Key</Typography.Text>
                {readOnly ? (
                  <div>{d.nodeKey}</div>
                ) : (
                  <Input
                    key={`${selectedId}-nodeKey`}
                    value={d.nodeKey ?? ''}
                    onChange={(e) => patchSelectedAgent({ nodeKey: e.target.value })}
                  />
                )}
              </div>
              <div>
                <Typography.Text type="secondary">显示名称</Typography.Text>
                {readOnly ? (
                  <div>{d.label || '—'}</div>
                ) : (
                  <Input
                    key={`${selectedId}-label`}
                    value={d.label ?? ''}
                    onChange={(e) => patchSelectedAgent({ label: e.target.value })}
                    placeholder="可选"
                  />
                )}
              </div>
              <div>
                <Typography.Text type="secondary">Agent</Typography.Text>
                {readOnly ? (
                  <div>
                    {agentOptions.find((o) => o.value === d.agentId)?.label ?? d.agentId ?? '—'}
                  </div>
                ) : (
                  <Select
                    key={`${selectedId}-agent`}
                    showSearch
                    allowClear
                    style={{ width: '100%' }}
                    placeholder="选择 Agent"
                    filterOption={false}
                    options={agentOptions}
                    loading={agentLoading}
                    value={d.agentId}
                    onSearch={onAgentSearch}
                    onChange={(v) => patchSelectedAgent({ agentId: v })}
                    getPopupContainer={(node) => node.parentElement ?? document.body}
                  />
                )}
              </div>
            </Space>
          ) : (
            <Typography.Paragraph type="secondary" style={{ marginTop: 12 }}>
              {readOnly ? '点击节点查看信息' : '点击画布中的 Agent 节点进行配置'}
            </Typography.Paragraph>
          )}
        </div>
      </div>
    </div>
  );
}
