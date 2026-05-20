/** 输入/输出 Schema 类型 */
export const IO_SCHEMA_TYPES = [
  { value: 'text', label: '文本 (string)' },
  { value: 'json', label: 'JSON 任意' },
  { value: 'object', label: '对象 (object)' },
  { value: 'array', label: '数组 (array)' },
  { value: 'tool', label: '工具参数 (tool)' },
  { value: 'message', label: '对话消息 (message)' },
] as const;

export type IoSchemaType = (typeof IO_SCHEMA_TYPES)[number]['value'];

/** 按类型提供的默认 JSON 模板 */
export const IO_SCHEMA_TEMPLATES: Record<IoSchemaType, string> = {
  text: JSON.stringify(
    {
      type: 'string',
      description: '文本输入',
      example: '',
    },
    null,
    2,
  ),
  json: JSON.stringify(
    {
      description: '任意 JSON 结构',
      example: {},
    },
    null,
    2,
  ),
  object: JSON.stringify(
    {
      type: 'object',
      properties: {
        query: { type: 'string', description: '用户问题' },
      },
      required: ['query'],
    },
    null,
    2,
  ),
  array: JSON.stringify(
    {
      type: 'array',
      items: { type: 'string' },
      description: '字符串列表',
    },
    null,
    2,
  ),
  tool: JSON.stringify(
    {
      type: 'object',
      properties: {
        name: { type: 'string', description: '工具名' },
        arguments: { type: 'object', description: '调用参数' },
      },
      required: ['name', 'arguments'],
    },
    null,
    2,
  ),
  message: JSON.stringify(
    {
      type: 'object',
      properties: {
        role: { type: 'string', enum: ['user', 'assistant', 'system'] },
        content: { type: 'string' },
      },
      required: ['role', 'content'],
    },
    null,
    2,
  ),
};

/** 常用业务模板（可一键填入） */
export const IO_SCHEMA_PRESETS: { key: string; label: string; template: string }[] = [
  {
    key: 'mcp_tool_call',
    label: 'MCP 工具调用',
    template: JSON.stringify(
      {
        type: 'object',
        properties: {
          tool: { type: 'string' },
          params: { type: 'object' },
        },
        required: ['tool', 'params'],
      },
      null,
      2,
    ),
  },
  {
    key: 'llm_chat',
    label: 'LLM 对话消息',
    template: IO_SCHEMA_TEMPLATES.message,
  },
  {
    key: 'rag_query',
    label: 'RAG 检索',
    template: JSON.stringify(
      {
        type: 'object',
        properties: {
          query: { type: 'string' },
          topK: { type: 'integer', default: 5 },
        },
        required: ['query'],
      },
      null,
      2,
    ),
  },
  {
    key: 'empty_object',
    label: '空对象',
    template: JSON.stringify({ type: 'object', properties: {} }, null, 2),
  },
];

export const IO_SCHEMA_TYPE_LABEL: Record<string, string> = Object.fromEntries(
  IO_SCHEMA_TYPES.map((t) => [t.value, t.label]),
);

export function templateForType(type?: string): string {
  if (type && type in IO_SCHEMA_TEMPLATES) {
    return IO_SCHEMA_TEMPLATES[type as IoSchemaType];
  }
  return IO_SCHEMA_TEMPLATES.object;
}

export function formatJsonText(raw?: string): string {
  if (!raw?.trim()) return '';
  return JSON.stringify(JSON.parse(raw), null, 2);
}

export function validateJsonText(raw?: string): Promise<void> {
  if (!raw?.trim()) return Promise.resolve();
  try {
    JSON.parse(raw);
    return Promise.resolve();
  } catch {
    return Promise.reject(new Error('请输入合法的 JSON'));
  }
}
