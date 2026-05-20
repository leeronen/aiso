import { AppModal } from '../../components/AppModal';
import { searchEmbeddingTypeOptions } from '../../api/selectOptions';
import { useDebouncedStringSearchOptions } from '../../hooks/useDebouncedStringSearchOptions';
import { useSyncModalForm } from '../../hooks/useModalForm';
import { App, Button, Form, Input, InputNumber, Select, Space, Switch, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { api } from '../../api/client';
import { runApi } from '../../api/runApi';
import { reportApiError } from '../../utils/http';
import type { ApiEnvelope } from '../../api/types';

type Row = {
  knowledgeBaseId?: number;
  knowledgeBaseName: string;
  description?: string;
  embeddingTypeCode?: string;
  embeddingTypeName?: string;
  embeddingModel?: string;
  chunkSize?: number;
  overlapSize?: number;
  status?: number;
};

type Page<T> = { records: T[]; total: number; current: number; size: number };

const CREATE_DEFAULTS: Partial<Row> = {
  status: 1,
  chunkSize: 1024,
  overlapSize: 128,
  embeddingTypeCode: 'chunk',
};

export function KbBasesPage() {
  const { message } = App.useApp();
  const embeddingSearch = useDebouncedStringSearchOptions(searchEmbeddingTypeOptions);
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<Row[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState({ current: 1, size: 10 });
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Row | null>(null);
  const [form] = Form.useForm<Row>();

  const modalValues = useMemo(() => {
    if (!open) return null;
    return editing ?? CREATE_DEFAULTS;
  }, [open, editing]);

  useSyncModalForm(open, form, modalValues);

  useEffect(() => {
    if (!open || !editing?.embeddingTypeCode) return;
    embeddingSearch.mergeSelected([
      {
        value: editing.embeddingTypeCode,
        label: editing.embeddingTypeName ?? editing.embeddingTypeCode,
      },
    ]);
  }, [open, editing, embeddingSearch]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiEnvelope<Page<Row>>>('/api/kb/bases', {
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

  const columns: ColumnsType<Row> = [
    { title: '名称', dataIndex: 'knowledgeBaseName', width: 160 },
    { title: 'Embedding 类型', dataIndex: 'embeddingTypeName', width: 160, render: (v) => v || '—' },
    { title: '向量模型', dataIndex: 'embeddingModel', width: 120, render: (v) => v || '—' },
    { title: 'Chunk', dataIndex: 'chunkSize', width: 80 },
    {
      title: '操作',
      width: 80,
      render: (_, r) => (
        <Button
          type="link"
          onClick={() => {
            setEditing(r);
            setOpen(true);
            void embeddingSearch.reload();
          }}
        >
          编辑
        </Button>
      ),
    },
  ];

  const submit = () =>
    void runApi(async () => {
      const v = await form.validateFields();
      const body = { ...v, knowledgeBaseId: editing?.knowledgeBaseId };
      const { data } = await api.post<ApiEnvelope<{ knowledgeBaseId: number }>>('/api/kb/bases', body);
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
      <Typography.Title level={4}>知识库</Typography.Title>
      <Space style={{ marginBottom: 12 }}>
        <Button
          type="primary"
          onClick={() => {
            setEditing(null);
            setOpen(true);
            void embeddingSearch.reload();
          }}
        >
          新建
        </Button>
      </Space>
      <Table<Row>
        rowKey={(r) => String(r.knowledgeBaseId)}
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
        title={editing ? '编辑知识库' : '新建知识库'}
        open={open}
        onCancel={() => {
          setOpen(false);
          setEditing(null);
        }}
        onOk={() => void submit()}
        width={640}
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="knowledgeBaseName" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item
            name="embeddingTypeCode"
            label="Embedding 类型"
            rules={[{ required: true, message: '请选择 Embedding 类型' }]}
            extra="类型来自数据库配置，可搜索名称或说明"
          >
            <Select
              showSearch
              allowClear
              filterOption={false}
              placeholder="搜索并选择 Embedding 类型"
              loading={embeddingSearch.loading}
              options={embeddingSearch.options.map((o) => ({
                value: o.value,
                label: o.label,
                title: o.description,
              }))}
              onSearch={embeddingSearch.onSearch}
              optionRender={(opt) => (
                <div>
                  <div>{opt.label}</div>
                  {opt.data?.title && (
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      {String(opt.data.title)}
                    </Typography.Text>
                  )}
                </div>
              )}
            />
          </Form.Item>
          <Form.Item name="embeddingModel" label="向量模型（可选）">
            <Input placeholder="如 bge-m3、text-embedding-3-small" />
          </Form.Item>
          <Form.Item name="chunkSize" label="Chunk 大小">
            <InputNumber min={128} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="overlapSize" label="重叠">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} />
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
