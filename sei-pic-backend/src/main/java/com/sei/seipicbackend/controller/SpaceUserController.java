package com.sei.seipicbackend.controller;

import com.sei.seipicbackend.common.BaseResponse;
import com.sei.seipicbackend.common.IdRequest;
import com.sei.seipicbackend.common.ResponseUtils;
import com.sei.seipicbackend.exception.ErrorCode;
import com.sei.seipicbackend.exception.ThrowUtils;
import com.sei.seipicbackend.model.dto.space.user.SpaceUserAddRequest;
import com.sei.seipicbackend.model.dto.space.user.SpaceUserEditRequest;
import com.sei.seipicbackend.model.dto.space.user.SpaceUserQueryRequest;
import com.sei.seipicbackend.model.pojo.SpaceUser;
import com.sei.seipicbackend.model.vo.space.SpaceUserVO;
import com.sei.seipicbackend.service.SpaceUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

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

    /**
     * 查询某个团队空间成员
     * @param spaceUserQueryRequest
     * @return
     */
    @PostMapping("/get")
    public BaseResponse<SpaceUser> deleteSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserQueryRequest==null, ErrorCode.PARAMS_ERROR);
        SpaceUser spaceUser = spaceUserService.getSpaceUser(spaceUserQueryRequest);
        return ResponseUtils.success(spaceUser);
    }

    /**
     * 查询团队空间 成员信息列表
     * @param spaceUserQueryRequest
     * @return
     */
    @PostMapping("/list")
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserQueryRequest==null, ErrorCode.PARAMS_ERROR);
        List<SpaceUserVO> spaceUserList = spaceUserService.listSpaceUser(spaceUserQueryRequest);
        return ResponseUtils.success(spaceUserList);
    }

    /**
     * 编辑成员信息 (设置权限)
     * @param spaceUserEditRequest
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserEditRequest==null, ErrorCode.PARAMS_ERROR);
        boolean result = spaceUserService.editSpaceUser(spaceUserEditRequest);
        return ResponseUtils.success(result);
    }

    /**
     * 查询用户加入的团队空间列表
     * @param request
     * @return
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        List<SpaceUserVO> spaceUserList = spaceUserService.listMyTeamSpace(request);
        return ResponseUtils.success(spaceUserList);
    }



    // endregion


}
