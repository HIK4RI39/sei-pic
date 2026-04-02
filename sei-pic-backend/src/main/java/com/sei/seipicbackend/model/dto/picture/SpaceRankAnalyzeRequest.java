package com.sei.seipicbackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * @author hikari39_
 * @since 2026-04-02
 */
@Data
public class SpaceRankAnalyzeRequest implements Serializable {

    /**
     * 排名前 N 的空间
     */
    private Integer topN = 10;

    private static final long serialVersionUID = 1L;
}
