package com.aios.platform.kb;

import com.aios.platform.config.JvmNetworkBootstrap;
import java.sql.Connection;
import java.sql.DriverManager;
import org.junit.jupiter.api.Test;

class PgJdbcConnectTest {

    @Test
    void connectLocalPgvector() throws Exception {
        JvmNetworkBootstrap.bypassProxyForLocalServices();
        String url =
                "jdbc:postgresql://127.0.0.1:5432/aios_vector?connectTimeout=10&socketTimeout=120&tcpKeepAlive=true";
        try (Connection c = DriverManager.getConnection(url, "aios", "aios123")) {
            System.out.println("connected: " + c.isValid(2));
        }
    }
}
