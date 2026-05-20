import { Access } from '../../components/Access';
import { AppModal } from '../../components/AppModal';
import { App, Button, Form, Input, Select, Space, Switch, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useSyncModalForm } from '../../hooks/useModalForm';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { api } from '../../api/client';
import { runApi } from '../../api/runApi';
import { reportApiError } from '../../utils/http';
import type { ApiEnvelope } from '../../api/types';

type RoleRow = {
  roleId: number;
  roleName: string;
  roleCode: string;
  description?: string;
  status?: number;
};

type PermOption = { permissionId: number; permissionName: string; permissionCode: string };

type Page<T> = { records: T[]; total: number };

export function RolesPage() {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<RoleRow[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState({ current: 1, size: 10 });
  const [permOptions, setPermOptions] = useState<PermOption[]>([]);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<RoleRow | null>(null);
  const [formSeed, setFormSeed] = useState<Partial<RoleRow & { permissionIds?: number[] }> | null>(
    null,
  );
  const [form] = Form.useForm<RoleRow & { permissionIds?: number[] }>();

  const modalValues = useMemo(() => {
    if (!open) return null;
    return formSeed ?? { status: 1, permissionIds: [] };
  }, [open, formSeed]);

  useSyncModalForm(open, form, modalValues);

  const closeModal = () => {
    setOpen(false);
    setEditing(null);
    setFormSeed(null);
  };

  const loadPerms = useCallback(async () => {
    try {
      const { data } = await api.get<ApiEnvelope<Page<PermOption>>>('/api/system/permissions', {
        params: { current: 1, size: 500 },
      });
      if (data.code === 0) {
        setPermOptions(data.data.records);
      }
    } catch (e) {
      reportApiError(e, message);
    }
  }, [message]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiEnvelope<Page<RoleRow>>>('/api/system/roles', {
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
    void loadPerms();
    void load();
  }, [load, loadPerms]);

  const openEdit = async (row: RoleRow) => {
    try {
      const { data } = await api.get<ApiEnvelope<RoleRow & { permissionIds: number[] }>>(
        `/api/system/roles/${row.roleId}`,
      );
      if (data.code !== 0) {
        message.error(data.message);
        return;
      }
      setEditing(row);
      setFormSeed({
        roleName: data.data.roleName,
        roleCode: data.data.roleCode,
        description: data.data.description,
        status: data.data.status,
        permissionIds: data.data.permissionIds,
      });
      setOpen(true);
    } catch (e) {
      reportApiError(e, message);
    }
  };

  const columns: ColumnsType<RoleRow> = [
    { title: '角色名', dataIndex: 'roleName' },
    { title: '编码', dataIndex: 'roleCode' },
    {
      title: '状态',
      dataIndex: 'status',
      render: (v) => (v === 1 ? <Tag color="success">启用</Tag> : <Tag>停用</Tag>),
    },
    {
      title: '操作',
      width: 140,
      render: (_, r) => (
        <Space>
          <Access code="role:update">
            <Button type="link" onClick={() => void openEdit(r)}>
              编辑
            </Button>
          </Access>
          <Access code="role:delete">
            <Button
              type="link"
              danger
              disabled={r.roleCode === 'SUPER_ADMIN'}
              onClick={() => void handleDelete(r)}
            >
              删除
            </Button>
          </Access>
        </Space>
      ),
    },
  ];

  const handleDelete = (row: RoleRow) => {
    void runApi(async () => {
      const { data } = await api.delete<ApiEnvelope<null>>(`/api/system/roles/${row.roleId}`);
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
      const body = {
        roleName: v.roleName,
        roleCode: v.roleCode,
        description: v.description,
        status: v.status,
        permissionIds: v.permissionIds ?? [],
      };
      if (editing) {
        const { data } = await api.put<ApiEnvelope<unknown>>(`/api/system/roles/${editing.roleId}`, body);
        if (data.code !== 0) {
          message.error(data.message);
          return;
        }
      } else {
        const { data } = await api.post<ApiEnvelope<unknown>>('/api/system/roles', body);
        if (data.code !== 0) {
          message.error(data.message);
          return;
        }
      }
      message.success('已保存');
      closeModal();
      void load();
    }, message);

  return (
    <div>
      <Typography.Title level={4}>角色管理</Typography.Title>
      <Access code="role:add">
        <Button
          type="primary"
          style={{ marginBottom: 12 }}
          onClick={() => {
            setEditing(null);
            setFormSeed({ status: 1, permissionIds: [] });
            setOpen(true);
          }}
        >
          新建角色
        </Button>
      </Access>
      <Table<RoleRow>
        rowKey={(r) => String(r.roleId)}
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
        title={editing ? '编辑角色' : '新建角色'}
        open={open}
        onCancel={closeModal}
        onOk={() => void submit()}
        width={640}
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="roleName" label="角色名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="roleCode" label="角色编码" rules={[{ required: true }]}>
            <Input disabled={editing?.roleCode === 'SUPER_ADMIN'} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="status" label="启用" valuePropName="checked" getValueFromEvent={(c) => (c ? 1 : 0)} getValueProps={(v) => ({ checked: (v ?? 1) === 1 })}>
            <Switch />
          </Form.Item>
          <Access anyOf={['role:assign', 'role:update', 'role:add']}>
            <Form.Item name="permissionIds" label="分配权限">
              <Select
                mode="multiple"
                optionFilterProp="label"
                options={permOptions.map((p) => ({
                  value: p.permissionId,
                  label: `${p.permissionName} (${p.permissionCode})`,
                }))}
              />
            </Form.Item>
          </Access>
        </Form>
      </AppModal>
    </div>
  );
}
