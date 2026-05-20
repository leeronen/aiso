import { Access } from '../../components/Access';
import { useSyncModalForm } from '../../hooks/useModalForm';
import { AppModal } from '../../components/AppModal';
import { App, Button, Form, Input, Space, Switch, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { api } from '../../api/client';
import { runApi } from '../../api/runApi';
import { reportApiError } from '../../utils/http';
import type { ApiEnvelope } from '../../api/types';

type Row = {
  userId: number;
  username: string;
  nickname?: string;
  email?: string;
  phone?: string;
  status?: number;
  admin?: boolean;
};

type Page<T> = { records: T[]; total: number; current: number; size: number };

type FormValues = {
  username?: string;
  password?: string;
  nickname?: string;
  email?: string;
  phone?: string;
  status?: number;
  admin?: boolean;
};

export function UsersPage() {
  const { message, modal } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<Row[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState({ current: 1, size: 10 });
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Row | null>(null);
  const [formValues, setFormValues] = useState<Partial<FormValues> | null>(null);
  const [form] = Form.useForm<FormValues>();

  const modalValues = useMemo(() => {
    if (!open) return null;
    return formValues;
  }, [open, formValues]);

  useSyncModalForm(open, form, modalValues);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiEnvelope<Page<Row>>>('/api/system/users', {
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
    setFormValues(null);
  };

  const openCreate = () => {
    setEditing(null);
    setFormValues({ status: 1, admin: false });
    setOpen(true);
  };

  const openEdit = async (row: Row) => {
    setEditing(row);
    try {
      const { data } = await api.get<ApiEnvelope<Row>>(`/api/system/users/${row.userId}`);
      if (data.code !== 0) {
        message.error(data.message);
        return;
      }
      setFormValues({
        username: data.data.username,
        nickname: data.data.nickname,
        email: data.data.email,
        phone: data.data.phone,
        status: data.data.status ?? 1,
        admin: data.data.admin ?? false,
        password: '',
      });
      setOpen(true);
    } catch {
      message.error('加载用户详情失败');
    }
  };

  const handleDelete = (row: Row) => {
    modal.confirm({
      title: '确认删除',
      content: `确定删除用户「${row.username}」吗？`,
      okType: 'danger',
      onOk: () =>
        runApi(async () => {
          const { data } = await api.delete<ApiEnvelope<null>>(`/api/system/users/${row.userId}`);
          if (data.code !== 0) {
            message.error(data.message);
            return;
          }
          message.success('已删除');
          void load();
        }, message),
    });
  };

  const columns: ColumnsType<Row> = [
    { title: '用户名', dataIndex: 'username', width: 120 },
    { title: '昵称', dataIndex: 'nickname' },
    { title: '邮箱', dataIndex: 'email' },
    { title: '手机', dataIndex: 'phone' },
    {
      title: '角色',
      dataIndex: 'admin',
      width: 100,
      render: (v: boolean) =>
        v ? <Tag color="gold">管理员</Tag> : <Tag>普通用户</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (v) => (v === 1 ? <Tag color="success">启用</Tag> : <Tag color="default">停用</Tag>),
    },
    {
      title: '操作',
      width: 160,
      render: (_, r) => (
        <Space>
          <Access code="user:update">
            <Button type="link" onClick={() => void openEdit(r)}>
              编辑
            </Button>
          </Access>
          <Access code="user:delete">
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
      if (editing) {
        const body = {
          nickname: v.nickname,
          email: v.email,
          phone: v.phone,
          status: v.status,
          admin: v.admin,
          password: v.password?.trim() || undefined,
        };
        const { data } = await api.put<ApiEnvelope<null>>(`/api/system/users/${editing.userId}`, body);
        if (data.code !== 0) {
          message.error(data.message);
          return;
        }
        message.success('已保存');
      } else {
        const { data } = await api.post<ApiEnvelope<{ userId: number }>>('/api/system/users', {
          username: v.username,
          password: v.password,
          nickname: v.nickname,
          email: v.email,
          phone: v.phone,
          status: v.status ?? 1,
          admin: v.admin ?? false,
        });
        if (data.code !== 0) {
          message.error(data.message);
          return;
        }
        message.success('已创建用户');
      }
      closeModal();
      void load();
    }, message);

  const isEdit = !!editing;

  return (
    <div>
      <Typography.Title level={4}>用户管理</Typography.Title>
      <Space style={{ marginBottom: 12 }}>
        <Access code="user:add">
          <Button type="primary" onClick={openCreate}>
            新建用户
          </Button>
        </Access>
      </Space>
      <Table<Row>
        rowKey={(r) => String(r.userId)}
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
        title={isEdit ? '编辑用户' : '新建用户'}
        open={open}
        onCancel={closeModal}
        onOk={() => void submit()}
        width={520}
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item
            name="username"
            label="用户名"
            rules={[{ required: !isEdit, message: '请输入用户名' }]}
          >
            <Input disabled={isEdit} placeholder="登录账号" autoComplete="username" />
          </Form.Item>
          <Form.Item
            name="password"
            label={isEdit ? '新密码（留空不修改）' : '密码'}
            rules={isEdit ? [] : [{ required: true, min: 6, message: '密码至少 6 位' }]}
          >
            <Input.Password
              autoComplete="new-password"
              placeholder={isEdit ? '不修改请留空' : '至少 6 位'}
            />
          </Form.Item>
          <Form.Item name="nickname" label="昵称">
            <Input autoComplete="nickname" />
          </Form.Item>
          <Form.Item name="email" label="邮箱">
            <Input autoComplete="email" />
          </Form.Item>
          <Form.Item name="phone" label="手机号">
            <Input autoComplete="tel" />
          </Form.Item>
          <Form.Item
            name="admin"
            label="超级管理员"
            valuePropName="checked"
            tooltip="拥有 SUPER_ADMIN 角色，具备系统全部 API 权限"
          >
            <Switch checkedChildren="是" unCheckedChildren="否" />
          </Form.Item>
          <Form.Item
            name="status"
            label="账号状态"
            valuePropName="checked"
            getValueFromEvent={(c) => (c ? 1 : 0)}
            getValueProps={(v) => ({ checked: (v ?? 1) === 1 })}
          >
            <Switch checkedChildren="启用" unCheckedChildren="停用" />
          </Form.Item>
        </Form>
      </AppModal>
    </div>
  );
}
