import {
  ApiOutlined,
  BookOutlined,
  CommentOutlined,
  DashboardOutlined,
  RobotOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import type { ReactNode } from 'react';

const iconMap: Record<string, ReactNode> = {
  HomeOutlined: <DashboardOutlined />,
  DashboardOutlined: <DashboardOutlined />,
  RobotOutlined: <RobotOutlined />,
  BookOutlined: <BookOutlined />,
  CommentOutlined: <CommentOutlined />,
  ApiOutlined: <ApiOutlined />,
  SettingOutlined: <SettingOutlined />,
};

export function menuIcon(name?: string) {
  if (!name) return undefined;
  return iconMap[name] ?? undefined;
}
