package com.sei.seipicbackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * @author hikari39_
 * @since 2026-03-28
 */
@Getter
public enum UserRoleEnum {
    USER("用户", "user"),
    ADMIN("管理员", "admin");

    private final String role;
    private final String value;

    UserRoleEnum(String role, String value) {
        this.role = role;
        this.value = value;
    }

    public static UserRoleEnum getEnumByValue(String value) {
        // 判空
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        // userRoleEnums类似于字段
        for(UserRoleEnum userRoleEnum : UserRoleEnum.values()) {
            if (userRoleEnum.value.equals(value)) {
                return userRoleEnum;
            }
        }

        return null;
    }
}
