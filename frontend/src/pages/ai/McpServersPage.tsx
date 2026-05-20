import { Access } from '../../components/Access';
import { AppModal } from '../../components/AppModal';
import { IoSchemaFields } from '../../components/IoSchemaFields';
import { IO_SCHEMA_TYPE_LABEL, templateForType } from '../../constants/ioSchemaTemplates';
import { useSyncModalForm } from '../../hooks/useModalForm';
import { App, Button, Form, Input, Select, Space, Switch, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { api } from '../../api/client';
import { runApi } from '../../api/runApi';
import { reportApiError } from '../../utils/http';
import type { ApiEnvelope } from '../../api/types';

type Row = {
  mcpServerId?: number;
  serverName: string;
  protocolType?: string;
  serverUrl?: string;
  authConfig?: string;
  inputType?: string;
  outputType?: string;
  inputSchema?: string;
  outputSchema?: string;
  status?: number;
};

type Page<T> = { records: T[]; total: number; current: number; size: number };

const PROTOCOL_OPTIONS = [
  { value: 'stdio', label: 'stdio' },
  { value: 'sse', label: 'SSE' },
  { value: 'http', label: 'HTTP' },
];

const CREATE_DEFAULTS: Partial<Row> = {
  status: 1,
  protocolType: 'sse',
  inputType: 'object',
  outputType: 'object',
  inputSchema: templateForType('object'),
  outputSchema: templateForType('object'),
};

function typeLabel(v?: string) {
  return (v && IO_SCHEMA_TYPE_LABEL[v]) || v || '—';
}

export function McpServersPage() {
  const { message } = App.useApp();
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

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiEnvelope<Page<Row>>>('/api/ai/mcp-servers', {
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

  const closeModal = () => {
    setOpen(false);
    setEditing(null);
  };

  const columns: ColumnsType<Row> = [
    { title: '服务名称', dataIndex: 'serverName', width: 160 },
    { title: '协议', dataIndex: 'protocolType', width: 80 },
    { title: '地址', dataIndex: 'serverUrl', ellipsis: true },
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
          <Access code="ai:mcp:update">
            <Button type="link" onClick={() => { setEditing(r); setOpen(true); }}>
              编辑
            </Button>
          </Access>
          <Access code="ai:mcp:delete">
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
      const body = { ...v, mcpServerId: editing?.mcpServerId };
      const { data } = await api.post<ApiEnvelope<{ mcpServerId: number }>>('/api/ai/mcp-servers', body);
      if (data.code !== 0) {
        message.error(data.message);
        return;
      }
      message.success('已保存');
      closeModal();
      void load();
    }, message);

  function handleDelete(row: Row) {
    void runApi(async () => {
      const { data } = await api.delete<ApiEnvelope<null>>(`/api/ai/mcp-servers/${row.mcpServerId}`);
      if (data.code !== 0) {
        message.error(data.message);
        return;
      }
      message.success('已删除');
      void load();
    }, message);
  }

  return (
    <div>
      <Typography.Title level={4}>MCP 管理</Typography.Title>
      <Space style={{ marginBottom: 12 }} wrap>
        <Access code="ai:mcp:add">
          <Button type="primary" onClick={() => { setEditing(null); setOpen(true); }}>
            新建 MCP
          </Button>
        </Access>
        <Input.Search
          placeholder="搜索名称/协议/地址"
          allowClear
          style={{ width: 260 }}
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
        rowKey={(r) => String(r.mcpServerId)}
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
        title={editing ? '编辑 MCP' : '新建 MCP'}
        open={open}
        onCancel={closeModal}
        onOk={() => void submit()}
        width={720}
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="serverName" label="服务名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="protocolType" label="协议类型">
            <Select allowClear options={PROTOCOL_OPTIONS} />
          </Form.Item>
          <Form.Item name="serverUrl" label="服务地址">
            <Input placeholder="http://127.0.0.1:8081/sse" />
          </Form.Item>
          <Form.Item name="authConfig" label="认证配置 (JSON)">
            <Input.TextArea rows={3} placeholder='{"token":"..."}' />
          </Form.Item>
          <IoSchemaFields title="输入配置" typeField="inputType" schemaField="inputSchema" form={form} />
          <IoSchemaFields title="输出配置" typeField="outputType" schemaField="outputSchema" form={form} />
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
