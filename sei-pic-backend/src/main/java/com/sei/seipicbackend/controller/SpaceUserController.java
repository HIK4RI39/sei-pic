package com.sei.seipicbackend.controller;

import com.sei.seipicbackend.common.BaseResponse;
import com.sei.seipicbackend.common.IdRequest;
import com.sei.seipicbackend.common.ResponseUtils;
import com.sei.seipicbackend.exception.ErrorCode;
import com.sei.seipicbackend.exception.ThrowUtils;
import com.sei.seipicbackend.model.dto.space.user.SpaceUserAddRequest;
import com.sei.seipicbackend.model.pojo.SpaceUser;
import com.sei.seipicbackend.service.SpaceUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @author hikari39_
 * @since 2026-04-03
 */
@RestController
@RequestMapping("/spaceUser")
@Slf4j
public class SpaceUserController {

    @Resource
    private SpaceUserService spaceUserService;

    // region -------------------------- 用户 --------------------------

    /**
     * 新增团队空间成员
     * @param spaceUserAddRequest
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAddRequest==null, ErrorCode.PARAMS_ERROR);
        Long spaceUserId = spaceUserService.addSpaceUser(spaceUserAddRequest);
        return ResponseUtils.success(spaceUserId);
    }

    /**
     * 删除团队空间成员
     * @param idRequest
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody IdRequest idRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(idRequest==null, ErrorCode.PARAMS_ERROR);
        long id = idRequest.getId();
        ThrowUtils.throwIf(id<=0, ErrorCode.PARAMS_ERROR);

        SpaceUser spaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(spaceUser==null, ErrorCode.NOT_FOUND_ERROR, "成员不存在");
        boolean result = spaceUserService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.PARAMS_ERROR, "删除失败");

        return ResponseUtils.success(true);
    }


    // endregion


}
