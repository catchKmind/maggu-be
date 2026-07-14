package com.maggu.maggu.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.maggu.maggu.global.exception.ErrorCode;
import lombok.Getter;

import java.time.Instant;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private static final String SUCCESS_CODE = "SUCCESS";

    private final boolean success;
    private final int status;
    private final String code;
    private final String message;
    private final T data;
    private final Instant timestamp;

    private ApiResponse(boolean success, int status, String code, String message, T data) {
        this.success = success;
        this.status = status;
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now();
    }

    public static <T> ApiResponse<T> success(int status, T data) {
        return new ApiResponse<>(true, status, SUCCESS_CODE, "OK", data);
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(200, data);
    }

    public static ApiResponse<Void> success() {
        return success(200, null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getStatus().value(), errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, T data) {
        return new ApiResponse<>(false, errorCode.getStatus().value(), errorCode.getCode(), errorCode.getMessage(), data);
    }
}
