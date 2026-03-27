package com.sei.seipicbackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author hikari39_
 * @since 2026-03-28
 */
@Data
public class UserLoginRequest implements Serializable {
    private String userAccount;
    private String password;
    private static final long serialVersionUID = 1L;
}
