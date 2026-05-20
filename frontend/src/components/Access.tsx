import type { ReactNode } from 'react';
import { usePermission } from '../hooks/usePermission';

type Props = {
  code?: string;
  anyOf?: string[];
  children: ReactNode;
  fallback?: ReactNode;
};

/** 无权限时不渲染子节点（admin 拥有全部权限） */
export function Access({ code, anyOf, children, fallback = null }: Props) {
  const { can, canAny } = usePermission();
  let allowed = false;
  if (code) {
    allowed = can(code);
  } else if (anyOf && anyOf.length > 0) {
    allowed = canAny(...anyOf);
  } else {
    allowed = true;
  }
  return <>{allowed ? children : fallback}</>;
}
