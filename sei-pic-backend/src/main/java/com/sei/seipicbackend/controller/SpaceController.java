package com.sei.seipicbackend.controller;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sei.seipicbackend.annotation.AuthCheck;
import com.sei.seipicbackend.common.BaseResponse;
import com.sei.seipicbackend.common.IdRequest;
import com.sei.seipicbackend.common.ResponseUtils;
import com.sei.seipicbackend.constant.UserConstant;
import com.sei.seipicbackend.exception.ErrorCode;
import com.sei.seipicbackend.exception.ThrowUtils;
import com.sei.seipicbackend.model.dto.picture.PictureQueryRequest;
import com.sei.seipicbackend.model.dto.space.SpaceAddRequest;
import com.sei.seipicbackend.model.dto.space.SpaceEditRequest;
import com.sei.seipicbackend.model.dto.space.SpaceQueryRequest;
import com.sei.seipicbackend.model.dto.space.SpaceUpdateRequest;
import com.sei.seipicbackend.model.enums.SpaceLevelEnum;
import com.sei.seipicbackend.model.pojo.Picture;
import com.sei.seipicbackend.model.pojo.Space;
import com.sei.seipicbackend.model.vo.SpaceLevel;
import com.sei.seipicbackend.model.vo.SpaceVO;
import com.sei.seipicbackend.service.SpaceService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * 管理员 分页获取空间
     * @param spaceQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<SpaceVO>> getSpacePage(@RequestBody SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceQueryRequest), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(spaceQueryRequest.getCurrent() <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(spaceQueryRequest.getPageSize() <= 0, ErrorCode.PARAMS_ERROR);
        Page<SpaceVO> spacePage = spaceService.getSpacePage(spaceQueryRequest, request);
        return ResponseUtils.success(spacePage);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceUpdateRequest), ErrorCode.PARAMS_ERROR);
        boolean result = spaceService.updateSpace(spaceUpdateRequest);
        return ResponseUtils.success(result);
    }

    // endregion

    // region -------------------------- 用户 --------------------------

    /**
     * 获取spaceLevel列表
     * @return
     */
    @PostMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values()).map(spaceLevelEnum ->
                new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()
                )
        ).collect(Collectors.toList());
        return ResponseUtils.success(spaceLevelList);
    }

    /**
     * 创建空间
     * @param spaceAddRequest
     * @param request
     * @return
     */
    @PostMapping("/create")
    public BaseResponse<Long> createSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceAddRequest), ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceService.createSpace(spaceAddRequest, request);
        return ResponseUtils.success(spaceId);
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

    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceEditRequest), ErrorCode.PARAMS_ERROR);
        Long id = spaceEditRequest.getId();
        String spaceName = spaceEditRequest.getSpaceName();
        ThrowUtils.throwIf(id==null, ErrorCode.PARAMS_ERROR, "空间id不能为空");
        ThrowUtils.throwIf(id<=0, ErrorCode.PARAMS_ERROR, "非法的空间id");
        ThrowUtils.throwIf(spaceName==null || StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR, "空间名称不能为空");
        boolean result = spaceService.editSpace(spaceEditRequest, request);
        return ResponseUtils.success(result);
    }

    // endregion



}
