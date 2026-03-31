package com.sei.seipicbackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * @desc 以图搜图请求体
 * @author hikari39_
 * @since 2026-04-01
 */
@Data
public class SearchPictureByPictureRequest implements Serializable {
    /**
     * 图片 id
     */
    private Long pictureId;

    private static final long serialVersionUID = 1L;
}
