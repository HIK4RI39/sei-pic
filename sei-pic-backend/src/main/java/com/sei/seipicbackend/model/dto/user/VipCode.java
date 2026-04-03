package com.sei.seipicbackend.model.dto.user;

/**
 * @author hikari39_
 * @since 2026-04-03
 */

import lombok.Data;

/**
 * 兑换码
 */
@Data
public class VipCode {

    // 兑换码
    private String code;

    // 是否已经使用
    private boolean hasUsed;
}