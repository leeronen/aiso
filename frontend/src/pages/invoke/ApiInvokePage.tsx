import { Access } from '../../components/Access';
import {
  InvokeHeadersEditor,
  rowsToHeaderRecord,
  type HeaderRow,
} from '../../components/invoke/InvokeHeadersEditor';
import {
  WorkflowInvokeInput,
  buildInvokeBody,
  parseInvokeSchema,
} from '../../components/invoke/WorkflowInvokeInput';
import { IO_SCHEMA_TYPE_LABEL } from '../../constants/ioSchemaTemplates';
import { API_TIMEOUT_MS, api } from '../../api/client';
import { App, Button, Card, Col, Form, Radio, Row, Select, Space, Typography } from 'antd';
import axios from 'axios';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { runApi } from '../../api/runApi';
import type { ApiEnvelope } from '../../api/types';
import { createTraceId, TRACE_HEADER } from '../../utils/traceId';
import { reportApiError } from '../../utils/http';

type WorkflowOption = { value: number; label: string };
type CallMode = 'platform' | 'open';

type IoMeta = {
  workflowId: number;
  workflowName?: string;
  inputType?: string;
  outputType?: string;
  inputSchema?: string;
  outputSchema?: string;
  exampleInput?: unknown;
};

type InvokeResult = {
  workflowId: number;
  workflowName?: string;
  inputType?: string;
  outputType?: string;
  input?: unknown;
  output?: unknown;
  rawReply?: string;
  totalTokens?: number;
  elapsedMs?: number;
};

const PLATFORM_HEADERS: HeaderRow[] = [{ key: 'Content-Type', value: 'application/json' }];

const OPEN_HEADERS: HeaderRow[] = [
  { key: 'Content-Type', value: 'application/json' },
  { key: 'X-API-Key', value: 'aios-demo-key-change-me' },
];

export function ApiInvokePage() {
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const [workflowOptions, setWorkflowOptions] = useState<WorkflowOption[]>([]);
  const [workflowId, setWorkflowId] = useState<number | null>(null);
  const [ioMeta, setIoMeta] = useState<IoMeta | null>(null);
  const [loadingIo, setLoadingIo] = useState(false);
  const [invoking, setInvoking] = useState(false);
  const [result, setResult] = useState<InvokeResult | null>(null);
  const [callMode, setCallMode] = useState<CallMode>('platform');
  const [headers, setHeaders] = useState<HeaderRow[]>(PLATFORM_HEADERS);
  const [lastRequestUrl, setLastRequestUrl] = useState<string>('');

  const parsedSchema = useMemo(
    () => parseInvokeSchema(ioMeta?.inputSchema),
    [ioMeta?.inputSchema],
  );

  const loadWorkflowOptions = useCallback(async () => {
    try {
      const { data } = await api.get<ApiEnvelope<WorkflowOption[]>>('/api/ai/workflows/options', {
        params: { limit: 100 },
      });
      if (data.code === 0) setWorkflowOptions(data.data);
    } catch (e) {
      reportApiError(e, message);
    }
  }, [message]);

  useEffect(() => {
    void loadWorkflowOptions();
  }, [loadWorkflowOptions]);

  const fetchIoMeta = async (id: number, mode: CallMode, headerRows: HeaderRow[]) => {
    const headerRecord = {
      ...rowsToHeaderRecord(headerRows),
      [TRACE_HEADER]: createTraceId(),
    };
    if (mode === 'open') {
      const { data } = await axios.get<ApiEnvelope<IoMeta>>(`/open/v1/workflows/${id}/io`, {
        headers: headerRecord,
        timeout: API_TIMEOUT_MS,
      });
      return data;
    }
    const { data } = await api.get<ApiEnvelope<IoMeta>>(`/api/invoke/workflows/${id}/io`, {
      headers: headerRecord,
    });
    return data;
  };

  const onCallModeChange = (mode: CallMode) => {
    const nextHeaders = mode === 'open' ? [...OPEN_HEADERS] : [...PLATFORM_HEADERS];
    setCallMode(mode);
    setHeaders(nextHeaders);
    if (workflowId) {
      setLoadingIo(true);
      void (async () => {
        try {
          const data = await fetchIoMeta(workflowId, mode, nextHeaders);
          if (data.code !== 0) message.error(data.message);
        } catch (e) {
          reportApiError(e, message);
        } finally {
          setLoadingIo(false);
        }
      })();
    }
  };

  const onWorkflowChange = (id: number | null) => {
    setWorkflowId(id);
    setIoMeta(null);
    setResult(null);
    form.resetFields();
    if (!id) return;
    setLoadingIo(true);
    void (async () => {
      try {
        const data = await fetchIoMeta(id, callMode, headers);
        if (data.code !== 0) {
          message.error(data.message);
          return;
        }
        setIoMeta(data.data);
        const ex = data.data.exampleInput;
        if (ex && typeof ex === 'object' && !Array.isArray(ex)) {
          form.setFieldsValue(ex as Record<string, unknown>);
        } else if (data.data.inputType === 'text' && typeof ex === 'string') {
          form.setFieldsValue({ content: ex });
        } else if (data.data.inputType === 'message' && ex && typeof ex === 'object') {
          form.setFieldsValue(ex as Record<string, unknown>);
        } else if (ex != null) {
          form.setFieldsValue({ payloadJson: JSON.stringify(ex, null, 2) });
        }
      } catch (e) {
        reportApiError(e, message);
      } finally {
        setLoadingIo(false);
      }
    })();
  };

  const invoke = () =>
    void runApi(async () => {
      if (!workflowId || !ioMeta) {
        message.warning('请先选择工作流');
        return;
      }
      const values = await form.validateFields();
      let body: unknown;
      try {
        body = buildInvokeBody(ioMeta.inputType, values, parsedSchema);
      } catch {
        message.error('入参 JSON 格式不正确');
        return;
      }

      const headerRecord = {
        ...rowsToHeaderRecord(headers),
        [TRACE_HEADER]: createTraceId(),
      };
      if (!headerRecord['Content-Type'] && !headerRecord['content-type']) {
        headerRecord['Content-Type'] = 'application/json';
      }

      const url =
        callMode === 'open'
          ? `/open/v1/workflows/${workflowId}/invoke`
          : `/api/invoke/workflows/${workflowId}`;
      setLastRequestUrl(url);

      setInvoking(true);
      try {
        let envelope: ApiEnvelope<InvokeResult>;
        if (callMode === 'open') {
          const { data } = await axios.post<ApiEnvelope<InvokeResult>>(url, body, {
            headers: headerRecord,
            timeout: API_TIMEOUT_MS,
          });
          envelope = data;
        } else {
          const { data } = await api.post<ApiEnvelope<InvokeResult>>(url, body, {
            headers: headerRecord,
          });
          envelope = data;
        }
        if (envelope.code !== 0) {
          message.error(envelope.message);
          return;
        }
        setResult(envelope.data);
        message.success('调用成功');
      } finally {
        setInvoking(false);
      }
    }, message);

  const openApiDoc =
    workflowId != null
      ? `POST /open/v1/workflows/${workflowId}/invoke\nHeader: X-API-Key: <your-api-key>\nContent-Type: application/json`
      : '选择工作流后显示 Open API 地址';

  return (
    <div>
      <Typography.Title level={4}>API 调用中心</Typography.Title>
      <Typography.Paragraph type="secondary">
        选择工作流，按入参 Schema 填写并调用；可自定义请求头（如 X-API-Key、Authorization）。
      </Typography.Paragraph>

      <Row gutter={16}>
        <Col xs={24} lg={10}>
          <Card title="调用配置" loading={loadingIo}>
            <Space direction="vertical" style={{ width: '100%' }} size="middle">
              <div>
                <Typography.Text type="secondary">调用方式</Typography.Text>
                <Radio.Group
                  style={{ display: 'block', marginTop: 8 }}
                  value={callMode}
                  onChange={(e) => onCallModeChange(e.target.value as CallMode)}
                  options={[
                    { label: '平台内（JWT，自动带登录 Token）', value: 'platform' },
                    { label: 'Open API（/open/v1，需 X-API-Key）', value: 'open' },
                  ]}
                />
              </div>
              <div>
                <Typography.Text type="secondary">工作流</Typography.Text>
                <Select
                  showSearch
                  allowClear
                  placeholder="选择工作流"
                  style={{ width: '100%', marginTop: 8 }}
                  value={workflowId ?? undefined}
                  options={workflowOptions.map((o) => ({
                    value: o.value,
                    label: o.label,
                  }))}
                  onChange={(v) => onWorkflowChange(v ?? null)}
                  filterOption={(input, opt) =>
                    String(opt?.label ?? '')
                      .toLowerCase()
                      .includes(input.toLowerCase())
                  }
                />
              </div>
              <Card size="small" title="请求头 Headers">
                <InvokeHeadersEditor value={headers} onChange={setHeaders} />
                {callMode === 'platform' ? (
                  <Typography.Paragraph type="secondary" style={{ marginTop: 8, marginBottom: 0, fontSize: 12 }}>
                    未填写 Authorization 时将自动使用当前登录 Token
                  </Typography.Paragraph>
                ) : null}
              </Card>
              {ioMeta ? (
                <Typography.Text type="secondary">
                  入参：{IO_SCHEMA_TYPE_LABEL[ioMeta.inputType ?? 'object'] ?? ioMeta.inputType} → 出参：
                  {IO_SCHEMA_TYPE_LABEL[ioMeta.outputType ?? 'object'] ?? ioMeta.outputType}
                </Typography.Text>
              ) : null}
              {ioMeta ? (
                <WorkflowInvokeInput
                  inputType={ioMeta.inputType}
                  inputSchema={ioMeta.inputSchema}
                  form={form}
                />
              ) : null}
              <Access code="invoke:workflow:run">
                <Button type="primary" onClick={invoke} loading={invoking} disabled={!workflowId}>
                  执行调用
                </Button>
              </Access>
              {lastRequestUrl ? (
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  最近请求：POST {lastRequestUrl}
                </Typography.Text>
              ) : null}
            </Space>
          </Card>
          <Card title="Open API 说明" size="small" style={{ marginTop: 16 }}>
            <pre style={{ margin: 0, fontSize: 12, whiteSpace: 'pre-wrap' }}>{openApiDoc}</pre>
          </Card>
        </Col>
        <Col xs={24} lg={14}>
          <Card title="调用结果">
            {result ? (
              <Space direction="vertical" style={{ width: '100%' }} size="middle">
                <Typography.Text type="secondary">
                  {result.workflowName} · {result.elapsedMs ?? 0}ms · Token {result.totalTokens ?? 0}
                </Typography.Text>
                <div>
                  <Typography.Text strong>出参（{result.outputType}）</Typography.Text>
                  <pre
                    style={{
                      marginTop: 8,
                      padding: 12,
                      background: '#fafafa',
                      borderRadius: 8,
                      maxHeight: 360,
                      overflow: 'auto',
                      fontSize: 12,
                    }}
                  >
                    {JSON.stringify(result.output, null, 2)}
                  </pre>
                </div>
                <div>
                  <Typography.Text strong>原始回复</Typography.Text>
                  <pre
                    style={{
                      marginTop: 8,
                      padding: 12,
                      background: '#fafafa',
                      borderRadius: 8,
                      maxHeight: 200,
                      overflow: 'auto',
                      fontSize: 12,
                      whiteSpace: 'pre-wrap',
                    }}
                  >
                    {result.rawReply}
                  </pre>
                </div>
              </Space>
            ) : (
              <Typography.Text type="secondary">执行调用后在此查看结构化出参</Typography.Text>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
}
