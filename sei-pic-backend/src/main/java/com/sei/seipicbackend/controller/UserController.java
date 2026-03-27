package com.sei.seipicbackend.controller;

import com.sei.seipicbackend.common.BaseResponse;
import com.sei.seipicbackend.common.ResponseUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.Mapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author hikari39_
 * @since 2026-03-27
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @GetMapping("/health")
    public BaseResponse<?> health () {
        return ResponseUtils.success("health");
    }
}
