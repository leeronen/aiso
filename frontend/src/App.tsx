import type { ReactNode } from 'react';
import { useEffect } from 'react';
import { App as AntApp, Spin, Typography } from 'antd';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from './layouts/AppLayout';
import { LoginPage } from './pages/Login';
import { DashboardPage } from './pages/Dashboard';
import { ModelsPage } from './pages/ai/ModelsPage';
import { AgentsPage } from './pages/ai/AgentsPage';
import { McpServersPage } from './pages/ai/McpServersPage';
import { SkillsPage } from './pages/ai/SkillsPage';
import { WorkflowsPage } from './pages/ai/WorkflowsPage';
import { KbBasesPage } from './pages/kb/KbBasesPage';
import { KbDocumentsPage } from './pages/kb/KbDocumentsPage';
import { ChatSessionsPage } from './pages/chat/ChatSessionsPage';
import { ApiInvokePage } from './pages/invoke/ApiInvokePage';
import { UsersPage } from './pages/system/UsersPage';
import { RolesPage } from './pages/system/RolesPage';
import { MenusPage } from './pages/system/MenusPage';
import { PermissionsPage } from './pages/system/PermissionsPage';
import { useAuthStore } from './store/auth';
import { getApiErrorMessage, isRequestAborted, isRequestTimeout } from './utils/http';

function useAuthHydration() {
  useEffect(() => {
    if (useAuthStore.persist.hasHydrated()) {
      useAuthStore.getState().markHydrated();
    }
    return useAuthStore.persist.onFinishHydration(() => {
      useAuthStore.getState().markHydrated();
    });
  }, []);
}

function AuthHydrateFallback() {
  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <Spin size="large" />
    </div>
  );
}

/** 登录 / 注册页：不校验 Token，不要求权限 */
function AuthPublic({ children }: { children: ReactNode }) {
  const hydrated = useAuthStore((s) => s.hydrated);
  useAuthHydration();
  if (!hydrated) {
    return <AuthHydrateFallback />;
  }
  return <>{children}</>;
}

/** Catch API timeouts / network errors that were not handled in page code */
function ApiErrorGuard() {
  const { message } = AntApp.useApp();

  useEffect(() => {
    const onUnhandled = (event: PromiseRejectionEvent) => {
      const reason = event.reason;
      if (isRequestAborted(reason)) {
        event.preventDefault();
        return;
      }
      if (
        isRequestTimeout(reason) ||
        (reason && typeof reason === 'object' && (reason as { code?: string }).code === 'ERR_NETWORK')
      ) {
        event.preventDefault();
        const text = getApiErrorMessage(reason);
        if (text) message.error(text);
      }
    };
    window.addEventListener('unhandledrejection', onUnhandled);
    return () => window.removeEventListener('unhandledrejection', onUnhandled);
  }, [message]);

  return null;
}

function Private({ children }: { children: ReactNode }) {
  const hydrated = useAuthStore((s) => s.hydrated);
  const token = useAuthStore((s) => s.accessToken);
  useAuthHydration();

  if (!hydrated) {
    return <AuthHydrateFallback />;
  }
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}

export default function App() {
  return (
    <AntApp>
      <ApiErrorGuard />
      <BrowserRouter>
        <Routes>
          <Route
            path="/login"
            element={
              <AuthPublic>
                <LoginPage />
              </AuthPublic>
            }
          />
          <Route
            path="/"
            element={
              <Private>
                <AppLayout />
              </Private>
            }
          >
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="dashboard" element={<DashboardPage />} />
            <Route path="ai/models" element={<ModelsPage />} />
            <Route path="ai/agents" element={<AgentsPage />} />
            <Route path="ai/mcp-servers" element={<McpServersPage />} />
            <Route path="ai/skills" element={<SkillsPage />} />
            <Route path="ai/workflows" element={<WorkflowsPage />} />
            <Route path="kb/bases" element={<KbBasesPage />} />
            <Route path="kb/documents" element={<KbDocumentsPage />} />
            <Route path="chat/sessions" element={<ChatSessionsPage />} />
            <Route path="invoke/center" element={<ApiInvokePage />} />
            <Route path="system/users" element={<UsersPage />} />
            <Route path="system/roles" element={<RolesPage />} />
            <Route path="system/menus" element={<MenusPage />} />
            <Route path="system/permissions" element={<PermissionsPage />} />
          </Route>
          <Route path="*" element={<Typography.Title level={4}>404</Typography.Title>} />
        </Routes>
      </BrowserRouter>
    </AntApp>
  );
}
