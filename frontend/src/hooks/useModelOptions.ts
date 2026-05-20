import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client';
import type { ApiEnvelope } from '../api/types';

type ModelRow = { modelId: number; modelName: string; modelCode?: string; status?: number };

type Page<T> = { records: T[] };

export function useModelOptions() {
  const [options, setOptions] = useState<{ value: number; label: string }[]>([]);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiEnvelope<Page<ModelRow>>>('/api/ai/models', {
        params: { current: 1, size: 500 },
      });
      if (data.code === 0) {
        setOptions(
          data.data.records
            .filter((m) => m.status === 1)
            .map((m) => ({
              value: m.modelId,
              label: `${m.modelName}${m.modelCode ? ` (${m.modelCode})` : ''}`,
            })),
        );
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  return { options, loading, reload: load };
}
