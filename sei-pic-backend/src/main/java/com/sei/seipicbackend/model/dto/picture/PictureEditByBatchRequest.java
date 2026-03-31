package com.sei.seipicbackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author hikari39_
 * @since 2026-04-01
 */
@Data
public class PictureEditByBatchRequest implements Serializable {

    /**
     * 命名规则
     */
    private String nameRule;

    /**
     * 图片 id 列表
     */
    private List<Long> pictureIdList;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;

    private static final long serialVersionUID = 1L;
}
