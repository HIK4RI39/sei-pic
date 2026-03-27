package com.sei.seipicbackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author hikari39_
 * @since 2026-03-28
 */
@Data
public class UserAddRequest implements Serializable {
    private String userAccount;
    private static final long serialVersionUID = 1L;
}
