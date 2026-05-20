import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons';
import { Button, Input, Space, Typography } from 'antd';

export type HeaderRow = { key: string; value: string };

type Props = {
  value?: HeaderRow[];
  onChange?: (rows: HeaderRow[]) => void;
};

export function rowsToHeaderRecord(rows: HeaderRow[]): Record<string, string> {
  const out: Record<string, string> = {};
  for (const row of rows) {
    const k = row.key?.trim();
    if (!k) continue;
    out[k] = row.value ?? '';
  }
  return out;
}

export function InvokeHeadersEditor({ value, onChange }: Props) {
  const rows = value?.length ? value : [{ key: '', value: '' }];

  const update = (next: HeaderRow[]) => {
    onChange?.(next);
  };

  return (
    <div>
      <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 8 }}>
        自定义 HTTP Header，空名称行将被忽略
      </Typography.Text>
      <Space direction="vertical" style={{ width: '100%' }} size={8}>
        {rows.map((row, index) => (
          <Space key={index} style={{ width: '100%' }} align="start">
            <Input
              placeholder="Header 名，如 X-API-Key"
              value={row.key}
              onChange={(e) => {
                const next = [...rows];
                next[index] = { ...next[index], key: e.target.value };
                update(next);
              }}
              style={{ width: 160 }}
            />
            <Input
              placeholder="Header 值"
              value={row.value}
              onChange={(e) => {
                const next = [...rows];
                next[index] = { ...next[index], value: e.target.value };
                update(next);
              }}
              style={{ flex: 1, minWidth: 120 }}
            />
            <Button
              type="text"
              danger
              icon={<MinusCircleOutlined />}
              disabled={rows.length <= 1}
              onClick={() => update(rows.filter((_, i) => i !== index))}
            />
          </Space>
        ))}
        <Button
          type="dashed"
          icon={<PlusOutlined />}
          onClick={() => update([...rows, { key: '', value: '' }])}
          block
        >
          添加 Header
        </Button>
      </Space>
    </div>
  );
}
