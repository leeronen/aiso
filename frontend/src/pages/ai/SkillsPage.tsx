import { Access } from '../../components/Access';
import { AppModal } from '../../components/AppModal';
import { IoSchemaFields } from '../../components/IoSchemaFields';
import { IO_SCHEMA_TYPE_LABEL, templateForType } from '../../constants/ioSchemaTemplates';
import { useSyncModalForm } from '../../hooks/useModalForm';
import { App, Button, Form, Input, InputNumber, Select, Space, Switch, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { searchWorkflowOptions } from '../../api/selectOptions';
import { api } from '../../api/client';
import { runApi } from '../../api/runApi';
import { reportApiError } from '../../utils/http';
import type { ApiEnvelope } from '../../api/types';

type Row = {
  skillId?: number;
  skillName: string;
  description?: string;
  mcpServerId?: number;
  mcpServerName?: string;
  inputType?: string;
  outputType?: string;
  inputSchema?: string;
  outputSchema?: string;
  promptTemplateId?: number;
  workflowId?: number;
  status?: number;
};

type McpOption = { value: number; label: string };

type Page<T> = { records: T[]; total: number; current: number; size: number };

const CREATE_DEFAULTS: Partial<Row> = {
  status: 1,
  inputType: 'object',
  outputType: 'object',
  inputSchema: templateForType('object'),
  outputSchema: templateForType('object'),
};

function typeLabel(v?: string) {
  return (v && IO_SCHEMA_TYPE_LABEL[v]) || v || '—';
}

export function SkillsPage() {
  const { message } = App.useApp();
  const [mcpOptions, setMcpOptions] = useState<{ value: number; label: string }[]>([]);
  const [workflowOptions, setWorkflowOptions] = useState<{ value: number; label: string }[]>([]);
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<Row[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState({ current: 1, size: 10 });
  const [keyword, setKeyword] = useState('');
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Row | null>(null);
  const [form] = Form.useForm<Row>();

  const modalValues = useMemo(() => {
    if (!open) return null;
    return editing ?? CREATE_DEFAULTS;
  }, [open, editing]);

  useSyncModalForm(open, form, modalValues);

  const loadWorkflowOptions = useCallback(async () => {
    try {
      const opts = await searchWorkflowOptions('');
      setWorkflowOptions(opts.map((w) => ({ value: w.value, label: w.label })));
    } catch {
      /* optional */
    }
  }, []);

  const loadMcpOptions = useCallback(async () => {
    try {
      const { data } = await api.get<ApiEnvelope<McpOption[]>>('/api/ai/mcp-servers/options');
      if (data.code === 0) {
        setMcpOptions(data.data.map((m) => ({ value: m.value, label: m.label })));
      }
    } catch {
      /* optional */
    }
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiEnvelope<Page<Row>>>('/api/ai/skills', {
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
    void loadMcpOptions();
    void loadWorkflowOptions();
    void load();
  }, [load, loadMcpOptions, loadWorkflowOptions]);

  const closeModal = () => {
    setOpen(false);
    setEditing(null);
  };

  function handleDelete(row: Row) {
    void runApi(async () => {
      const { data } = await api.delete<ApiEnvelope<null>>(`/api/ai/skills/${row.skillId}`);
      if (data.code !== 0) {
        message.error(data.message);
        return;
      }
      message.success('已删除');
      void load();
    }, message);
  }

  const columns: ColumnsType<Row> = [
    { title: '名称', dataIndex: 'skillName', width: 160 },
    { title: '描述', dataIndex: 'description', ellipsis: true },
    { title: '关联 MCP', dataIndex: 'mcpServerName', width: 120, render: (v) => v || '—' },
    { title: '输入类型', dataIndex: 'inputType', width: 120, render: typeLabel },
    { title: '输出类型', dataIndex: 'outputType', width: 120, render: typeLabel },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (v) => (v === 1 ? <Tag color="success">启用</Tag> : <Tag>停用</Tag>),
    },
    {
      title: '操作',
      width: 140,
      render: (_, r) => (
        <Space>
          <Access code="ai:skill:update">
            <Button type="link" onClick={() => { setEditing(r); setOpen(true); }}>
              编辑
            </Button>
          </Access>
          <Access code="ai:skill:delete">
            <Button type="link" danger onClick={() => handleDelete(r)}>
              删除
            </Button>
          </Access>
        </Space>
      ),
    },
  ];

  const submit = () =>
    void runApi(async () => {
      const v = await form.validateFields();
      const body = { ...v, skillId: editing?.skillId };
      const { data } = await api.post<ApiEnvelope<{ skillId: number }>>('/api/ai/skills', body);
      if (data.code !== 0) {
        message.error(data.message);
        return;
      }
      message.success('已保存');
      closeModal();
      void load();
    }, message);

  return (
    <div>
      <Typography.Title level={4}>Skill 管理</Typography.Title>
      <Space style={{ marginBottom: 12 }} wrap>
        <Access code="ai:skill:add">
          <Button type="primary" onClick={() => { setEditing(null); setOpen(true); }}>
            新建 Skill
          </Button>
        </Access>
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
        rowKey={(r) => String(r.skillId)}
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
        title={editing ? '编辑 Skill' : '新建 Skill'}
        open={open}
        onCancel={closeModal}
        onOk={() => void submit()}
        width={720}
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="skillName" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="mcpServerId" label="关联 MCP">
            <Select allowClear showSearch optionFilterProp="label" options={mcpOptions} placeholder="可选" />
          </Form.Item>
          <IoSchemaFields title="输入配置" typeField="inputType" schemaField="inputSchema" form={form} />
          <IoSchemaFields title="输出配置" typeField="outputType" schemaField="outputSchema" form={form} />
          <Form.Item name="promptTemplateId" label="Prompt 模板 ID">
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item name="workflowId" label="关联工作流">
            <Select allowClear showSearch optionFilterProp="label" options={workflowOptions} placeholder="可选" />
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
