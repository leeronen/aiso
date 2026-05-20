import {
  IO_SCHEMA_PRESETS,
  IO_SCHEMA_TYPES,
  formatJsonText,
  templateForType,
  validateJsonText,
} from '../constants/ioSchemaTemplates';
import { App, Button, Card, Form, Input, Select, Space, Typography } from 'antd';
import type { FormInstance } from 'antd';

type Props = {
  title: string;
  typeField: 'inputType' | 'outputType';
  schemaField: 'inputSchema' | 'outputSchema';
  form: FormInstance;
};

export function IoSchemaFields({ title, typeField, schemaField, form }: Props) {
  const { message } = App.useApp();

  const applyTypeTemplate = () => {
    const t = form.getFieldValue(typeField) as string | undefined;
    form.setFieldValue(schemaField, templateForType(t));
  };

  const applyPreset = (template: string) => {
    form.setFieldValue(schemaField, template);
  };

  const formatSchema = () => {
    try {
      const raw = form.getFieldValue(schemaField) as string | undefined;
      form.setFieldValue(schemaField, formatJsonText(raw));
    } catch {
      message.error('当前内容不是合法 JSON，无法格式化');
    }
  };

  return (
    <Card size="small" title={title} style={{ marginBottom: 12 }}>
      <Form.Item name={typeField} label="类型" rules={[{ required: true, message: '请选择类型' }]}>
        <Select options={[...IO_SCHEMA_TYPES]} placeholder="选择输入/输出类型" />
      </Form.Item>
      <Form.Item label="快速模板">
        <Select
          allowClear
          placeholder="选择预设模板填入下方 JSON"
          options={IO_SCHEMA_PRESETS.map((p) => ({ value: p.key, label: p.label }))}
          onChange={(key) => {
            const preset = IO_SCHEMA_PRESETS.find((p) => p.key === key);
            if (preset) applyPreset(preset.template);
          }}
        />
      </Form.Item>
      <Space style={{ marginBottom: 8 }}>
        <Button size="small" onClick={applyTypeTemplate}>
          按类型生成模板
        </Button>
        <Button size="small" onClick={formatSchema}>
          格式化 JSON
        </Button>
      </Space>
      <Form.Item
        name={schemaField}
        label="JSON 模板 / Schema"
        rules={[{ validator: (_, v) => validateJsonText(v as string) }]}
        extra={
          <Typography.Text type="secondary">
            可粘贴 JSON Schema 或示例 JSON；保存前会自动校验格式
          </Typography.Text>
        }
      >
        <Input.TextArea
          rows={8}
          placeholder='{"type":"object","properties":{...}}'
          style={{ fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace', fontSize: 12 }}
        />
      </Form.Item>
    </Card>
  );
}
