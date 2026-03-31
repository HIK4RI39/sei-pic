package com.sei.seipicbackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * @author hikari39_
 * @since 2026-04-01
 */
@Data
public class SearchPictureByColorRequest implements Serializable {

    /**
     * 图片主色调
     */
    private String picColor;

    /**
     * 空间 id
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}
