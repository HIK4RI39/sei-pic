package com.sei.seipicbackend.model.dto.user;

import lombok.Data;
import java.io.Serializable;

/**
 * @author hikari39_
 * @since 2026-03-28
 */
@Data
public class UserEditRequest implements Serializable {
    /**
     * 密码
     */
    private String oldPassword;

    /**
     * 密码
     */
    private String newPassWord;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户头像url
     */
    private String userAvatar;

    private static final long serialVersionUID = 1L;
}
