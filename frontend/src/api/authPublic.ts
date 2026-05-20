/** 登录 / 注册 / 刷新 Token：不携带 JWT，401 也不触发全局登出 */
const AUTH_PUBLIC_PATHS = ['/api/auth/login', '/api/auth/register', '/api/auth/refresh'] as const;

export function isAuthPublicUrl(url?: string): boolean {
  if (!url) return false;
  return AUTH_PUBLIC_PATHS.some((p) => url.includes(p));
}
