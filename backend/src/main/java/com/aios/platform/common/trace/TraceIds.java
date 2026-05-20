package com.aios.platform.common.trace;

import java.util.UUID;
import org.slf4j.MDC;

/** 全链路 TraceId：请求头 {@value #HEADER}，日志 MDC 键 {@value #MDC_KEY}。 */
public final class TraceIds {

    public static final String HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";

    private TraceIds() {}

    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String resolve(String incoming) {
        if (incoming == null) {
            return generate();
        }
        String trimmed = incoming.trim();
        if (trimmed.isEmpty() || trimmed.length() > 64) {
            return generate();
        }
        return trimmed;
    }

    public static String current() {
        return MDC.get(MDC_KEY);
    }

    public static void put(String traceId) {
        if (traceId != null && !traceId.isBlank()) {
            MDC.put(MDC_KEY, traceId);
        }
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}
