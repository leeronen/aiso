import { useAuthStore } from '../store/auth';

export function usePermission() {
  const admin = useAuthStore((s) => s.admin);
  const permissions = useAuthStore((s) => s.permissions);

  const can = (code: string) => {
    if (admin) return true;
    return permissions.includes(code);
  };

  const canAny = (...codes: string[]) => codes.some((c) => can(c));

  return { admin, permissions, can, canAny };
}
