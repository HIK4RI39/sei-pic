package com.sei.seipicbackend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * @author hikari39_
 * @since 2026-03-31
 */
@Data
public class SpaceEditRequest implements Serializable {

    /**
     * 空间 id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    private static final long serialVersionUID = 1L;
}
