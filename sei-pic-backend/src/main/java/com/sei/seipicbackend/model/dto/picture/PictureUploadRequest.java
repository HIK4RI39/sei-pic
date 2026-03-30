package com.sei.seipicbackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

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
     * spaceId
     */
    private Long spaceId;

    /**
     * 图片url
     */
    private String fileUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 图片分类, 用于批量上传设置
     */
    private String category;

    /**
     * 图片标签, 用于批量上传设置
     */
    private List<String> tags;


    private static final long serialVersionUID = 1L;
}
