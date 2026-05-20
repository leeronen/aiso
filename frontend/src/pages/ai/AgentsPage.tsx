import { AppModal } from '../../components/AppModal';
import {
  searchKbOptions,
  searchMcpOptions,
  searchModelOptions,
  searchSkillOptions,
} from '../../api/selectOptions';
import { useConfigOptions } from '../../hooks/useConfigOptions';
import { useDebouncedSearchOptions } from '../../hooks/useDebouncedSearchOptions';
import { useSyncModalForm } from '../../hooks/useModalForm';
import { App, Button, Form, Input, Select, Space, Switch, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { api } from '../../api/client';
import { runApi } from '../../api/runApi';
import { reportApiError } from '../../utils/http';
import type { ApiEnvelope } from '../../api/types';

type Row = {
  agentId?: number;
  agentName: string;
  description?: string;
  modelId?: number;
  modelName?: string;
  systemPrompt?: string;
  welcomeMessage?: string;
  thinkingMode?: string;
  memoryMode?: string;
  memoryModeLabel?: string;
  toolMode?: string;
  toolModeLabel?: string;
  knowledgeBaseIds?: number[];
  mcpServerIds?: number[];
  skillIds?: number[];
  knowledgeBaseSummary?: string;
  mcpServerSummary?: string;
  skillSummary?: string;
  status?: number;
};

type Page<T> = { records: T[]; total: number; current: number; size: number };

const THINKING_OPTIONS = [
  { value: 'ReAct', label: 'ReAct' },
  { value: 'CoT', label: 'CoT' },
  { value: 'PlanExecute', label: 'Plan & Execute' },
  { value: 'MultiAgent', label: 'Multi-Agent' },
];

const CREATE_DEFAULTS: Partial<Row> = {
  status: 1,
  thinkingMode: 'ReAct',
  memoryMode: 'session',
  toolMode: 'auto',
  knowledgeBaseIds: [],
  mcpServerIds: [],
  skillIds: [],
};

function SearchSelect(props: {
  value?: number | number[];
  onChange?: (v: number | number[] | undefined) => void;
  mode?: 'multiple';
  placeholder?: string;
  options: { value: number; label: string }[];
  loading?: boolean;
  onSearch: (kw: string) => void;
}) {
  const { value, onChange, mode, placeholder, options, loading, onSearch } = props;
  return (
    <Select
      showSearch
      allowClear
      mode={mode}
      filterOption={false}
      placeholder={placeholder}
      options={options}
      loading={loading}
      value={value}
      onChange={onChange}
      onSearch={onSearch}
      optionFilterProp="label"
    />
  );
}

export function AgentsPage() {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<Row[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState({ current: 1, size: 10 });
  const [keyword, setKeyword] = useState('');
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Row | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [form] = Form.useForm<Row>();

  const memoryModes = useConfigOptions('agent_memory_mode');
  const toolModes = useConfigOptions('agent_tool_mode');
  const modelSearch = useDebouncedSearchOptions(searchModelOptions);
  const kbSearch = useDebouncedSearchOptions(searchKbOptions);
  const mcpSearch = useDebouncedSearchOptions(searchMcpOptions);
  const skillSearch = useDebouncedSearchOptions(searchSkillOptions);

  const modalValues = useMemo(() => {
    if (!open) return null;
    return editing ?? CREATE_DEFAULTS;
  }, [open, editing]);

  useSyncModalForm(open, form, modalValues);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiEnvelope<Page<Row>>>('/api/ai/agents', {
        params: { current: page.current, size: page.size, keyword: keyword || undefined },
      });
      if (data.code !== 0) {
        message.error(data.message);
        return;
      }
      setRows(data.data.records);
      setTotal(data.data.total);
    } catch (e) {
      reportApiError(e, message);
    } finally {
      setLoading(false);
    }
  }, [message, page.current, page.size, keyword]);

  useEffect(() => {
    void load();
  }, [load]);

  const openCreate = () => {
    setEditing(null);
    setOpen(true);
    modelSearch.reload();
    kbSearch.reload();
    mcpSearch.reload();
    skillSearch.reload();
  };

  const openEdit = (row: Row) => {
    void runApi(async () => {
      setDetailLoading(true);
      try {
        const { data } = await api.get<ApiEnvelope<Row>>(`/api/ai/agents/${row.agentId}`);
        if (data.code !== 0) {
          message.error(data.message);
          return;
        }
        const detail = data.data;
        setEditing(detail);
        setOpen(true);
        if (detail.modelId && detail.modelName) {
          modelSearch.mergeSelected([{ value: detail.modelId, label: detail.modelName }]);
        }
        const [kbAll, mcpAll, skillAll] = await Promise.all([
          searchKbOptions(''),
          searchMcpOptions(''),
          searchSkillOptions(''),
        ]);
        const kbIds = new Set(detail.knowledgeBaseIds ?? []);
        const mcpIds = new Set(detail.mcpServerIds ?? []);
        const skillIds = new Set(detail.skillIds ?? []);
        kbSearch.mergeSelected(kbAll.filter((o) => kbIds.has(o.value)));
        mcpSearch.mergeSelected(mcpAll.filter((o) => mcpIds.has(o.value)));
        skillSearch.mergeSelected(skillAll.filter((o) => skillIds.has(o.value)));
      } finally {
        setDetailLoading(false);
      }
    }, message);
  };

  const columns: ColumnsType<Row> = [
    { title: '名称', dataIndex: 'agentName', width: 140 },
    { title: '绑定模型', dataIndex: 'modelName', width: 160, render: (v) => v || '—' },
    { title: '思考模式', dataIndex: 'thinkingMode', width: 110 },
    { title: '记忆模式', dataIndex: 'memoryModeLabel', width: 110, render: (v) => v || '—' },
    { title: '工具模式', dataIndex: 'toolModeLabel', width: 110, render: (v) => v || '—' },
    { title: '知识库', dataIndex: 'knowledgeBaseSummary', ellipsis: true },
    { title: 'MCP', dataIndex: 'mcpServerSummary', width: 120, ellipsis: true },
    { title: 'Skill', dataIndex: 'skillSummary', width: 120, ellipsis: true },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (v) => (v === 1 ? <Tag color="success">启用</Tag> : <Tag>停用</Tag>),
    },
    {
      title: '操作',
      width: 80,
      render: (_, r) => (
        <Button type="link" onClick={() => openEdit(r)} loading={detailLoading}>
          编辑
        </Button>
      ),
    },
  ];

  const submit = () =>
    void runApi(async () => {
      const v = await form.validateFields();
      const body = { ...v, agentId: editing?.agentId };
      const { data } = await api.post<ApiEnvelope<{ agentId: number }>>('/api/ai/agents', body);
      if (data.code !== 0) {
        message.error(data.message);
        return;
      }
      message.success('已保存');
      setOpen(false);
      setEditing(null);
      void load();
    }, message);

  return (
    <div>
      <Typography.Title level={4}>Agent 管理</Typography.Title>
      <Space style={{ marginBottom: 12 }} wrap>
        <Button type="primary" onClick={openCreate}>
          新建
        </Button>
        <Input.Search
          placeholder="搜索名称/描述"
          allowClear
          style={{ width: 220 }}
          onSearch={(v) => {
            setKeyword(v);
            setPage((p) => ({ ...p, current: 1 }));
          }}
        />
        <Button onClick={() => void load()} loading={loading}>
          刷新
        </Button>
      </Space>
      <Table<Row>
        rowKey={(r) => String(r.agentId)}
        loading={loading}
        columns={columns}
        dataSource={rows}
        pagination={{
          current: page.current,
          pageSize: page.size,
          total,
          showSizeChanger: true,
          onChange: (c, s) => setPage({ current: c, size: s }),
        }}
      />
      <AppModal
        title={editing ? '编辑 Agent' : '新建 Agent'}
        open={open}
        onCancel={() => {
          setOpen(false);
          setEditing(null);
        }}
        onOk={() => void submit()}
        width={760}
        confirmLoading={detailLoading}
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="agentName" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="modelId" label="绑定模型" rules={[{ required: true, message: '请选择模型' }]}>
            <SearchSelect
              placeholder="输入名称/编码搜索模型"
              options={modelSearch.options}
              loading={modelSearch.loading}
              onSearch={modelSearch.onSearch}
            />
          </Form.Item>
          <Form.Item name="thinkingMode" label="思考模式">
            <Select options={THINKING_OPTIONS} />
          </Form.Item>
          <Form.Item name="memoryMode" label="记忆选择模式" rules={[{ required: true }]}>
            <Select
              loading={memoryModes.loading}
              options={memoryModes.options}
              placeholder="从系统配置中选择"
            />
          </Form.Item>
          <Form.Item name="toolMode" label="工具选择模式" rules={[{ required: true }]}>
            <Select loading={toolModes.loading} options={toolModes.options} placeholder="从系统配置中选择" />
          </Form.Item>
          <Form.Item name="knowledgeBaseIds" label="知识库">
            <SearchSelect
              mode="multiple"
              placeholder="搜索并选择知识库"
              options={kbSearch.options}
              loading={kbSearch.loading}
              onSearch={kbSearch.onSearch}
            />
          </Form.Item>
          <Form.Item name="mcpServerIds" label="MCP">
            <SearchSelect
              mode="multiple"
              placeholder="搜索并选择 MCP"
              options={mcpSearch.options}
              loading={mcpSearch.loading}
              onSearch={mcpSearch.onSearch}
            />
          </Form.Item>
          <Form.Item name="skillIds" label="Skill">
            <SearchSelect
              mode="multiple"
              placeholder="搜索并选择 Skill"
              options={skillSearch.options}
              loading={skillSearch.loading}
              onSearch={skillSearch.onSearch}
            />
          </Form.Item>
          <Form.Item name="welcomeMessage" label="欢迎语">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="systemPrompt" label="系统 Prompt">
            <Input.TextArea rows={6} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item
            name="status"
            label="启用"
            valuePropName="checked"
            getValueFromEvent={(c) => (c ? 1 : 0)}
            getValueProps={(v) => ({ checked: (v ?? 1) === 1 })}
          >
            <Switch />
          </Form.Item>
        </Form>
      </AppModal>
    </div>
  );
}
