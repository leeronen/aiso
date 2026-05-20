import { Form, Input, InputNumber, Select, Switch } from 'antd';
import type { FormInstance } from 'antd';
import { useMemo } from 'react';

type SchemaProperty = {
  type?: string;
  description?: string;
  enum?: string[];
  default?: unknown;
};

type ParsedSchema = {
  properties: Record<string, SchemaProperty>;
  required: string[];
};

function parseSchema(schemaJson?: string): ParsedSchema | null {
  if (!schemaJson?.trim()) return null;
  try {
    const s = JSON.parse(schemaJson) as {
      properties?: Record<string, SchemaProperty>;
      required?: string[];
    };
    if (!s.properties || typeof s.properties !== 'object') return null;
    return { properties: s.properties, required: s.required ?? [] };
  } catch {
    return null;
  }
}

type Props = {
  inputType?: string;
  inputSchema?: string;
  form: FormInstance;
};

export function WorkflowInvokeInput({ inputType, inputSchema, form }: Props) {
  const parsed = useMemo(() => parseSchema(inputSchema), [inputSchema]);
  const type = inputType ?? 'object';

  if (type === 'text') {
    return (
      <Form form={form} layout="vertical">
        <Form.Item name="content" label="文本入参" rules={[{ required: true, message: '请输入内容' }]}>
          <Input.TextArea rows={6} placeholder="输入要传给工作流的文本" />
        </Form.Item>
      </Form>
    );
  }

  if (type === 'message') {
    return (
      <Form form={form} layout="vertical" initialValues={{ role: 'user' }}>
        <Form.Item name="role" label="角色" rules={[{ required: true }]}>
          <Select
            options={[
              { value: 'user', label: 'user' },
              { value: 'system', label: 'system' },
            ]}
          />
        </Form.Item>
        <Form.Item name="content" label="消息内容" rules={[{ required: true, message: '请输入消息' }]}>
          <Input.TextArea rows={6} />
        </Form.Item>
      </Form>
    );
  }

  if (parsed && Object.keys(parsed.properties).length > 0) {
    return (
      <Form form={form} layout="vertical">
        {Object.entries(parsed.properties).map(([key, prop]) => {
          const required = parsed.required.includes(key);
          const label = prop.description ? `${key}（${prop.description}）` : key;
          if (prop.enum?.length) {
            return (
              <Form.Item key={key} name={key} label={label} rules={required ? [{ required: true }] : []}>
                <Select options={prop.enum.map((v) => ({ value: v, label: v }))} allowClear />
              </Form.Item>
            );
          }
          if (prop.type === 'boolean') {
            return (
              <Form.Item key={key} name={key} label={label} valuePropName="checked">
                <Switch />
              </Form.Item>
            );
          }
          if (prop.type === 'integer' || prop.type === 'number') {
            return (
              <Form.Item key={key} name={key} label={label} rules={required ? [{ required: true }] : []}>
                <InputNumber style={{ width: '100%' }} />
              </Form.Item>
            );
          }
          return (
            <Form.Item key={key} name={key} label={label} rules={required ? [{ required: true }] : []}>
              <Input.TextArea rows={prop.type === 'object' || prop.type === 'array' ? 4 : 2} />
            </Form.Item>
          );
        })}
      </Form>
    );
  }

  return (
    <Form form={form} layout="vertical">
      <Form.Item
        name="payloadJson"
        label="JSON 入参"
        rules={[{ required: true, message: '请输入 JSON' }]}
        extra="按工作流入参 Schema 填写完整 JSON"
      >
        <Input.TextArea
          rows={12}
          style={{ fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace', fontSize: 12 }}
        />
      </Form.Item>
    </Form>
  );
}

export function buildInvokeBody(
  inputType: string | undefined,
  values: Record<string, unknown>,
  parsed: ParsedSchema | null,
): unknown {
  const type = inputType ?? 'object';
  if (type === 'text') {
    return values.content ?? '';
  }
  if (type === 'message') {
    return { role: values.role ?? 'user', content: values.content ?? '' };
  }
  if (parsed && Object.keys(parsed.properties).length > 0) {
    const body: Record<string, unknown> = {};
    for (const key of Object.keys(parsed.properties)) {
      if (values[key] !== undefined) body[key] = values[key];
    }
    return body;
  }
  const raw = values.payloadJson as string | undefined;
  if (!raw?.trim()) return {};
  return JSON.parse(raw) as unknown;
}

export function parseInvokeSchema(schemaJson?: string) {
  return parseSchema(schemaJson);
}
