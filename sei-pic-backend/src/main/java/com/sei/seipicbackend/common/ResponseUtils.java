package com.sei.seipicbackend.common;

import com.sei.seipicbackend.exception.ErrorCode;

/**
 * @desc 快速构造响应体
 * @author hikari39_
 * @since 2026-03-27
 */
public class ResponseUtils {
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "ok");
    }

    public static <T> BaseResponse<T> success(int code, T data, String message) {
        return new BaseResponse<>(0, data, "ok");
    }

    public static BaseResponse<?> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }

    public static BaseResponse<?> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode.getCode(), null, errorCode.getMessage());
    }

    public static BaseResponse<?> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }
}
