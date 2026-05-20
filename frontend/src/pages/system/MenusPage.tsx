import { Access } from '../../components/Access';
import { AppModal } from '../../components/AppModal';
import { menuIcon } from '../../utils/menuIcons';
import { App, Button, Form, Input, InputNumber, Space, Switch, Table, Tag, TreeSelect, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { DataNode } from 'antd/es/tree-select';
import { useSyncModalForm } from '../../hooks/useModalForm';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { api } from '../../api/client';
import { runApi } from '../../api/runApi';
import { reportApiError } from '../../utils/http';
import type { ApiEnvelope } from '../../api/types';

type MenuNode = {
  menuId: number;
  parentId?: number;
  menuName: string;
  path?: string;
  icon?: string;
  sortOrder?: number;
  permissionCode?: string;
  visible?: number;
  status?: number;
  children?: MenuNode[];
};

export function MenusPage() {
  const { message, modal } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [tree, setTree] = useState<MenuNode[]>([]);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<MenuNode | null>(null);
  const [createParentId, setCreateParentId] = useState(0);
  const [form] = Form.useForm<MenuNode>();

  const modalValues = useMemo(() => {
    if (!open) return null;
    if (editing) return editing;
    return { parentId: createParentId, visible: 1, status: 1, sortOrder: 0 };
  }, [open, editing, createParentId]);

  useSyncModalForm(open, form, modalValues);

  const parentTreeData = useMemo(
    () => buildParentTreeOptions(tree, editing?.menuId),
    [tree, editing?.menuId],
  );

  const closeModal = () => {
    setOpen(false);
    setEditing(null);
  };

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiEnvelope<MenuNode[]>>('/api/system/menus/tree');
      if (data.code !== 0) {
        message.error(data.message);
        return;
      }
      setTree(normalizeTree(data.data));
    } catch (e) {
      reportApiError(e, message);
    } finally {
      setLoading(false);
    }
  }, [message]);

  useEffect(() => {
    void load();
  }, [load]);

  const openEdit = (row: MenuNode) => {
    setEditing(row);
    setOpen(true);
  };

  const openCreate = (parentId?: number) => {
    setEditing(null);
    setCreateParentId(parentId ?? 0);
    setOpen(true);
  };

  const submit = () =>
    void runApi(async () => {
      const v = await form.validateFields();
      if (editing) {
        const { data } = await api.put<ApiEnvelope<unknown>>(`/api/system/menus/${editing.menuId}`, v);
        if (data.code !== 0) {
          message.error(data.message);
          return;
        }
      } else {
        const { data } = await api.post<ApiEnvelope<unknown>>('/api/system/menus', v);
        if (data.code !== 0) {
          message.error(data.message);
          return;
        }
      }
      message.success('已保存');
      closeModal();
      void load();
    }, message);

  const handleDelete = (row: MenuNode) => {
    modal.confirm({
      title: '确认删除',
      content: row.children?.length
        ? `「${row.menuName}」下有子菜单，请先删除子菜单。`
        : `确定删除菜单「${row.menuName}」吗？`,
      okType: 'danger',
      okButtonProps: { disabled: !!row.children?.length },
      onOk: () =>
        runApi(async () => {
          const { data } = await api.delete<ApiEnvelope<null>>(`/api/system/menus/${row.menuId}`);
          if (data.code !== 0) {
            message.error(data.message);
            return;
          }
          message.success('已删除');
          void load();
        }, message),
    });
  };

  const columns: ColumnsType<MenuNode> = [
    {
      title: '菜单名称',
      dataIndex: 'menuName',
      width: 280,
      render: (_, r) => (
        <Space size={8}>
          <span style={{ color: 'rgba(0,0,0,0.45)', display: 'inline-flex' }}>{menuIcon(r.icon)}</span>
          <span style={{ fontWeight: r.children?.length ? 600 : 400 }}>{r.menuName}</span>
        </Space>
      ),
    },
    {
      title: '路由路径',
      dataIndex: 'path',
      ellipsis: true,
      render: (v) => v || <Typography.Text type="secondary">—</Typography.Text>,
    },
    {
      title: '权限码',
      dataIndex: 'permissionCode',
      width: 140,
      ellipsis: true,
      render: (v) => v || '—',
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      width: 72,
      align: 'center',
    },
    {
      title: '状态',
      width: 120,
      render: (_, r) => (
        <Space size={4}>
          {r.visible === 1 ? <Tag color="blue">显示</Tag> : <Tag>隐藏</Tag>}
          {r.status === 1 ? <Tag color="success">启用</Tag> : <Tag color="default">停用</Tag>}
        </Space>
      ),
    },
    {
      title: '操作',
      width: 220,
      fixed: 'right',
      render: (_, r) => (
        <Space size={0} wrap>
          <Access code="menu:add">
            <Button type="link" size="small" onClick={() => openCreate(r.menuId)}>
              子菜单
            </Button>
          </Access>
          <Access code="menu:update">
            <Button type="link" size="small" onClick={() => openEdit(r)}>
              编辑
            </Button>
          </Access>
          <Access code="menu:delete">
            <Button type="link" size="small" danger onClick={() => handleDelete(r)}>
              删除
            </Button>
          </Access>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Typography.Title level={4} style={{ marginBottom: 12 }}>
        菜单管理
      </Typography.Title>
      <Typography.Paragraph type="secondary" style={{ marginBottom: 16 }}>
        树形展示菜单层级；可展开/收起，支持在任意节点下新建子菜单。
      </Typography.Paragraph>
      <Space style={{ marginBottom: 16 }}>
        <Access code="menu:add">
          <Button type="primary" onClick={() => openCreate(0)}>
            新建顶级菜单
          </Button>
        </Access>
        <Button onClick={() => void load()} loading={loading}>
          刷新
        </Button>
      </Space>
      <Table<MenuNode>
        rowKey="menuId"
        loading={loading}
        columns={columns}
        dataSource={tree}
        pagination={false}
        defaultExpandAllRows
        indentSize={28}
        scroll={{ x: 900 }}
        size="middle"
      />
      <AppModal
        title={editing ? '编辑菜单' : '新建菜单'}
        open={open}
        onCancel={closeModal}
        onOk={() => void submit()}
        width={560}
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="parentId" label="上级菜单" rules={[{ required: true }]}>
            <TreeSelect
              treeData={parentTreeData}
              treeDefaultExpandAll
              placeholder="选择上级菜单"
              style={{ width: '100%' }}
            />
          </Form.Item>
          <Form.Item name="menuName" label="菜单名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="path" label="路由路径">
            <Input placeholder="/system/xxx" />
          </Form.Item>
          <Form.Item name="icon" label="图标">
            <Input placeholder="SettingOutlined" />
          </Form.Item>
          <Form.Item name="permissionCode" label="可见权限码">
            <Input placeholder="user:view" />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item
            name="visible"
            label="显示"
            valuePropName="checked"
            getValueFromEvent={(c) => (c ? 1 : 0)}
            getValueProps={(v) => ({ checked: (v ?? 1) === 1 })}
          >
            <Switch />
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

/** 去掉空 children，便于 Table 树形渲染 */
function normalizeTree(nodes: MenuNode[]): MenuNode[] {
  return nodes.map((n) => {
    const children = n.children?.length ? normalizeTree(n.children) : undefined;
    return children?.length ? { ...n, children } : { ...n, children: undefined };
  });
}

function buildParentTreeOptions(nodes: MenuNode[], excludeMenuId?: number): DataNode[] {
  const blocked = excludeMenuId ? collectBlockedIds(nodes, excludeMenuId) : new Set<number>();

  const mapNodes = (list: MenuNode[]): DataNode[] =>
    list
      .filter((n) => !blocked.has(n.menuId))
      .map((n) => ({
        value: n.menuId,
        title: n.menuName,
        children: n.children?.length ? mapNodes(n.children) : undefined,
      }));

  return [
    {
      value: 0,
      title: '顶级菜单',
      children: mapNodes(nodes),
    },
  ];
}

/** 编辑时禁止选自身或任意子孙为父级 */
function collectBlockedIds(nodes: MenuNode[], targetId: number): Set<number> {
  const blocked = new Set<number>([targetId]);

  const collectSubtree = (n: MenuNode) => {
    blocked.add(n.menuId);
    n.children?.forEach(collectSubtree);
  };

  const findTarget = (list: MenuNode[]): boolean => {
    for (const n of list) {
      if (n.menuId === targetId) {
        collectSubtree(n);
        return true;
      }
      if (n.children?.length && findTarget(n.children)) {
        return true;
      }
    }
    return false;
  };

  findTarget(nodes);
  return blocked;
}
