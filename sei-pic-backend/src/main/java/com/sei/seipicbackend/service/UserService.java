package com.sei.seipicbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sei.seipicbackend.model.dto.user.UserAddRequest;
import com.sei.seipicbackend.model.dto.user.UserEditRequest;
import com.sei.seipicbackend.model.dto.user.UserPageRequest;
import com.sei.seipicbackend.model.dto.user.UserUpdateRequest;
import com.sei.seipicbackend.model.pojo.User;
import com.sei.seipicbackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author hikari39
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2026-03-28 03:18:14
*/
public interface UserService extends IService<User> {

    // region -------------------------- 通用 --------------------------

    // 转化为UserVO
    UserVO getUserVO(User user);

    // 管理员鉴权
    void checkAdmin(UserVO userVO);

    // 是否是管理员
    boolean isAdmin(User user);

    // endregion

    // region -------------------------- 管理员 --------------------------

    // 创建用户
    Long addUser(UserAddRequest userAddRequest);

    // 根据ID查询用户
    User getUserById(Long id);

    // 分页获取用户
    Page<UserVO>  listUserVoByPage(UserPageRequest userPageRequest);

    // 更新用户
    Boolean updateUser(UserUpdateRequest userUpdateRequest);
    // endregion


    // region -------------------------- 用户 --------------------------
    // 注册
    long userRegister(String userAccount, String password, String checkPassword);

    // 登录
    UserVO login(String userAccount, String password, HttpServletRequest request);

    // 获取登录用户
    UserVO getLoginUser(HttpServletRequest request);

    // 登出
    Boolean logout(HttpServletRequest request);

    // 获得UserVO
    UserVO getUserVoById(Long id);

    // 删除用户
    Boolean deleteUserById(long id);

    // 编辑用户信息
    Boolean editUser(UserEditRequest userEditRequest, HttpServletRequest request);

    // endregion
}
