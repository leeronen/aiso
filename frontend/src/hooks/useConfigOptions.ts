import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client';
import type { ApiEnvelope } from '../api/types';

type ConfigOption = { value: string; label: string; description?: string };

export function useConfigOptions(configType: string) {
  const [options, setOptions] = useState<{ value: string; label: string }[]>([]);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get<ApiEnvelope<ConfigOption[]>>('/api/system/config-options', {
        params: { configType },
      });
      if (data.code === 0) {
        setOptions(data.data.map((o) => ({ value: o.value, label: o.label })));
      }
    } finally {
      setLoading(false);
    }
  }, [configType]);

  useEffect(() => {
    void load();
  }, [load]);

  return { options, loading, reload: load };
}
