package com.sei.seipicbackend.model.dto.user;

import lombok.Data;
import java.io.Serializable;

/**
 * @author hikari39_
 * @since 2026-03-28
 */
@Data
public class UserUpdateRequest implements Serializable {
    /**
     * 账号
     */
    private Long id;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    private static final long serialVersionUID = 1L;
}
