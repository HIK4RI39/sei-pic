package com.sei.seipicbackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sei.seipicbackend.common.ResponseUtils;
import com.sei.seipicbackend.exception.ErrorCode;
import com.sei.seipicbackend.exception.ThrowUtils;
import com.sei.seipicbackend.mapper.UserMapper;
import com.sei.seipicbackend.model.enums.UserRoleEnum;
import com.sei.seipicbackend.model.pojo.User;
import com.sei.seipicbackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
* @author hikari39
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2026-03-28 03:18:14
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    private static final String SALT = "sei";

    /**
     * 用户注册
     * @param userAccount
     * @param password
     * @param checkPassword
     * @return
     */
    @Override
    public long userRegister(String userAccount, String password, String checkPassword) {
        ThrowUtils.throwIf(
            !StrUtil.isAllNotBlank(userAccount, password, checkPassword),
            ErrorCode.PARAMS_ERROR
        );
        // 账号长度>=4
        ThrowUtils.throwIf(userAccount.length()<4, ErrorCode.PARAMS_ERROR);
        // 密码长度>=4
        ThrowUtils.throwIf(password.length()<8, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(checkPassword.length()<8, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!password.equals(checkPassword), ErrorCode.PARAMS_ERROR);

        boolean exists = lambdaQuery().eq(User::getUserAccount, userAccount).exists();
        ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "账号已存在");
        // 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean save = save(user);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR);
        return user.getId();
    }
}




