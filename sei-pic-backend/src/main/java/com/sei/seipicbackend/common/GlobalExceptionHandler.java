package com.sei.seipicbackend.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.sei.seipicbackend.exception.BusinessException;
import com.sei.seipicbackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常拦截器
 * @author hikari39_
 * @since 2026-03-27
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public BaseResponse<?> notLoginException(NotLoginException e) {
        log.error("NotLoginException", e);
        return ResponseUtils.error(ErrorCode.NOT_LOGIN_ERROR, e.getMessage());
    }

    @ExceptionHandler(NotPermissionException.class)
    public BaseResponse<?> notPermissionExceptionHandler(NotPermissionException e) {
        log.error("NotPermissionException", e);
        return ResponseUtils.error(ErrorCode.NO_AUTH_ERROR, e.getMessage());
    }


    /**
     * 捕获自定义异常
     * @param e businessException
     * @return
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return ResponseUtils.error(e.getCode(), e.getMessage());
    }

    /**
     * 捕获系统异常
     * @param e
     * @return
     */
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> exceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResponseUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}
