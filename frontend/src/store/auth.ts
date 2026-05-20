import { create } from 'zustand';
import { persist } from 'zustand/middleware';

type AuthState = {
  accessToken: string | null;
  refreshToken: string | null;
  permissions: string[];
  admin: boolean;
  hydrated: boolean;
  setTokens: (access: string, refresh: string) => void;
  setProfile: (permissions: string[], admin: boolean) => void;
  clear: () => void;
  markHydrated: () => void;
};

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      permissions: [],
      admin: false,
      hydrated: false,
      setTokens: (access, refresh) => set({ accessToken: access, refreshToken: refresh }),
      setProfile: (permissions, admin) => set({ permissions, admin }),
      clear: () =>
        set({
          accessToken: null,
          refreshToken: null,
          permissions: [],
          admin: false,
        }),
      markHydrated: () => set({ hydrated: true }),
    }),
    {
      name: 'aios-auth',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        permissions: state.permissions,
        admin: state.admin,
      }),
      onRehydrateStorage: () => (state) => {
        state?.markHydrated();
      },
    },
  ),
);
