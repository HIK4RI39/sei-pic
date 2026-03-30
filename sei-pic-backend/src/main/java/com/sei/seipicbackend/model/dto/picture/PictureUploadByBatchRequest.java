package com.sei.seipicbackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author hikari39_
 * @since 2026-03-29
 */
@Data
public class PictureUploadByBatchRequest implements Serializable {
    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 抓取数量
     */
    private Integer count = 10;

    /**
     * 名称前缀
     */
    private String namePrefix;

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
