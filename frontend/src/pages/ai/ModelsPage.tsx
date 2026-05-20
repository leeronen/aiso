import { Access } from '../../components/Access';
import { useSyncModalForm } from '../../hooks/useModalForm';
import { AppModal } from '../../components/AppModal';
import { App, Button, Form, Input, InputNumber, Select, Space, Switch, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { api } from '../../api/client';
import { runApi } from '../../api/runApi';
import { reportApiError } from '../../utils/http';
import type { ApiEnvelope } from '../../api/types';

type Row = {
  modelId?: number;
  modelName: string;
  modelCode: string;
  providerType?: string;
  baseUrl?: string;
  apiKey?: string;
  maxTokens?: number;
  temperature?: number;
  topP?: number;
  supportFunctionCall?: number;
  supportVision?: number;
  status?: number;
};

type Page<T> = { records: T[]; total: number; current: number; size: number };

const CREATE_DEFAULTS: Partial<Row> = {
  status: 1,
  maxTokens: 8192,
  temperature: 0.7,
  topP: 1,
  supportFunctionCall: 1,
  supportVision: 0,
};

export function ModelsPage() {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<Row[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState({ current: 1, size: 10 });
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Row | null>(null);
  const [form] = Form.useForm<Row>();

  const modalValues = useMemo(() => {
    if (!open) return null;
    return editing ? { ...editing, apiKey: undefined } : CREATE_DEFAULTS;
  }, [open, editing]);

  useSyncModalForm(open, form, modalValues);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiEnvelope<Page<Row>>>('/api/ai/models', {
        params: { current: page.current, size: page.size },
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
  }, [message, page.current, page.size]);

  useEffect(() => {
    void load();
  }, [load]);

  const closeModal = () => {
    setOpen(false);
    setEditing(null);
  };

  const columns: ColumnsType<Row> = [
    { title: '名称', dataIndex: 'modelName' },
    { title: '编码', dataIndex: 'modelCode' },
    { title: '供应商', dataIndex: 'providerType' },
    { title: '状态', dataIndex: 'status', render: (v) => (v === 1 ? '启用' : '停用') },
    {
      title: '操作',
      render: (_, r) => (
        <Space>
          <Access code="ai:model:update">
            <Button
              type="link"
              onClick={() => {
                setEditing(r);
                setOpen(true);
              }}
            >
              编辑
            </Button>
          </Access>
        </Space>
      ),
    },
  ];

  const submit = () =>
    void runApi(async () => {
      const v = await form.validateFields();
      const body = { ...v, modelId: editing?.modelId };
      const { data } = await api.post<ApiEnvelope<{ modelId: number }>>('/api/ai/models', body);
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
      <Typography.Title level={4}>模型管理</Typography.Title>
      <Space style={{ marginBottom: 12 }}>
        <Access code="ai:model:add">
          <Button
            type="primary"
            onClick={() => {
              setEditing(null);
              setOpen(true);
            }}
          >
            新建
          </Button>
        </Access>
      </Space>
      <Table<Row>
        rowKey={(r) => String(r.modelId)}
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
        title={editing ? '编辑模型' : '新建模型'}
        open={open}
        onCancel={closeModal}
        onOk={() => void submit()}
        width={640}
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="modelName" label="模型名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="modelCode" label="模型编码" rules={[{ required: true }]}>
            <Input placeholder="如 gpt-4o" />
          </Form.Item>
          <Form.Item name="providerType" label="供应商类型">
            <Select
              allowClear
              options={[
                { value: 'openai', label: 'OpenAI' },
                { value: 'claude', label: 'Claude' },
                { value: 'qwen', label: 'Qwen' },
                { value: 'deepseek', label: 'DeepSeek' },
                { value: 'ollama', label: 'Ollama' },
              ]}
            />
          </Form.Item>
          <Form.Item name="baseUrl" label="Base URL">
            <Input />
          </Form.Item>
          <Form.Item name="apiKey" label="API Key">
            <Input.Password
              autoComplete="off"
              placeholder="新建必填；编辑留空表示不修改"
            />
          </Form.Item>
          <Form.Item name="maxTokens" label="最大 Token">
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item name="temperature" label="温度">
            <InputNumber style={{ width: '100%' }} min={0} max={2} step={0.05} />
          </Form.Item>
          <Form.Item name="topP" label="Top P">
            <InputNumber style={{ width: '100%' }} min={0} max={1} step={0.05} />
          </Form.Item>
          <Form.Item
            name="supportFunctionCall"
            label="支持 Function Call"
            valuePropName="checked"
            getValueFromEvent={(c) => (c ? 1 : 0)}
            getValueProps={(v) => ({ checked: v === 1 })}
          >
            <Switch />
          </Form.Item>
          <Form.Item
            name="supportVision"
            label="支持视觉"
            valuePropName="checked"
            getValueFromEvent={(c) => (c ? 1 : 0)}
            getValueProps={(v) => ({ checked: v === 1 })}
          >
            <Switch />
          </Form.Item>
          <Form.Item
            name="status"
            label="启用"
            valuePropName="checked"
            getValueFromEvent={(c) => (c ? 1 : 0)}
            getValueProps={(v) => ({ checked: v === 1 })}
          >
            <Switch />
          </Form.Item>
        </Form>
      </AppModal>
    </div>
  );
}
