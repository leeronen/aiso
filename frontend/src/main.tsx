import React from 'react';
import ReactDOM from 'react-dom/client';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import App from './App';
import { isRequestAborted } from './utils/http';
import './index.css';

/** Suppress benign abort noise (StrictMode remount, route change, browser extensions) */
window.addEventListener('unhandledrejection', (event) => {
  if (isRequestAborted(event.reason)) {
    event.preventDefault();
  }
});

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider locale={zhCN}>
      <App />
    </ConfigProvider>
  </React.StrictMode>,
);
