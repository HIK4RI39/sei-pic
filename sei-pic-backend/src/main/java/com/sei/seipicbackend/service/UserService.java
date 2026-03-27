package com.sei.seipicbackend.service;

import com.sei.seipicbackend.common.BaseResponse;
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
}
