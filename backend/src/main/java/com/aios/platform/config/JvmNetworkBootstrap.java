package com.aios.platform.config;

/**
 * IDE/系统常配置 socksProxyHost=127.0.0.1，会导致 JDBC 连本地 PostgreSQL/MySQL 走 SOCKS 而失败
 *（异常信息可能仅为 "127.0.0.1"）。启动时把本机地址加入 nonProxyHosts。
 */
public final class JvmNetworkBootstrap {

    private static final String LOCAL_BYPASS = "localhost|127.0.0.1|127.*|[::1]|*.localhost|0.0.0.0";

    private JvmNetworkBootstrap() {}

    public static void bypassProxyForLocalServices() {
        mergeNonProxyHosts("socksNonProxyHosts", LOCAL_BYPASS);
        mergeNonProxyHosts("http.nonProxyHosts", LOCAL_BYPASS);
        mergeNonProxyHosts("ftp.nonProxyHosts", LOCAL_BYPASS);
        // 部分 IDE 将 socksProxyHost 设为 127.0.0.1，会导致 JDBC 连本机 PG/MySQL 报 UnknownHostException: 127.0.0.1
        String socksHost = System.getProperty("socksProxyHost");
        if (socksHost != null && ("127.0.0.1".equals(socksHost) || "localhost".equalsIgnoreCase(socksHost))) {
            System.clearProperty("socksProxyHost");
            System.clearProperty("socksProxyPort");
        }
    }

    private static void mergeNonProxyHosts(String key, String extra) {
        String existing = System.getProperty(key, "");
        if (existing.contains("127.0.0.1") && existing.contains("localhost")) {
            return;
        }
        String merged = existing.isBlank() ? extra : existing + "|" + extra;
        System.setProperty(key, merged);
    }
}
