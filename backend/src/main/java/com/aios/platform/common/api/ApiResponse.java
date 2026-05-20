package com.aios.platform.common.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;
    /** 全链路追踪 ID，与请求/响应头 X-Trace-Id 一致 */
    private String traceId;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data, currentTraceId());
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(0, "ok", null, currentTraceId());
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null, currentTraceId());
    }

    private static String currentTraceId() {
        return com.aios.platform.common.trace.TraceIds.current();
    }
}
