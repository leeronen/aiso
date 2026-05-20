import { api } from './client';
import type { ApiEnvelope } from './types';
import { useAuthStore } from '../store/auth';

export type UserProfile = {
  userId: number;
  username: string;
  nickname?: string;
  email?: string;
  admin?: boolean;
  permissions?: string[];
};

export async function fetchAndStoreProfile() {
  const { data } = await api.get<ApiEnvelope<UserProfile>>('/api/auth/me');
  if (data.code === 0) {
    useAuthStore.getState().setProfile(data.data.permissions ?? [], !!data.data.admin);
  }
  return data;
}
