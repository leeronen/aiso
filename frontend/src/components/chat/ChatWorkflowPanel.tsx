import { Access } from '../Access';
import { AppModal } from '../AppModal';
import { IoSchemaFields } from '../IoSchemaFields';
import { WorkflowGraphEditor } from '../workflow/WorkflowGraphEditor';
import { templateForType } from '../../constants/ioSchemaTemplates';
import { searchAgentOptions } from '../../api/selectOptions';
import { useDebouncedSearchOptions } from '../../hooks/useDebouncedSearchOptions';
import { useSyncModalForm } from '../../hooks/useModalForm';
import { createDefaultGraph, graphToJson } from '../../utils/workflowGraphUtils';
import { App, Button, Form, Input, List, Space, Switch, Tag, Typography } from 'antd';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { api } from '../../api/client';
import { runApi } from '../../api/runApi';
import { reportApiError } from '../../utils/http';
import type { ApiEnvelope } from '../../api/types';

export type WorkflowItem = {
  workflowId: number;
  workflowName: string;
  description?: string;
  version?: string;
  versionNo?: number;
  status?: number;
  agentSummary?: string;
  inputType?: string;
  outputType?: string;
  inputSchema?: string;
  outputSchema?: string;
  graphJson?: string;
};

type Page<T> = { records: T[]; total: number };

const CREATE_DEFAULTS: Partial<WorkflowItem> = {
  status: 1,
  inputType: 'object',
  outputType: 'object',
  inputSchema: templateForType('object'),
  outputSchema: templateForType('object'),
  graphJson: graphToJson(createDefaultGraph()),
};

type Props = {
  selectedId?: number | null;
  onSelect: (workflowId: number | null, item?: WorkflowItem) => void;
};

export function ChatWorkflowPanel({ selectedId, onSelect }: Props) {
  const { message, modal } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [items, setItems] = useState<WorkflowItem[]>([]);
  const [keyword, setKeyword] = useState('');
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<WorkflowItem | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [form] = Form.useForm<WorkflowItem>();
  const agentSearch = useDebouncedSearchOptions(searchAgentOptions);

  const modalValues = useMemo(() => {
    if (!open) return null;
    return editing ?? CREATE_DEFAULTS;
  }, [open, editing]);

  useSyncModalForm(open, form, modalValues);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiEnvelope<Page<WorkflowItem>>>('/api/ai/workflows', {
        params: { current: 1, size: 100, keyword: keyword || undefined },
      });
      if (data.code !== 0) {
        message.error(data.message);
        return;
      }
      setItems(data.data.records);
    } catch (e) {
      reportApiError(e, message);
    } finally {
      setLoading(false);
    }
  }, [message, keyword]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (!open) return;
    agentSearch.reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const openEdit = (row: WorkflowItem) => {
    setDetailLoading(true);
    setEditing(row);
    setOpen(true);
    void (async () => {
      try {
        const { data } = await api.get<ApiEnvelope<WorkflowItem>>(`/api/ai/workflows/${row.workflowId}`);
        if (data.code !== 0) {
          message.error(data.message);
          return;
        }
        const detail = data.data;
        setEditing(detail);
        form.setFieldsValue({
          ...detail,
          graphJson: detail.graphJson || CREATE_DEFAULTS.graphJson,
        });
      } catch (e) {
        reportApiError(e, message);
      } finally {
        setDetailLoading(false);
      }
    })();
  };

  const handleDelete = (row: WorkflowItem) => {
    void modal.confirm({
      title: `删除工作流「${row.workflowName}」？`,
      content: '删除后不可恢复，关联会话将失去工作流绑定。',
      okType: 'danger',
      onOk: () =>
        runApi(async () => {
          const { data } = await api.delete<ApiEnvelope<null>>(`/api/ai/workflows/${row.workflowId}`);
          if (data.code !== 0) {
            message.error(data.message);
            return;
          }
          message.success('已删除');
          if (selectedId === row.workflowId) onSelect(null);
          void load();
        }, message),
    });
  };

  const submit = () =>
    void runApi(async () => {
      const v = await form.validateFields();
      const body = { ...v, workflowId: editing?.workflowId };
      const { data } = await api.post<ApiEnvelope<{ workflowId: number }>>('/api/ai/workflows', body);
      if (data.code !== 0) {
        message.error(data.message);
        return;
      }
      message.success('已保存');
      setOpen(false);
      setEditing(null);
      await load();
      if (!editing?.workflowId && data.data.workflowId) {
        onSelect(data.data.workflowId);
      }
    }, message);

  return (
    <div className="chat-workflow-panel">
      <Typography.Text strong>工作流</Typography.Text>
      <Typography.Paragraph type="secondary" style={{ margin: '4px 0 8px', fontSize: 12 }}>
        选择工作流后新建会话将自动绑定
      </Typography.Paragraph>
      <Space direction="vertical" style={{ width: '100%', marginBottom: 8 }} size="small">
        <Access code="ai:workflow:add">
          <Button type="primary" block size="small" onClick={() => { setEditing(null); setOpen(true); }}>
            新建工作流
          </Button>
        </Access>
        <Input.Search
          size="small"
          placeholder="搜索"
          allowClear
          onSearch={setKeyword}
        />
      </Space>
      <List
        size="small"
        loading={loading}
        dataSource={items}
        locale={{ emptyText: '暂无工作流' }}
        style={{ flex: 1, overflow: 'auto' }}
        renderItem={(item) => {
          const active = selectedId === item.workflowId;
          return (
            <List.Item
              style={{
                cursor: 'pointer',
                padding: '8px 10px',
                borderRadius: 6,
                background: active ? '#e6f4ff' : undefined,
                border: active ? '1px solid #91caff' : '1px solid transparent',
              }}
              onClick={() => onSelect(item.workflowId, item)}
            >
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontWeight: 500 }}>{item.workflowName}</div>
                <Typography.Text type="secondary" ellipsis style={{ fontSize: 12, display: 'block' }}>
                  {item.agentSummary || item.description || '—'}
                </Typography.Text>
                <Space size={4} style={{ marginTop: 4 }}>
                  <Tag style={{ margin: 0 }}>{item.version || `v${item.versionNo ?? 1}`}</Tag>
                  {item.status === 1 ? (
                    <Tag color="success" style={{ margin: 0 }}>启用</Tag>
                  ) : (
                    <Tag style={{ margin: 0 }}>停用</Tag>
                  )}
                </Space>
              </div>
              <Space size={0} onClick={(e) => e.stopPropagation()}>
                <Access code="ai:workflow:update">
                  <Button type="link" size="small" onClick={() => openEdit(item)}>
                    编辑
                  </Button>
                </Access>
                <Access code="ai:workflow:delete">
                  <Button type="link" size="small" danger onClick={() => handleDelete(item)}>
                    删除
                  </Button>
                </Access>
              </Space>
            </List.Item>
          );
        }}
      />
      <AppModal
        title={editing?.workflowId ? '编辑工作流' : '新建工作流'}
        open={open}
        forceRender={false}
        onCancel={() => { setOpen(false); setEditing(null); }}
        onOk={() => void submit()}
        width={1060}
        confirmLoading={detailLoading}
        styles={{ body: { maxHeight: '70vh', overflowY: 'auto' } }}
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="workflowName" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} />
          </Form.Item>
          <IoSchemaFields title="入参" typeField="inputType" schemaField="inputSchema" form={form} />
          <IoSchemaFields title="出参" typeField="outputType" schemaField="outputSchema" form={form} />
          <Form.Item name="graphJson" rules={[{ required: true, message: '请配置画布' }]}>
            <WorkflowGraphEditor
              agentOptions={agentSearch.options}
              agentLoading={agentSearch.loading}
              onAgentSearch={agentSearch.onSearch}
            />
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