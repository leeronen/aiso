import axios from 'axios';
import { createTraceId, TRACE_HEADER } from '../utils/traceId';
import { isAuthPublicUrl } from './authPublic';
import { useAuthStore } from '../store/auth';

/** Shared with vite proxy — restart `npm run dev` after changing */
export const API_TIMEOUT_MS = 12_000;

export const api = axios.create({
  baseURL: '/',
  timeout: API_TIMEOUT_MS,
});

api.interceptors.request.use((config) => {
  config.headers = config.headers ?? {};
  config.headers[TRACE_HEADER] = createTraceId();
  if (isAuthPublicUrl(config.url)) {
    return config;
  }
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const url = err.config?.url as string | undefined;
    if (err.response?.status === 401 && !isAuthPublicUrl(url)) {
      useAuthStore.getState().clear();
    }
    return Promise.reject(err);
  },
);
