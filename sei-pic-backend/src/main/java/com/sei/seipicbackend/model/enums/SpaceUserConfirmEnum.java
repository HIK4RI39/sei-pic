package com.sei.seipicbackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * @author hikari39_
 * @since 2026-04-03
 */
@Getter
public enum SpaceUserConfirmEnum {
    CONFIRMING("待确认", 0),
    AGREED("同意", 1),
    REJECTED("拒绝", 2);

    private final String text;
    private final int value;

    SpaceUserConfirmEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static SpaceUserConfirmEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (SpaceUserConfirmEnum spaceUserStatusEnum : SpaceUserConfirmEnum.values()) {
            if (spaceUserStatusEnum.value == value) {
                return spaceUserStatusEnum;
            }
        }
        return null;
    }
}
