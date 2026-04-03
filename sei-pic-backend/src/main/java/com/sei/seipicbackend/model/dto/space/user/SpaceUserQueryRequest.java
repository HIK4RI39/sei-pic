package com.sei.seipicbackend.model.dto.space.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author hikari39_
 * @since 2026-04-03
 */
@Data
public class SpaceUserQueryRequest implements Serializable {

    /**
     * ID
     */
    private Long id;

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

    /**
     * 确认状态 0-待确认 1-同意 2-拒绝
     */
    private Integer confirmStatus;

    private static final long serialVersionUID = 1L;
}
