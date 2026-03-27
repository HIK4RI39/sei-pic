package com.sei.seipicbackend.controller;

import cn.hutool.core.util.ObjUtil;
import com.sei.seipicbackend.common.BaseResponse;
import com.sei.seipicbackend.common.ResponseUtils;
import com.sei.seipicbackend.exception.ErrorCode;
import com.sei.seipicbackend.exception.ThrowUtils;
import com.sei.seipicbackend.model.dto.UserRegisterRequest;
import com.sei.seipicbackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.print.DocFlavor;

/**
 * @author hikari39_
 * @since 2026-03-27
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    @GetMapping("/health")
    public BaseResponse<?> health () {
        return ResponseUtils.success("health");
    }

    @PostMapping("/register")
    public BaseResponse<Long> register(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(userRegisterRequest), ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String password = userRegisterRequest.getPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long userId = userService.userRegister(userAccount, password, checkPassword);
        return ResponseUtils.success(userId);
    }
}
