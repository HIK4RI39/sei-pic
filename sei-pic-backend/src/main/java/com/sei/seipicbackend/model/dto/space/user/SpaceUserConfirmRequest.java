package com.sei.seipicbackend.model.dto.space.user;

import lombok.Data;

/**
 * @author hikari39_
 * @since 2026-04-03
 */
@Data
public class SpaceUserConfirmRequest {
    /**
     * 空间成员记录id
     */
    private Long id;

    /**
     *
     */
    private Integer confirmStatus;
}
