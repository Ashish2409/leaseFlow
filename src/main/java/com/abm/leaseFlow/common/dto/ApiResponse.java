package com.abm.leaseFlow.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;

/**
 * Unified API response envelope for all endpoints.
 * Success: { success: true, data: T }
 * Error:   { success: false, error: ApiErrorDetail }
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ApiErrorDetail error;
    private final Instant timestamp;

    private ApiResponse(boolean success, T data, ApiErrorDetail error) {
        this.success   = success;
        this.data      = data;
        this.error     = error;
        this.timestamp = Instant.now();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ApiErrorDetail(code, message));
    }

    @Getter
    public static class ApiErrorDetail {
        private final String code;
        private final String message;

        public ApiErrorDetail(String code, String message) {
            this.code    = code;
            this.message = message;
        }
    }
}
