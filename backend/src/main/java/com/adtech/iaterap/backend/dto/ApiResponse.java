package com.adtech.iaterap.backend.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.LinkedHashMap;

/** Stable success envelope used by the public Procedure, Rule and Coverage APIs. */
public record ApiResponse<T>(T data, Map<String, Object> meta, LocalDateTime timestamp) {
    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, Map.of(), LocalDateTime.now());
    }

    public static <T> ApiResponse<T> page(T data, int count, String nextCursor) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("count", count);
        meta.put("nextCursor", nextCursor);
        return new ApiResponse<>(data, meta, LocalDateTime.now());
    }
}
