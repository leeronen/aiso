import { Access } from '../../components/Access';
import { AppModal } from '../../components/AppModal';
import { IoSchemaFields } from '../../components/IoSchemaFields';
import { WorkflowGraphEditor } from '../../components/workflow/WorkflowGraphEditor';
import { IO_SCHEMA_TYPE_LABEL, templateForType } from '../../constants/ioSchemaTemplates';
import { searchAgentOptions } from '../../api/selectOptions';
import { useDebouncedSearchOptions } from '../../hooks/useDebouncedSearchOptions';
import { useSyncModalForm } from '../../hooks/useModalForm';
import { createDefaultGraph, graphToJson } from '../../utils/workflowGraphUtils';
import {
  App,
  Button,
  Drawer,
  Form,
  Input,
  Modal,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { api } from '../../api/client';
import { runApi } from '../../api/runApi';
import { reportApiError } from '../../utils/http';
import type { ApiEnvelope } from '../../api/types';

type Row = {
  workflowId?: number;
  workflowName: string;
  description?: string;
  inputType?: string;
  outputType?: string;
  inputSchema?: string;
  outputSchema?: string;
  version?: string;
  versionNo?: number;
  status?: number;
  graphJson?: string;
  agentSummary?: string;
  agentCount?: number;
};

type VersionRow = {
  versionId: number;
  workflowId: number;
  versionNo: number;
  versionLabel: string;
  workflowName: string;
  changeSummary?: string;
  createdTime?: string;
  agentCount?: number;
};

type VersionDetail = VersionRow & {
  description?: string;
  inputType?: string;
  outputType?: string;
  graphJson?: string;
};

type Page<T> = { records: T[]; total: number; current: number; size: number };

const CREATE_DEFAULTS: Partial<Row> = {
  status: 1,
  inputType: 'object',
  outputType: 'object',
  inputSchema: templateForType('object'),
  outputSchema: templateForType('object'),
  graphJson: graphToJson(createDefaultGraph()),
};

function typeLabel(v?: string) {
  return (v && IO_SCHEMA_TYPE_LABEL[v]) || v || '—';
}

export function WorkflowsPage() {
  const { message, modal } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<Row[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState({ current: 1, size: 10 });
  const [keyword, setKeyword] = useState('');
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Row | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [form] = Form.useForm<Row>();

  const [historyOpen, setHistoryOpen] = useState(false);
  const [historyWorkflow, setHistoryWorkflow] = useState<Row | null>(null);
  const [versions, setVersions] = useState<VersionRow[]>([]);
  const [versionsLoading, setVersionsLoading] = useState(false);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewDetail, setPreviewDetail] = useState<VersionDetail | null>(null);

  const agentSearch = useDebouncedSearchOptions(searchAgentOptions);
  const previewAgentSearch = useDebouncedSearchOptions(searchAgentOptions);

  const modalValues = useMemo(() => {
    if (!open) return null;
    return editing ?? CREATE_DEFAULTS;
  }, [open, editing]);

  useSyncModalForm(open, form, modalValues);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiEnvelope<Page<Row>>>('/api/ai/workflows', {
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

  useEffect(() => {
    if (!open) return;
    agentSearch.reload();
    // 仅弹窗打开时拉取一次 Agent 选项；勿把 agentSearch 放入 deps（每次 render 新对象会死循环请求）
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const mergeAgentsFromGraph = async (graphJson?: string) => {
    if (!graphJson) return;
    try {
      const g = JSON.parse(graphJson) as { nodes?: { data?: { agentId?: number } }[] };
      const agentIds = new Set(
        (g.nodes ?? []).map((n) => n.data?.agentId).filter((id): id is number => id != null),
      );
      if (agentIds.size === 0) return;
      const agentAll = await searchAgentOptions('');
      const picked = agentAll.filter((o) => agentIds.has(o.value));
      agentSearch.mergeSelected(picked);
      previewAgentSearch.mergeSelected(picked);
    } catch {
      /* ignore */
    }
  };

  const openEdit = (row: Row) => {
    setDetailLoading(true);
    setEditing(row);
    setOpen(true);
    void (async () => {
      try {
        const { data } = await api.get<ApiEnvelope<Row>>(`/api/ai/workflows/${row.workflowId}`);
        if (data.code !== 0) {
          message.error(data.message);
          return;
        }
        const detail = data.data;
        setEditing(detail);
        await mergeAgentsFromGraph(detail.graphJson);
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

  const openHistory = (row: Row) => {
    setHistoryWorkflow(row);
    setHistoryOpen(true);
    setVersionsLoading(true);
    void (async () => {
      try {
        const { data } = await api.get<ApiEnvelope<VersionRow[]>>(
          `/api/ai/workflows/${row.workflowId}/versions`,
        );
        if (data.code !== 0) {
          message.error(data.message);
          return;
        }
        setVersions(data.data);
      } catch (e) {
        reportApiError(e, message);
      } finally {
        setVersionsLoading(false);
      }
    })();
  };

  const previewVersion = (versionId: number) => {
    void runApi(async () => {
      const { data } = await api.get<ApiEnvelope<VersionDetail>>(
        `/api/ai/workflows/versions/${versionId}`,
      );
      if (data.code !== 0) {
        message.error(data.message);
        return;
      }
      setPreviewDetail(data.data);
      setPreviewOpen(true);
      await mergeAgentsFromGraph(data.data.graphJson);
      previewAgentSearch.reload();
    }, message);
  };

  const restoreVersion = (v: VersionRow) => {
    void modal.confirm({
      title: `恢复到 ${v.versionLabel}？`,
      content: '将以此版本内容覆盖当前工作流，并生成新的版本记录。',
      onOk: () =>
        runApi(async () => {
          const { data } = await api.post<ApiEnvelope<{ workflowId: number }>>(
            `/api/ai/workflows/versions/${v.versionId}/restore`,
          );
          if (data.code !== 0) {
            message.error(data.message);
            return;
          }
          message.success('已恢复');
          setHistoryOpen(false);
          void load();
        }, message),
    });
  };

  const closeModal = () => {
    setOpen(false);
    setEditing(null);
  };

  function handleDelete(row: Row) {
    void runApi(async () => {
      const { data } = await api.delete<ApiEnvelope<null>>(`/api/ai/workflows/${row.workflowId}`);
      if (data.code !== 0) {
        message.error(data.message);
        return;
      }
      message.success('已删除');
      void load();
    }, message);
  }

  const columns: ColumnsType<Row> = [
    { title: '名称', dataIndex: 'workflowName', width: 160 },
    { title: '描述', dataIndex: 'description', ellipsis: true },
    { title: 'Agent 链路', dataIndex: 'agentSummary', ellipsis: true },
    { title: '入参', dataIndex: 'inputType', width: 90, render: typeLabel },
    { title: '出参', dataIndex: 'outputType', width: 90, render: typeLabel },
    {
      title: '版本',
      dataIndex: 'version',
      width: 80,
      render: (v, r) => v || (r.versionNo ? `v${r.versionNo}` : '—'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (v) => (v === 1 ? <Tag color="success">启用</Tag> : <Tag>停用</Tag>),
    },
    {
      title: '操作',
      width: 200,
      render: (_, r) => (
        <Space wrap>
          <Access code="ai:workflow:update">
            <Button type="link" onClick={() => openEdit(r)}>
              编辑
            </Button>
          </Access>
          <Access code="ai:workflow:view">
            <Button type="link" onClick={() => openHistory(r)}>
              历史
            </Button>
          </Access>
          <Access code="ai:workflow:delete">
            <Button type="link" danger onClick={() => handleDelete(r)}>
              删除
            </Button>
          </Access>
        </Space>
      ),
    },
  ];

  const versionColumns: ColumnsType<VersionRow> = [
    { title: '版本', dataIndex: 'versionLabel', width: 72 },
    { title: '说明', dataIndex: 'changeSummary', ellipsis: true },
    { title: '节点数', dataIndex: 'agentCount', width: 72 },
    { title: '保存时间', dataIndex: 'createdTime', width: 170 },
    {
      title: '操作',
      width: 140,
      render: (_, v) => (
        <Space>
          <Button type="link" onClick={() => previewVersion(v.versionId)}>
            查看
          </Button>
          <Access code="ai:workflow:update">
            <Button type="link" onClick={() => restoreVersion(v)}>
              恢复
            </Button>
          </Access>
        </Space>
      ),
    },
  ];

  const submit = () =>
    void runApi(async () => {
      const v = await form.validateFields();
      const body = { ...v, workflowId: editing?.workflowId };
      const { data } = await api.post<ApiEnvelope<{ workflowId: number }>>('/api/ai/workflows', body);
      if (data.code !== 0) {
        message.error(data.message);
        return;
      }
      message.success('已保存（已生成新版本）');
      closeModal();
      void load();
    }, message);

  const currentVersionLabel = editing?.version ?? (editing?.versionNo ? `v${editing.versionNo}` : null);

  return (
    <div>
      <Typography.Title level={4}>工作流管理</Typography.Title>
      <Space style={{ marginBottom: 12 }} wrap>
        <Access code="ai:workflow:add">
          <Button type="primary" onClick={() => { setEditing(null); setOpen(true); }}>
            新建工作流
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
        rowKey={(r) => String(r.workflowId)}
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
        title={
          <Space>
            <span>{editing?.workflowId ? '编辑工作流' : '新建工作流'}</span>
            {currentVersionLabel ? <Tag>{currentVersionLabel}</Tag> : null}
          </Space>
        }
        open={open}
        forceRender={false}
        onCancel={closeModal}
        onOk={() => void submit()}
        width={1060}
        confirmLoading={detailLoading}
        styles={{ body: { maxHeight: '75vh', overflowY: 'auto' } }}
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="workflowName" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 8 }}>
            保存后自动递增版本（v1、v2…），可在列表「历史」中查看与恢复。
          </Typography.Paragraph>
          <IoSchemaFields title="入参配置" typeField="inputType" schemaField="inputSchema" form={form} />
          <IoSchemaFields title="出参配置" typeField="outputType" schemaField="outputSchema" form={form} />
          <Typography.Text strong>流程画布</Typography.Text>
          <Form.Item
            name="graphJson"
            rules={[{ required: true, message: '请配置工作流画布' }]}
            style={{ marginTop: 8 }}
          >
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

      <Drawer
        title={`版本历史 · ${historyWorkflow?.workflowName ?? ''}`}
        open={historyOpen}
        onClose={() => setHistoryOpen(false)}
        width={640}
      >
        <Table<VersionRow>
          rowKey="versionId"
          size="small"
          loading={versionsLoading}
          columns={versionColumns}
          dataSource={versions}
          pagination={false}
        />
      </Drawer>

      <Modal
        title={previewDetail ? `版本 ${previewDetail.versionLabel}` : '版本详情'}
        open={previewOpen}
        onCancel={() => setPreviewOpen(false)}
        footer={null}
        width={1060}
        styles={{ body: { maxHeight: '75vh', overflowY: 'auto' } }}
      >
        {previewDetail ? (
          <>
            <Typography.Paragraph type="secondary">
              {previewDetail.changeSummary || '—'} · {previewDetail.createdTime || ''}
            </Typography.Paragraph>
            <WorkflowGraphEditor
              readOnly
              value={previewDetail.graphJson}
              agentOptions={previewAgentSearch.options}
            />
          </>
        ) : null}
      </Modal>
    </div>
  );
}
