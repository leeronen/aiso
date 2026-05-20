import { Access } from '../../components/Access';
import { AppModal } from '../../components/AppModal';
import { App, Button, Form, Input, Space, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useSyncModalForm } from '../../hooks/useModalForm';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { api } from '../../api/client';
import { runApi } from '../../api/runApi';
import { reportApiError } from '../../utils/http';
import type { ApiEnvelope } from '../../api/types';

type Row = {
  permissionId: number;
  permissionName: string;
  permissionCode: string;
  permissionType?: string;
};

type Page<T> = { records: T[]; total: number };

export function PermissionsPage() {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<Row[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState({ current: 1, size: 20 });
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Row | null>(null);
  const [form] = Form.useForm<Row>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiEnvelope<Page<Row>>>('/api/system/permissions', {
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
    { title: '权限名称', dataIndex: 'permissionName' },
    { title: '权限编码', dataIndex: 'permissionCode', width: 220 },
    { title: '类型', dataIndex: 'permissionType', width: 80 },
    {
      title: '操作',
      width: 140,
      render: (_, r) => (
        <Space>
          <Access code="permission:update">
            <Button type="link" onClick={() => { setEditing(r); setOpen(true); }}>
              编辑
            </Button>
          </Access>
          <Access code="permission:delete">
            <Button type="link" danger onClick={() => void handleDelete(r)}>
              删除
            </Button>
          </Access>
        </Space>
      ),
    },
  ];

  const handleDelete = (row: Row) => {
    void runApi(async () => {
      const { data } = await api.delete<ApiEnvelope<null>>(`/api/system/permissions/${row.permissionId}`);
      if (data.code !== 0) {
        message.error(data.message);
        return;
      }
      message.success('已删除');
      void load();
    }, message);
  };

  const submit = () =>
    void runApi(async () => {
      const v = await form.validateFields();
      if (editing) {
        const { data } = await api.put<ApiEnvelope<unknown>>(`/api/system/permissions/${editing.permissionId}`, v);
        if (data.code !== 0) {
          message.error(data.message);
          return;
        }
      } else {
        const { data } = await api.post<ApiEnvelope<unknown>>('/api/system/permissions', v);
        if (data.code !== 0) {
          message.error(data.message);
          return;
        }
      }
      message.success('已保存');
      setOpen(false);
      setEditing(null);
      form.resetFields();
      void load();
    }, message);

  return (
    <div>
      <Typography.Title level={4}>权限管理</Typography.Title>
      <Access code="permission:add">
        <Button
          type="primary"
          style={{ marginBottom: 12 }}
          onClick={() => {
            setEditing(null);
            setOpen(true);
          }}
        >
          新建权限
        </Button>
      </Access>
      <Table<Row>
        rowKey={(r) => String(r.permissionId)}
        loading={loading}
        columns={columns}
        dataSource={rows}
        pagination={{
          current: page.current,
          pageSize: page.size,
          total,
          onChange: (c, s) => setPage({ current: c, size: s }),
        }}
      />
      <AppModal
        title={editing ? '编辑权限' : '新建权限'}
        open={open}
        onCancel={() => { setOpen(false); setEditing(null); }}
        onOk={() => void submit()}
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="permissionName" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="permissionCode" label="编码" rules={[{ required: true }]}>
            <Input disabled={!!editing} placeholder="如 user:view" />
          </Form.Item>
          <Form.Item name="permissionType" label="类型">
            <Input />
          </Form.Item>
        </Form>
      </AppModal>
    </div>
  );
}
