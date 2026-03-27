package com.sei.seipicbackend.common;

import lombok.Data;

/**
 * @author hikari39_
 * @since 2026-03-27
 */
@Data
public class PageRequest {
    /**
     * 页码, 默认为1
     */
    private int current = 1;

    /**
     * 页大小, 默认为10
      */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序方式, 默认升序
     */
    private String sortOrder = "descend";
}
