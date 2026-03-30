package com.sei.seipicbackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * @author hikari39_
 * @since 2026-03-29
 */
@Data
public class PictureUploadRequest implements Serializable {
    /**
     * 图片 id（用于修改）
     */
    private Long id;

    /**
     * 图片url
     */
    private String fileUrl;

    /**
     * 图片名称
     */
    private String picName;


    private static final long serialVersionUID = 1L;
}
