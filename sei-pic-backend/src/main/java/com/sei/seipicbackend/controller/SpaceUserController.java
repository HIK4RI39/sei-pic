package com.sei.seipicbackend.controller;

import com.sei.seipicbackend.common.BaseResponse;
import com.sei.seipicbackend.common.IdRequest;
import com.sei.seipicbackend.common.ResponseUtils;
import com.sei.seipicbackend.exception.ErrorCode;
import com.sei.seipicbackend.exception.ThrowUtils;
import com.sei.seipicbackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.sei.seipicbackend.manager.auth.model.SpaceUserPermissionConstant;
import com.sei.seipicbackend.model.dto.space.user.SpaceUserAddRequest;
import com.sei.seipicbackend.model.dto.space.user.SpaceUserConfirmRequest;
import com.sei.seipicbackend.model.dto.space.user.SpaceUserEditRequest;
import com.sei.seipicbackend.model.dto.space.user.SpaceUserQueryRequest;
import com.sei.seipicbackend.model.enums.SpaceUserConfirmEnum;
import com.sei.seipicbackend.model.pojo.Space;
import com.sei.seipicbackend.model.pojo.SpaceUser;
import com.sei.seipicbackend.model.vo.space.SpaceUserVO;
import com.sei.seipicbackend.service.SpaceService;
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

    @Resource
    private SpaceService spaceService;

    // region -------------------------- 用户 --------------------------

    // 退出团队空间
    @PostMapping("/quit")
    public BaseResponse<Boolean> quitSpaceUser(@RequestBody IdRequest idRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(idRequest==null, ErrorCode.PARAMS_ERROR);
        boolean result = spaceUserService.quitSpaceUser(idRequest, request);
        return ResponseUtils.success(result);
    }

    /**
     * 确认团队空间邀请

 * 该接口用于处理用户确认加入团队空间的请求
     * @param spaceUserConfirmRequest 包含邀请确认所需信息的请求体
     * @return 返回操作结果，成功则返回true，失败则返回错误信息
     */
    @PostMapping("/confirm")  // HTTP POST请求映射到"/confirm"路径
    public BaseResponse<Boolean> confirmSpaceUser(@RequestBody SpaceUserConfirmRequest spaceUserConfirmRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserConfirmRequest==null, ErrorCode.PARAMS_ERROR);
        boolean result = spaceUserService.confirmSpaceUser(spaceUserConfirmRequest, request);
        return ResponseUtils.success(result);
    }


    /**
     * 新增团队空间成员
     * @param spaceUserAddRequest
     * @return
     */
    @PostMapping("/add")
    @SaSpaceCheckPermission(value= SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAddRequest==null, ErrorCode.PARAMS_ERROR);
        Long spaceUserId = spaceUserService.addSpaceUser(spaceUserAddRequest, request);
        return ResponseUtils.success(spaceUserId);
    }

    /**
     * 删除团队空间成员
     * @param idRequest
     * @return
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value= SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody IdRequest idRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(idRequest==null, ErrorCode.PARAMS_ERROR);
        long id = idRequest.getId();
        ThrowUtils.throwIf(id<=0, ErrorCode.PARAMS_ERROR);

        SpaceUser spaceUser = spaceUserService.query().eq("id", id)
                .eq("confirmStatus", SpaceUserConfirmEnum.AGREED.getValue())
                .one();

        ThrowUtils.throwIf(spaceUser==null, ErrorCode.NOT_FOUND_ERROR, "成员不存在");

        // 不能删除创始人
        Long userId = spaceUser.getUserId();
        Long spaceId = spaceUser.getSpaceId();
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space==null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        ThrowUtils.throwIf(userId.equals(space.getUserId()), ErrorCode.PARAMS_ERROR, "不能删除空间创建者");

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
    @SaSpaceCheckPermission(value= SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest, HttpServletRequest request) {
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
    @SaSpaceCheckPermission(value= SpaceUserPermissionConstant.SPACE_USER_MANAGE)
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
    @SaSpaceCheckPermission(value= SpaceUserPermissionConstant.SPACE_USER_MANAGE)
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
