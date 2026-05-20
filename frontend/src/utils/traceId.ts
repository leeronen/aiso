/** 与后端 TraceIds.HEADER 对齐 */
export const TRACE_HEADER = 'X-Trace-Id';

export function createTraceId(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID().replace(/-/g, '');
  }
  return `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 10)}`;
}
