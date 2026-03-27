package com.sei.seipicbackend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * @author hikari39_
 * @since 2026-03-27
 */
@Data
public class BaseResponse<T> implements Serializable {
    /**
     * 响应码
     */
    private int code;

    /**
     * 相应数据
     */
    private T data;

    /**
     * 响应消息
     */
    private String message;

    private static final long serialVersionUID = 1L;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, String message) {
        this(code, null, message);
    }
}
