import { Access } from '../../components/Access';
import { AppModal } from '../../components/AppModal';
import { useSyncModalForm } from '../../hooks/useModalForm';
import {
  App,
  Button,
  Empty,
  Form,
  Input,
  Layout,
  Radio,
  Space,
  Table,
  Tag,
  Tree,
  Typography,
  Upload,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { DataNode } from 'antd/es/tree';
import type { UploadFile } from 'antd/es/upload/interface';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { api } from '../../api/client';
import { runApi } from '../../api/runApi';
import { reportApiError } from '../../utils/http';
import type { ApiEnvelope } from '../../api/types';

type TreeNode = { key: number; title: string; documentCount?: number; children?: TreeNode[] };

type Row = {
  documentId?: number;
  knowledgeBaseId?: number;
  knowledgeBaseName?: string;
  documentName: string;
  summary?: string;
  sourceType?: string;
  content?: string;
  sourceUrl?: string;
  fileType?: string;
  fileUrl?: string;
  parseStatus?: string;
  chunkCount?: number;
  fileSize?: number;
  status?: number;
};

type Page<T> = { records: T[]; total: number; current: number; size: number };

const SOURCE_LABEL: Record<string, string> = {
  manual: '手动录入',
  upload: '文件上传',
  url: 'URL 下载',
};

function formatIndexMessage(data?: { chunkCount?: number; parseStatus?: string }) {
  if (!data?.parseStatus) return '已保存';
  if (data.parseStatus === 'DONE') {
    return `已保存并向量化，共 ${data.chunkCount ?? 0} 个 Chunk`;
  }
  if (data.parseStatus === 'FAILED') {
    return '已保存，向量化失败（可点击「向量化」重试）';
  }
  if (data.parseStatus === 'PENDING') {
    return '已保存（正文暂不可向量化，请上传文本类文件或填写正文）';
  }
  return `已保存（${data.parseStatus}）`;
}

const PARSE_COLOR: Record<string, string> = {
  PENDING: 'default',
  PARSING: 'processing',
  INDEXING: 'processing',
  DONE: 'success',
  FAILED: 'error',
};

export function KbDocumentsPage() {
  const { message } = App.useApp();
  const [treeLoading, setTreeLoading] = useState(false);
  const [treeData, setTreeData] = useState<DataNode[]>([]);
  const [selectedKbId, setSelectedKbId] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<Row[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState({ current: 1, size: 10 });
  const [keyword, setKeyword] = useState('');
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Row | null>(null);
  const [sourceType, setSourceType] = useState<'manual' | 'upload' | 'url'>('manual');
  const [uploadFile, setUploadFile] = useState<UploadFile | null>(null);
  const [form] = Form.useForm<Row & { sourceType?: string }>();

  const kbIdForQuery = selectedKbId && selectedKbId > 0 ? selectedKbId : undefined;

  const modalValues = useMemo(() => {
    if (!open) return null;
    if (editing) {
      return { ...editing, sourceType: editing.sourceType ?? 'manual' };
    }
    return {
      sourceType: 'manual',
      knowledgeBaseId: kbIdForQuery,
    };
  }, [open, editing, kbIdForQuery]);

  useSyncModalForm(open, form, modalValues);

  useEffect(() => {
    if (open) {
      setSourceType((editing?.sourceType as 'manual' | 'upload' | 'url') ?? 'manual');
      setUploadFile(null);
    }
  }, [open, editing]);

  const loadTree = useCallback(async () => {
    setTreeLoading(true);
    try {
      const { data } = await api.get<ApiEnvelope<TreeNode[]>>('/api/kb/bases/tree');
      if (data.code !== 0) {
        message.error(data.message);
        return;
      }
      setTreeData(
        data.data.map((n) => ({
          key: String(n.key),
          title: n.title,
          isLeaf: true,
        })),
      );
    } catch (e) {
      reportApiError(e, message);
    } finally {
      setTreeLoading(false);
    }
  }, [message]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiEnvelope<Page<Row>>>('/api/kb/documents', {
        params: {
          current: page.current,
          size: page.size,
          knowledgeBaseId: kbIdForQuery,
          keyword: keyword || undefined,
        },
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
  }, [message, page.current, page.size, kbIdForQuery, keyword]);

  useEffect(() => {
    void loadTree();
  }, [loadTree]);

  useEffect(() => {
    void load();
  }, [load]);

  const openCreate = () => {
    if (!kbIdForQuery) {
      message.warning('请先在左侧选择知识库（不能选「全部知识库」）');
      return;
    }
    setEditing(null);
    setOpen(true);
  };

  const openEdit = (row: Row) => {
    void runApi(async () => {
      const { data } = await api.get<ApiEnvelope<Row>>(`/api/kb/documents/${row.documentId}`);
      if (data.code !== 0) {
        message.error(data.message);
        return;
      }
      setEditing(data.data);
      setOpen(true);
    }, message);
  };

  const columns: ColumnsType<Row> = [
    { title: '文档名称', dataIndex: 'documentName', width: 180, ellipsis: true },
    { title: '简述', dataIndex: 'summary', ellipsis: true },
    { title: '知识库', dataIndex: 'knowledgeBaseName', width: 120, render: (v) => v || '—' },
    {
      title: '来源',
      dataIndex: 'sourceType',
      width: 100,
      render: (v) => <Tag>{SOURCE_LABEL[v ?? ''] ?? v}</Tag>,
    },
    { title: '类型', dataIndex: 'fileType', width: 70 },
    {
      title: '向量化',
      dataIndex: 'parseStatus',
      width: 90,
      render: (v) => <Tag color={PARSE_COLOR[v ?? ''] ?? 'default'}>{v ?? '—'}</Tag>,
    },
    { title: 'Chunks', dataIndex: 'chunkCount', width: 70, render: (v) => v ?? 0 },
    {
      title: '操作',
      width: 200,
      render: (_, r) => (
        <Space>
          <Access code="kb:document:update">
            <Button type="link" onClick={() => handleIndex(r)}>
              向量化
            </Button>
          </Access>
          <Access code="kb:document:update">
            <Button type="link" onClick={() => openEdit(r)}>
              编辑
            </Button>
          </Access>
          <Access code="kb:document:delete">
            <Button type="link" danger onClick={() => handleDelete(r)}>
              删除
            </Button>
          </Access>
        </Space>
      ),
    },
  ];

  function handleIndex(row: Row) {
    void runApi(async () => {
      const { data } = await api.post<ApiEnvelope<{ chunkCount: number }>>(
        `/api/kb/documents/${row.documentId}/index`,
        {},
        { timeout: 120_000 },
      );
      if (data.code !== 0) {
        message.error(data.message);
        return;
      }
      message.success(`已向量化，共 ${data.data.chunkCount} 个 Chunk（pgvector）`);
      void load();
      void loadTree();
    }, message);
  }

  function handleDelete(row: Row) {
    void runApi(async () => {
      const { data } = await api.delete<ApiEnvelope<null>>(`/api/kb/documents/${row.documentId}`);
      if (data.code !== 0) {
        message.error(data.message);
        return;
      }
      message.success('已删除');
      void load();
      void loadTree();
    }, message);
  }

  const submit = () =>
    void runApi(async () => {
      const v = await form.validateFields();
      const kbId = v.knowledgeBaseId ?? kbIdForQuery;
      if (!kbId) {
        message.error('请选择知识库');
        return;
      }

      let saveMsg = '已保存';
      if (sourceType === 'upload') {
        const file = uploadFile?.originFileObj;
        if (!file && !editing?.documentId) {
          message.error('请选择要上传的文件');
          return;
        }
        if (file) {
          const fd = new FormData();
          fd.append('knowledgeBaseId', String(kbId));
          fd.append('documentName', v.documentName);
          if (v.summary) fd.append('summary', v.summary);
          if (editing?.documentId) fd.append('documentId', String(editing.documentId));
          fd.append('file', file);
          const { data } = await api.post<
            ApiEnvelope<{ documentId: number; chunkCount?: number; parseStatus?: string }>
          >('/api/kb/documents/upload', fd, {
            headers: { 'Content-Type': 'multipart/form-data' },
            timeout: 120_000,
          });
          if (data.code !== 0) {
            message.error(data.message);
            return;
          }
          saveMsg = formatIndexMessage(data.data);
        }
      } else {
        const body = {
          documentId: editing?.documentId,
          knowledgeBaseId: kbId,
          documentName: v.documentName,
          summary: v.summary,
          sourceType: editing?.sourceType ?? sourceType,
          content: v.content,
          sourceUrl: v.sourceUrl,
        };
        const { data } = await api.post<
          ApiEnvelope<{ documentId: number; chunkCount?: number; parseStatus?: string }>
        >('/api/kb/documents', body, { timeout: 120_000 });
        if (data.code !== 0) {
          message.error(data.message);
          return;
        }
        saveMsg = formatIndexMessage(data.data);
      }

      message.success(saveMsg);
      setOpen(false);
      setEditing(null);
      void load();
      void loadTree();
    }, message);

  const selectedTitle =
    treeData.find((n) => n.key === String(selectedKbId ?? 0))?.title?.toString() ?? '全部知识库';

  return (
    <div>
      <Typography.Title level={4}>文档管理</Typography.Title>
      <Layout style={{ background: 'transparent', minHeight: 520 }}>
        <Layout.Sider width={260} theme="light" style={{ borderRadius: 8, marginRight: 16, padding: 12 }}>
          <Typography.Text strong>知识库</Typography.Text>
          {treeLoading ? (
            <div style={{ marginTop: 12 }}>加载中…</div>
          ) : treeData.length ? (
            <Tree
              style={{ marginTop: 8 }}
              treeData={treeData}
              selectedKeys={[String(selectedKbId ?? 0)]}
              defaultExpandAll
              onSelect={(keys) => {
                const k = keys[0] != null ? Number(keys[0]) : 0;
                setSelectedKbId(k);
                setPage((p) => ({ ...p, current: 1 }));
              }}
            />
          ) : (
            <Empty description="暂无知识库" style={{ marginTop: 24 }} />
          )}
        </Layout.Sider>
        <Layout.Content>
          <Space style={{ marginBottom: 12 }} wrap>
            <Access code="kb:document:add">
              <Button type="primary" onClick={openCreate} disabled={!kbIdForQuery}>
                新建文档
              </Button>
            </Access>
            <Input.Search
              placeholder="搜索文档名/简述"
              allowClear
              style={{ width: 220 }}
              onSearch={(v) => {
                setKeyword(v);
                setPage((p) => ({ ...p, current: 1 }));
              }}
            />
            <Typography.Text type="secondary">当前：{selectedTitle}</Typography.Text>
            <Button onClick={() => void load()} loading={loading}>
              刷新
            </Button>
          </Space>
          <Table<Row>
            rowKey={(r) => String(r.documentId)}
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
        </Layout.Content>
      </Layout>

      <AppModal
        title={editing ? '编辑文档' : '新建文档'}
        open={open}
        onCancel={() => {
          setOpen(false);
          setEditing(null);
        }}
        onOk={() => void submit()}
        width={720}
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="knowledgeBaseId" hidden>
            <Input />
          </Form.Item>
          <Form.Item name="documentName" label="文档名称" rules={[{ required: true }]}>
            <Input placeholder="文档标题" />
          </Form.Item>
          <Form.Item name="summary" label="简述">
            <Input.TextArea rows={2} placeholder="一句话描述文档用途或内容" maxLength={500} showCount />
          </Form.Item>
          {!editing && (
            <Form.Item label="录入方式">
              <Radio.Group
                value={sourceType}
                onChange={(e) => setSourceType(e.target.value)}
                optionType="button"
                options={[
                  { value: 'manual', label: '手动录入' },
                  { value: 'upload', label: '文件上传' },
                  { value: 'url', label: 'URL 下载' },
                ]}
              />
            </Form.Item>
          )}
          {editing && (
            <Form.Item label="来源">
              <Tag>{SOURCE_LABEL[editing.sourceType ?? ''] ?? editing.sourceType}</Tag>
            </Form.Item>
          )}
          {(sourceType === 'manual' || editing?.sourceType === 'manual') && (
            <Form.Item name="content" label="正文" rules={editing ? [] : [{ required: true, message: '请填写正文' }]}>
              <Input.TextArea rows={10} placeholder="支持 Markdown / 纯文本" />
            </Form.Item>
          )}
          {(sourceType === 'url' || editing?.sourceType === 'url') && !editing && (
            <Form.Item name="sourceUrl" label="下载地址" rules={[{ required: true, type: 'url', message: '请输入有效 URL' }]}>
              <Input placeholder="https://example.com/doc.pdf" />
            </Form.Item>
          )}
          {editing?.sourceType === 'url' && editing.sourceUrl && (
            <Form.Item label="下载地址">
              <Typography.Link href={editing.sourceUrl} target="_blank">
                {editing.sourceUrl}
              </Typography.Link>
            </Form.Item>
          )}
          {(sourceType === 'upload' || editing?.sourceType === 'upload') && (
            <Form.Item label="文件" required={!editing}>
              <Upload.Dragger
                maxCount={1}
                beforeUpload={() => false}
                fileList={uploadFile ? [uploadFile] : []}
                onChange={({ fileList }) => setUploadFile(fileList[0] ?? null)}
              >
                <p>点击或拖拽文件到此处</p>
                <p style={{ color: '#888' }}>支持 txt、md、pdf、doc、docx、html、json、csv（最大 50MB）</p>
              </Upload.Dragger>
              {editing?.fileUrl && (
                <Typography.Link href={editing.fileUrl} style={{ marginTop: 8, display: 'block' }}>
                  下载当前文件
                </Typography.Link>
              )}
            </Form.Item>
          )}
        </Form>
      </AppModal>
    </div>
  );
}
