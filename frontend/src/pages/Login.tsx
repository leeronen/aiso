import { App, Button, Card, Form, Input, Tabs, Typography } from 'antd';
import { api } from '../api/client';
import type { ApiEnvelope } from '../api/types';
import { useAuthStore } from '../store/auth';
import { useNavigate } from 'react-router-dom';
import { getApiErrorMessage, isRequestAborted, reportApiError } from '../utils/http';

type TokenData = { accessToken: string; refreshToken: string; expiresInSeconds: number };

export function LoginPage() {
  const nav = useNavigate();
  const setTokens = useAuthStore((s) => s.setTokens);
  const { message } = App.useApp();

  const onLogin = async (v: { username: string; password: string }) => {
    try {
      const { data } = await api.post<ApiEnvelope<TokenData>>('/api/auth/login', v);
      if (data.code !== 0) {
        message.error(data.message);
        return;
      }
      setTokens(data.data.accessToken, data.data.refreshToken);
      message.success('登录成功');
      nav('/dashboard');
    } catch (e: unknown) {
      if (!isRequestAborted(e)) {
        message.error(getApiErrorMessage(e));
      }
    }
  };

  const onRegister = async (v: { username: string; password: string; email: string }) => {
    try {
      const { data } = await api.post<ApiEnvelope<null>>('/api/auth/register', v);
      if (data.code !== 0) {
        message.error(data.message);
        return;
      }
      message.success('注册成功，请登录');
    } catch (e: unknown) {
      reportApiError(e, message);
    }
  };

  return (
    <LoginShell>
      <Card style={{ width: 420 }}>
        <Typography.Title level={3} style={{ textAlign: 'center' }}>
          AIOS 管理平台
        </Typography.Title>
        <Typography.Paragraph type="secondary" style={{ textAlign: 'center' }}>
          默认管理员 admin / admin（首次启动自动初始化）
        </Typography.Paragraph>
        <Tabs
          centered
          items={[
            {
              key: 'login',
              label: '登录',
              children: (
                <Form layout="vertical" onFinish={onLogin}>
                  <Form.Item name="username" label="账号" rules={[{ required: true }]}>
                    <Input autoComplete="username" />
                  </Form.Item>
                  <Form.Item name="password" label="密码" rules={[{ required: true }]}>
                    <Input.Password autoComplete="current-password" />
                  </Form.Item>
                  <Button type="primary" htmlType="submit" block>
                    登录
                  </Button>
                </Form>
              ),
            },
            {
              key: 'reg',
              label: '注册',
              children: (
                <Form layout="vertical" onFinish={onRegister}>
                  <Form.Item name="username" label="账号" rules={[{ required: true }]}>
                    <Input autoComplete="username" />
                  </Form.Item>
                  <Form.Item name="email" label="邮箱" rules={[{ required: true, type: 'email' }]}>
                    <Input autoComplete="email" />
                  </Form.Item>
                  <Form.Item name="password" label="密码" rules={[{ required: true, min: 6 }]}>
                    <Input.Password autoComplete="new-password" />
                  </Form.Item>
                  <Button type="primary" htmlType="submit" block>
                    注册
                  </Button>
                </Form>
              ),
            },
          ]}
        />
      </Card>
    </LoginShell>
  );
}

function LoginShell({ children }: { children: React.ReactNode }) {
  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg,#0f172a,#1e293b)',
      }}
    >
      {children}
    </div>
  );
}
