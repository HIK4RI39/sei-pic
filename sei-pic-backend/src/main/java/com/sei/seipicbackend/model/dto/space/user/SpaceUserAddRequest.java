package com.sei.seipicbackend.model.dto.space.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author hikari39_
 * @since 2026-04-03
 */
@Data
public class SpaceUserAddRequest implements Serializable {

    /**
     * 空间 ID
     */
    private Long spaceId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    private static final long serialVersionUID = 1L;
}
