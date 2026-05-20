import { api } from './client';
import type { ApiEnvelope } from './types';
import type { SelectOption } from '../hooks/useDebouncedSearchOptions';

type ApiSelectOption = { value: number; label: string };

async function fetchOptions(url: string, keyword: string, limit = 20): Promise<SelectOption[]> {
  const { data } = await api.get<ApiEnvelope<ApiSelectOption[]>>(url, {
    params: { keyword: keyword || undefined, limit },
  });
  if (data.code !== 0) return [];
  return data.data;
}

export const searchModelOptions = (keyword: string) => fetchOptions('/api/ai/models/options', keyword);
export const searchKbOptions = (keyword: string) => fetchOptions('/api/kb/bases/options', keyword);
export const searchMcpOptions = (keyword: string) => fetchOptions('/api/ai/mcp-servers/options', keyword);
export const searchSkillOptions = (keyword: string) => fetchOptions('/api/ai/skills/options', keyword);
export const searchAgentOptions = (keyword: string) => fetchOptions('/api/ai/agents/options', keyword);
export const searchWorkflowOptions = (keyword: string) => fetchOptions('/api/ai/workflows/options', keyword);

type ApiStringOption = { value: string; label: string; description?: string };

export async function searchEmbeddingTypeOptions(keyword: string): Promise<ApiStringOption[]> {
  const { data } = await api.get<ApiEnvelope<ApiStringOption[]>>('/api/kb/embedding-types/options', {
    params: { keyword: keyword || undefined, limit: 30 },
  });
  if (data.code !== 0) return [];
  return data.data;
}
