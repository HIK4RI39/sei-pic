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
import com.sei.seipicbackend.model.dto.user.*;
import com.sei.seipicbackend.model.pojo.User;
import com.sei.seipicbackend.model.vo.UserVO;
import com.sei.seipicbackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @author hikari39_
 * @since 2026-03-27
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

//    @GetMapping("/health")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
//    public BaseResponse<?> health () {
//        return ResponseUtils.success("health");
//    }

    // region -------------------------- 用户 --------------------------


    /**
     * 用户注册
     * @param vipCode
     * @return 用户ID
     */
    @PostMapping("/vip")
    public BaseResponse<Boolean> exchangeVip(@RequestBody VipCode vipCode, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isNull(vipCode), ErrorCode.PARAMS_ERROR);
        boolean result = userService.exchangeVip(vipCode, request);
        return ResponseUtils.success(result);
    }

    /**
     * 用户注册
     * @param userRegisterRequest
     * @return 用户ID
     */
    @PostMapping("/register")
    public BaseResponse<Long> register(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(userRegisterRequest), ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String password = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long userId = userService.userRegister(userAccount, password, checkPassword);
        return ResponseUtils.success(userId);
    }

    /**
     * 用户登录
     * @param userLoginRequest
     * @return 脱敏后的用户数据
     */
    @PostMapping("/login")
    public BaseResponse<UserVO> login(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isNull(userLoginRequest), ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String password = userLoginRequest.getUserPassword();
        UserVO userVO = userService.login(userAccount, password, request);
        return ResponseUtils.success(userVO);
    }

    /**
     * 用户登录 withoutCache
     * @param request
     * @return 脱敏后的用户信息
     */
    @GetMapping("/get/login/withoutCache")
    public BaseResponse<UserVO> getLoginUserWithoutCache(HttpServletRequest request) {
        // 删除1次用户状态
//        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        UserVO loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        User user = userService.getById(userId);
        // 再存入一次
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        return ResponseUtils.success(user.beanToVo());
    }


    /**
     * 用户登录
     * @param request
     * @return 脱敏后的用户信息
     */
    @GetMapping("/get/login")
    public BaseResponse<UserVO> getLoginUser(HttpServletRequest request) {
        return ResponseUtils.success(userService.getLoginUser(request));
    }

    /**
     * 用户注销
     * @param request
     * @return
     */
    @GetMapping("/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest request) {
        return ResponseUtils.success(userService.logout(request));
    }

    /**
     * 用户 根据id获取用户
     * @param idRequest
     * @return
     */
    @PostMapping("/get/vo")
    @AuthCheck // 需要登录
    public BaseResponse<UserVO> getUserVoById(@RequestBody IdRequest idRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(idRequest) || idRequest.getId()<=0, ErrorCode.PARAMS_ERROR);
        long id = idRequest.getId();
        return ResponseUtils.success(userService.getUserVoById(id));
    }

    /**
     * 用户 编辑用户
     * @param userEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editUser(@RequestBody UserEditRequest userEditRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isNull(userEditRequest), ErrorCode.PARAMS_ERROR);
        return ResponseUtils.success(userService.editUser(userEditRequest, request));
    }

    // endregion


    // region -------------------------- 管理员 --------------------------

    /**
     * 管理员 新增用户
     * @param userAddRequest
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(userAddRequest), ErrorCode.PARAMS_ERROR);
        return ResponseUtils.success(userService.addUser(userAddRequest));
    }

    /**
     * 管理员 根据id获取用户
     * @param idRequest
     * @return
     */
    @PostMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(@RequestBody IdRequest idRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(idRequest) || idRequest.getId()<=0, ErrorCode.PARAMS_ERROR);
        long id = idRequest.getId();
        return ResponseUtils.success(userService.getUserById(id));
    }

    /**
     * 管理员 删除用户
     * @param idRequest
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUserById(@RequestBody IdRequest idRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(idRequest) || idRequest.getId()<=0, ErrorCode.PARAMS_ERROR);
        long id = idRequest.getId();
        return ResponseUtils.success(userService.deleteUserById(id));
    }

    /**
     * 管理员 更新用户
     * @param userUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(userUpdateRequest), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userUpdateRequest.getId()==null || userUpdateRequest.getId()<=0, ErrorCode.PARAMS_ERROR);
        String userName = userUpdateRequest.getUserName();
        String userProfile = userUpdateRequest.getUserProfile();
        String userPassword = userUpdateRequest.getUserPassword();
        String userRole = userUpdateRequest.getUserRole();
        ThrowUtils.throwIf(StrUtil.isAllBlank(userName, userProfile, userPassword, userRole), ErrorCode.PARAMS_ERROR);
        return ResponseUtils.success(userService.updateUser(userUpdateRequest));
    }

    /**
     * 管理员 批量获取用户vo
     * @return
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVoByPage(@RequestBody UserPageRequest userPageRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(userPageRequest), ErrorCode.PARAMS_ERROR);
        return ResponseUtils.success(userService.listUserVoByPage(userPageRequest));
    }

    // endregion
}
