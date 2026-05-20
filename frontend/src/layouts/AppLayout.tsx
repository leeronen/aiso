import { App as AntApp, Button, Layout, Menu } from 'antd';
import type { MenuProps } from 'antd';
import { LogoutOutlined } from '@ant-design/icons';
import type { CSSProperties } from 'react';
import { useEffect, useMemo, useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import type { ApiEnvelope } from '../api/types';
import { fetchAndStoreProfile } from '../api/profile';
import { useAuthStore } from '../store/auth';
import { isRequestAborted, reportApiError } from '../utils/http';
import { menuIcon } from '../utils/menuIcons';

const { Header, Sider, Content } = Layout;

type NavMenu = {
  menuId: number;
  menuName: string;
  path?: string;
  icon?: string;
  children?: NavMenu[];
};

const siderLogoStyle: CSSProperties = {
  height: 56,
  margin: 12,
  borderRadius: 8,
  background: 'rgba(255,255,255,0.12)',
  color: '#fff',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  fontWeight: 700,
  letterSpacing: 1,
};

const headerStyle: CSSProperties = {
  padding: '0 20px',
  background: '#fff',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'flex-end',
  gap: 12,
};

function toMenuItems(nodes: NavMenu[]): MenuProps['items'] {
  return nodes.map((n) => {
    const children = n.children?.length ? toMenuItems(n.children) : undefined;
    if (children?.length) {
      return {
        key: `group-${n.menuId}`,
        icon: menuIcon(n.icon),
        label: n.menuName,
        children,
      };
    }
    return {
      key: n.path || `menu-${n.menuId}`,
      icon: menuIcon(n.icon),
      label: n.menuName,
    };
  });
}

export function AppLayout() {
  const { message } = AntApp.useApp();
  const nav = useNavigate();
  const loc = useLocation();
  const clear = useAuthStore((s) => s.clear);
  const token = useAuthStore((s) => s.accessToken);
  const [navMenus, setNavMenus] = useState<NavMenu[]>([]);

  useEffect(() => {
    if (!token) {
      setNavMenus([]);
      return;
    }
    let active = true;
    const sessionToken = token;

    void (async () => {
      try {
        await fetchAndStoreProfile();
        if (!active || useAuthStore.getState().accessToken !== sessionToken) return;

        const { data } = await api.get<ApiEnvelope<NavMenu[]>>('/api/system/menus/nav');
        if (!active || useAuthStore.getState().accessToken !== sessionToken) return;
        if (data.code === 0) {
          setNavMenus(data.data);
        }
      } catch (e) {
        if (active && !isRequestAborted(e)) {
          reportApiError(e, message);
        }
      }
    })();

    return () => {
      active = false;
    };
  }, [token, message]);

  const menuItems = useMemo(() => toMenuItems(navMenus), [navMenus]);

  const onMenu: MenuProps['onClick'] = ({ key }) => {
    if (typeof key === 'string' && key.startsWith('/')) {
      nav(key);
    }
  };

  const handleLogout = () => {
    clear();
    nav('/login');
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider breakpoint="lg" collapsedWidth={64}>
        <div style={siderLogoStyle}>AIOS</div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[loc.pathname]}
          items={menuItems}
          onClick={onMenu}
        />
      </Sider>
      <Layout>
        <Header style={headerStyle}>
          <Button type="default" icon={<LogoutOutlined />} onClick={handleLogout}>
            退出
          </Button>
        </Header>
        <Content style={{ margin: 16 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
