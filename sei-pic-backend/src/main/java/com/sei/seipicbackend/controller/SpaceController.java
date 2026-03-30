package com.sei.seipicbackend.controller;

import cn.hutool.core.util.ObjUtil;
import com.sei.seipicbackend.common.BaseResponse;
import com.sei.seipicbackend.common.IdRequest;
import com.sei.seipicbackend.common.ResponseUtils;
import com.sei.seipicbackend.exception.ErrorCode;
import com.sei.seipicbackend.exception.ThrowUtils;
import com.sei.seipicbackend.model.dto.space.SpaceAddRequest;
import com.sei.seipicbackend.service.SpaceService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @author hikari39_
 * @since 2026-03-31
 */
@RestController
@RequestMapping("/space")
public class SpaceController {
    @Resource
    private SpaceService spaceService;

    // region -------------------------- 管理员 --------------------------

    // endregion

    // region -------------------------- 用户 --------------------------

    /**
     * 创建空间
     * @param spaceAddRequest
     * @param request
     * @return
     */
    @PostMapping("/create")
    public BaseResponse<Boolean> createSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceAddRequest), ErrorCode.PARAMS_ERROR);
        boolean result = spaceService.createSpace(spaceAddRequest, request);
        return ResponseUtils.success(result);
    }

    /**
     * 删除空间
     * @param idRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody IdRequest idRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(idRequest), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(idRequest.getId() < 0, ErrorCode.PARAMS_ERROR);
        boolean result = spaceService.deleteSpace(idRequest, request);
        return ResponseUtils.success(result);
    }
        // endregion



}
