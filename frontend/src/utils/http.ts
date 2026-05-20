import axios from 'axios';
import type { MessageInstance } from 'antd/es/message/interface';
import { TRACE_HEADER } from './traceId';

/** Axios / fetch aborted by navigation, StrictMode remount, or extension hooks */
export function isRequestAborted(err: unknown): boolean {
  if (axios.isCancel(err)) return true;
  if (!err || typeof err !== 'object') return false;
  const e = err as { code?: string; message?: string; name?: string };
  if (e.code === 'ERR_CANCELED') return true;
  if (e.name === 'CanceledError' || e.name === 'AbortError') return true;
  const msg = typeof e.message === 'string' ? e.message : '';
  return /abort|canceled|cancelled/i.test(msg);
}

export function isRequestTimeout(err: unknown): boolean {
  if (!err || typeof err !== 'object') return false;
  const e = err as { code?: string; message?: string };
  return e.code === 'ECONNABORTED' || (typeof e.message === 'string' && e.message.includes('timeout'));
}

function traceIdFromError(err: unknown): string | undefined {
  if (!err || typeof err !== 'object') return undefined;
  const e = err as {
    response?: { headers?: Record<string, string>; data?: { traceId?: string } };
  };
  const fromBody = e.response?.data?.traceId;
  if (fromBody) return fromBody;
  const headers = e.response?.headers;
  if (!headers) return undefined;
  return headers[TRACE_HEADER] ?? headers[TRACE_HEADER.toLowerCase()];
}

/** User-facing message for axios / network errors */
export function getApiErrorMessage(err: unknown): string {
  if (isRequestAborted(err)) return '';
  if (isRequestTimeout(err)) {
    return '请求超时：请确认后端已启动（http://127.0.0.1:8080）且 MySQL 可连接';
  }
  if (err && typeof err === 'object' && 'response' in err) {
    const r = (err as { response?: { status?: number; data?: { message?: string; traceId?: string } } })
      .response;
    const tid = traceIdFromError(err);
    const traceSuffix = tid ? `（traceId: ${tid}）` : '';
    if (r?.status === 401) return `未登录或登录已过期，请重新登录${traceSuffix}`;
    if (r?.data?.message) return `${r.data.message}${traceSuffix}`;
    if (r?.status) return `请求失败（HTTP ${r.status}）${traceSuffix}`;
  }
  if (err && typeof err === 'object' && 'code' in err) {
    const code = (err as { code?: string }).code;
    if (code === 'ERR_NETWORK') {
      return '无法连接后端，请在 backend 目录运行 mvn spring-boot:run';
    }
  }
  return '网络请求失败';
}

/** Log / toast API errors; returns true if handled (caller should return) */
export function reportApiError(err: unknown, message?: MessageInstance): boolean {
  if (isRequestAborted(err)) return true;
  const text = getApiErrorMessage(err);
  if (!text) return true;
  if (message) {
    message.error(text);
  } else {
    console.error(text, err);
  }
  return true;
}
