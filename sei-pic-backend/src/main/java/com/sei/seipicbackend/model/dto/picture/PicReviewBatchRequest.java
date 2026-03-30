package com.sei.seipicbackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author hikari39_
 * @since 2026-03-29
 */
@Data
public class PicReviewBatchRequest implements Serializable {

    /**
     * id list
     */
    private List<Long> idList;

    /**
     * 状态：0-待审核, 1-通过, 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;


    private static final long serialVersionUID = 1L;
}
