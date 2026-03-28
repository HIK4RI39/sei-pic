package com.sei.seipicbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sei.seipicbackend.model.dto.user.UserAddRequest;
import com.sei.seipicbackend.model.dto.user.UserEditRequest;
import com.sei.seipicbackend.model.dto.user.UserPageRequest;
import com.sei.seipicbackend.model.dto.user.UserUpdateRequest;
import com.sei.seipicbackend.model.pojo.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sei.seipicbackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author hikari39
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2026-03-28 03:18:14
*/
public interface UserService extends IService<User> {
    long userRegister(String userAccount, String password, String checkPassword);

    UserVO login(String userAccount, String password, HttpServletRequest request);

    UserVO getLoginUser(HttpServletRequest request);

    Boolean logout(HttpServletRequest request);

    Long addUser(UserAddRequest userAddRequest);

    UserVO getUserVoById(Long id);

    User getUserById(Long id);

    Boolean deleteUserById(long id);

    Page<UserVO>  listUserVoByPage(UserPageRequest userPageRequest);

    Boolean updateUser(UserUpdateRequest userUpdateRequest);

    Boolean editUser(UserEditRequest userEditRequest, HttpServletRequest request);

    UserVO getUserVO(User user);
}
