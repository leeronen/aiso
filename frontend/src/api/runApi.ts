import type { MessageInstance } from 'antd/es/message/interface';
import { reportApiError } from '../utils/http';

/** Run an API call; errors are toasted and not rethrown (no uncaught promise) */
export async function runApi<T>(
  fn: () => Promise<T>,
  message?: MessageInstance,
): Promise<T | undefined> {
  try {
    return await fn();
  } catch (e) {
    reportApiError(e, message);
    return undefined;
  }
}
