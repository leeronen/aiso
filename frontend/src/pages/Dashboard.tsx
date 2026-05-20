import { App, Button, Card, Col, Row, Spin, Statistic, Typography } from 'antd';
import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client';
import type { ApiEnvelope } from '../api/types';
import { useAuthStore } from '../store/auth';
import { getApiErrorMessage, isRequestAborted } from '../utils/http';

export type DashboardSummary = {
  userTotal?: number;
  modelTotal?: number;
  agentTotal?: number;
  knowledgeBaseTotal?: number;
  documentTotal?: number;
  sessionTotal?: number;
  messageTotal?: number;
  onlineUsers?: number;
  note?: string;
};

export function DashboardPage() {
  const { message } = App.useApp();
  const hydrated = useAuthStore((s) => s.hydrated);
  const accessToken = useAuthStore((s) => s.accessToken);
  const [loading, setLoading] = useState(false);
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const { data } = await api.get<ApiEnvelope<DashboardSummary>>('/api/dashboard/summary');
      if (data.code !== 0) {
        setError(data.message || '加载失败');
        message.error(data.message || '加载失败');
        return;
      }
      setSummary(data.data);
    } catch (e: unknown) {
      if (isRequestAborted(e)) return;
      const msg = getApiErrorMessage(e);
      setError(msg);
      message.error(msg);
    } finally {
      setLoading(false);
    }
  }, [message]);

  useEffect(() => {
    if (!hydrated || !accessToken) {
      return;
    }
    void load();
  }, [hydrated, accessToken, load]);

  if (!hydrated) {
    return (
      <div style={{ padding: 48, textAlign: 'center' }}>
        <Spin />
      </div>
    );
  }

  if (loading && !summary) {
    return (
      <div style={{ padding: 48 }}>
        <Spin spinning tip="正在加载统计数据…" size="large">
          <div style={{ minHeight: 160 }} />
        </Spin>
      </div>
    );
  }

  if (error && !summary) {
    return (
      <div>
        <Typography.Title level={4}>仪表盘</Typography.Title>
        <Typography.Paragraph type="danger">{error}</Typography.Paragraph>
        <Button type="primary" onClick={() => void load()}>
          重试
        </Button>
        <Typography.Paragraph type="secondary" style={{ marginTop: 12 }}>
          接口：GET /api/dashboard/summary（需登录且后端运行在 http://127.0.0.1:8080）
        </Typography.Paragraph>
      </div>
    );
  }

  const s = summary ?? {};
  const num = (k: keyof DashboardSummary) => Number(s[k] ?? 0);

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Typography.Title level={4} style={{ margin: 0 }}>
          仪表盘
        </Typography.Title>
        <Button loading={loading} onClick={() => void load()}>
          刷新
        </Button>
      </div>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} md={8} lg={6}>
          <Card>
            <Statistic title="用户总数" value={num('userTotal')} />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={8} lg={6}>
          <Card>
            <Statistic title="模型" value={num('modelTotal')} />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={8} lg={6}>
          <Card>
            <Statistic title="Agent" value={num('agentTotal')} />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={8} lg={6}>
          <Card>
            <Statistic title="知识库" value={num('knowledgeBaseTotal')} />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={8} lg={6}>
          <Card>
            <Statistic title="文档" value={num('documentTotal')} />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={8} lg={6}>
          <Card>
            <Statistic title="会话" value={num('sessionTotal')} />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={8} lg={6}>
          <Card>
            <Statistic title="消息" value={num('messageTotal')} />
          </Card>
        </Col>
      </Row>
      {s.note ? (
        <Typography.Paragraph type="secondary" style={{ marginTop: 16 }}>
          {s.note}
        </Typography.Paragraph>
      ) : null}
    </div>
  );
}
