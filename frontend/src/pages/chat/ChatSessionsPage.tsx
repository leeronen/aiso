import { Access } from '../../components/Access';
import { ChatWorkflowPanel, type WorkflowItem } from '../../components/chat/ChatWorkflowPanel';
import { App, Button, Drawer, Input, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useCallback, useEffect, useRef, useState } from 'react';
import { api } from '../../api/client';
import { runApi } from '../../api/runApi';
import { reportApiError } from '../../utils/http';
import type { ApiEnvelope } from '../../api/types';
import './ChatSessionsPage.css';

type Session = {
  sessionId: number;
  userId?: number;
  agentId?: number;
  workflowId?: number;
  workflowName?: string;
  sessionTitle?: string;
  sessionStatus?: string;
};

type Msg = {
  messageId: number;
  roleType: string;
  content: string;
  tokenCount?: number;
  responseTime?: number;
};

type TokenStats = {
  sessionId: number;
  messageTotalTokens: number;
  usageRecordPromptTokens: number;
  usageRecordCompletionTokens: number;
  usageRecordTotalTokens: number;
  llmCallCount: number;
};

type Page<T> = { records: T[]; total: number; current: number; size: number };

export function ChatSessionsPage() {
  const { message, modal } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<Session[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState({ current: 1, size: 10 });
  const [sel, setSel] = useState<Session | null>(null);
  const [msgs, setMsgs] = useState<Msg[]>([]);
  const [tokenStats, setTokenStats] = useState<TokenStats | null>(null);
  const [draft, setDraft] = useState('');
  const [selectedWorkflowId, setSelectedWorkflowId] = useState<number | null>(null);
  const [selectedWorkflow, setSelectedWorkflow] = useState<WorkflowItem | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiEnvelope<Page<Session>>>('/api/chat/sessions', {
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

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [msgs, sel?.sessionId]);

  const onSelectWorkflow = (id: number | null, item?: WorkflowItem) => {
    setSelectedWorkflowId(id);
    setSelectedWorkflow(item ?? null);
  };

  const loadSessionDetail = async (sessionId: number) => {
    const [msgRes, statRes] = await Promise.all([
      api.get<ApiEnvelope<Msg[]>>(`/api/chat/sessions/${sessionId}/messages`),
      api.get<ApiEnvelope<TokenStats>>(`/api/chat/sessions/${sessionId}/token-stats`),
    ]);
    if (msgRes.data.code === 0) setMsgs(msgRes.data.data);
    if (statRes.data.code === 0) setTokenStats(statRes.data.data);
  };

  const open = (s: Session) => {
    setSel(s);
    setTokenStats(null);
    void runApi(() => loadSessionDetail(s.sessionId), message);
  };

  const send = () => {
    if (!sel || !draft.trim()) return;
    void runApi(async () => {
      const { data } = await api.post<ApiEnvelope<Msg[]>>(`/api/chat/sessions/${sel.sessionId}/messages`, {
        content: draft,
      });
      if (data.code !== 0) {
        message.error(data.message);
        return;
      }
      setMsgs(data.data);
      setDraft('');
      if (sel) void loadSessionDetail(sel.sessionId);
      void load();
    }, message);
  };

  const newSession = () => {
    if (!selectedWorkflowId) {
      message.warning('请先在左侧选择工作流');
      return;
    }
    void runApi(async () => {
      const title = selectedWorkflow?.workflowName
        ? `${selectedWorkflow.workflowName} · 会话`
        : '新会话';
      const { data } = await api.post<ApiEnvelope<{ sessionId: number }>>('/api/chat/sessions', {
        title,
        workflowId: selectedWorkflowId,
        agentId: null,
      });
      if (data.code !== 0) {
        message.error(data.message);
        return;
      }
      message.success('已创建会话');
      void load();
    }, message);
  };

  const deleteSession = (row: Session) => {
    void modal.confirm({
      title: '删除会话？',
      content: '会话消息将一并删除。',
      okType: 'danger',
      onOk: () =>
        runApi(async () => {
          const { data } = await api.delete<ApiEnvelope<null>>(`/api/chat/sessions/${row.sessionId}`);
          if (data.code !== 0) {
            message.error(data.message);
            return;
          }
          message.success('已删除');
          if (sel?.sessionId === row.sessionId) setSel(null);
          void load();
        }, message),
    });
  };

  const columns: ColumnsType<Session> = [
    { title: '标题', dataIndex: 'sessionTitle', ellipsis: true },
    {
      title: '工作流',
      dataIndex: 'workflowName',
      width: 140,
      ellipsis: true,
      render: (v) => v || '—',
    },
    { title: 'Agent', dataIndex: 'agentId', width: 80 },
    { title: '状态', dataIndex: 'sessionStatus', width: 90 },
    {
      title: '操作',
      width: 140,
      render: (_, r) => (
        <Space>
          <Button type="link" onClick={() => open(r)}>
            打开
          </Button>
          <Access code="chat:session:delete">
            <Button type="link" danger onClick={() => deleteSession(r)}>
              删除
            </Button>
          </Access>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Typography.Title level={4}>聊天中心</Typography.Title>
      {selectedWorkflow ? (
        <Typography.Paragraph type="secondary" style={{ marginBottom: 12 }}>
          当前工作流：<Tag color="blue">{selectedWorkflow.workflowName}</Tag>
        </Typography.Paragraph>
      ) : (
        <Typography.Paragraph type="secondary" style={{ marginBottom: 12 }}>
          请从左侧选择工作流后再新建会话
        </Typography.Paragraph>
      )}
      <div className="chat-center-layout">
        <ChatWorkflowPanel selectedId={selectedWorkflowId} onSelect={onSelectWorkflow} />
        <div className="chat-sessions-main">
          <Space style={{ marginBottom: 12 }}>
            <Access code="chat:session:add">
              <Button type="primary" onClick={newSession} disabled={!selectedWorkflowId}>
                新建会话
              </Button>
            </Access>
            <Button onClick={() => void load()} loading={loading}>
              刷新
            </Button>
          </Space>
          <Table<Session>
            rowKey={(r) => String(r.sessionId)}
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
        </div>
      </div>
      <Drawer
        title={sel?.sessionTitle ?? '会话'}
        width={560}
        open={!!sel}
        onClose={() => setSel(null)}
        className="chat-session-drawer"
      >
        {sel?.workflowName ? (
          <Typography.Paragraph type="secondary">工作流：{sel.workflowName}</Typography.Paragraph>
        ) : null}
        {tokenStats ? (
          <Typography.Paragraph type="secondary" style={{ marginBottom: 8 }}>
            会话 Token：消息合计 {tokenStats.messageTotalTokens} · LLM 调用 {tokenStats.llmCallCount} 次 · 输入{' '}
            {tokenStats.usageRecordPromptTokens} / 输出 {tokenStats.usageRecordCompletionTokens} · 合计{' '}
            {tokenStats.usageRecordTotalTokens}
          </Typography.Paragraph>
        ) : null}
        <div className="chat-drawer-messages">
          {msgs.map((m) => {
            const isUser = m.roleType === 'user';
            return (
              <div
                key={m.messageId}
                className={`chat-bubble-row ${isUser ? 'chat-bubble-row--user' : 'chat-bubble-row--assistant'}`}
              >
                <div className={`chat-bubble ${isUser ? 'chat-bubble--user' : 'chat-bubble--assistant'}`}>
                  <div className="chat-bubble-label">{isUser ? '客人' : '系统'}</div>
                  <div className="chat-bubble-content">{m.content}</div>
                  {m.tokenCount != null && m.tokenCount > 0 ? (
                    <div className="chat-bubble-meta">
                      {m.tokenCount} tokens
                      {m.responseTime != null && m.responseTime > 0 ? ` · ${m.responseTime}ms` : ''}
                    </div>
                  ) : null}
                </div>
              </div>
            );
          })}
          <div ref={messagesEndRef} />
        </div>
        <Space.Compact className="chat-drawer-input">
          <Input value={draft} onChange={(e) => setDraft(e.target.value)} placeholder="输入消息" onPressEnter={send} />
          <Button type="primary" onClick={send}>
            发送
          </Button>
        </Space.Compact>
      </Drawer>
    </div>
  );
}
