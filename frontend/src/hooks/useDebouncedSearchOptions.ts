import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

export type SelectOption = { value: number; label: string };

function debounce<T extends (...args: never[]) => void>(fn: T, ms: number) {
  let timer: ReturnType<typeof setTimeout>;
  return (...args: Parameters<T>) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), ms);
  };
}

export function useDebouncedSearchOptions(
  fetcher: (keyword: string) => Promise<SelectOption[]>,
  debounceMs = 300,
) {
  const [options, setOptions] = useState<SelectOption[]>([]);
  const [loading, setLoading] = useState(false);
  const fetcherRef = useRef(fetcher);
  fetcherRef.current = fetcher;

  const runSearch = useCallback(async (keyword: string) => {
    setLoading(true);
    try {
      const items = await fetcherRef.current(keyword);
      setOptions(items);
    } finally {
      setLoading(false);
    }
  }, []);

  const debouncedSearch = useMemo(() => debounce((kw: string) => void runSearch(kw), debounceMs), [runSearch, debounceMs]);

  useEffect(() => {
    void runSearch('');
  }, [runSearch]);

  const mergeSelected = useCallback((selected: SelectOption[]) => {
    if (!selected.length) return;
    setOptions((prev) => {
      const map = new Map<number, SelectOption>();
      [...selected, ...prev].forEach((o) => map.set(o.value, o));
      return Array.from(map.values());
    });
  }, []);

  const reload = useCallback(() => {
    void runSearch('');
  }, [runSearch]);

  return { options, loading, onSearch: debouncedSearch, mergeSelected, reload };
}
